package com.example.scanner.photosphere

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * The sparse per-pixel label data the seam solver works on.
 *
 * Every pixel of the reduced label grid lists the frames that cover it, their
 * colours as sampled from those frames, and the cost of assigning the pixel to
 * each of them. The arrays are packed per node so the whole thing stays flat:
 * node [n] owns `nodeLabelCount[n]` consecutive entries in [nodeLabels],
 * [nodeData] and (three per entry) [nodeColors], starting at
 * `offsets[n]`. [nodeMean] is the mean of the covering colours at each pixel,
 * which is what a frame *not* covering that pixel is said to have recorded
 * there — the fill value that keeps the seam cost a true metric (see
 * [SeamSolver]).
 */
internal class PackedGrid(
    val width: Int,
    val height: Int,
    val labelCount: Int,
    val nodeLabelCount: IntArray,
    val nodeLabels: IntArray,
    val nodeData: FloatArray,
    val nodeColors: FloatArray,
    val nodeMean: FloatArray,
) {
    val nodeCount: Int = width * height

    /** Where each node's packed entries begin; `offsets[nodeCount]` is the total. */
    val offsets: IntArray = IntArray(nodeCount + 1).also {
        for (n in 0 until nodeCount) it[n + 1] = it[n] + nodeLabelCount[n]
    }
}

/**
 * The multi-label seam solver: assigns every canvas pixel to exactly one frame.
 *
 * Where cross-fading lets every overlapping frame contribute to a pixel (which
 * causes ghosting and washes out fine details when handheld parallax is present),
 * seam carving asks which single frame should paint it and cuts there,
 * reserving the blend for a few pixels either side of the cut. The assignment
 * is the minimizer of an energy over the label grid:
 *
 * ```
 * E(f) = Σ_p D(p, f(p))  +  Σ_{edges (p,q)} λ · ( |I_fp(p) − I_fq(p)| + |I_fp(q) − I_fq(q)| )
 * ```
 *
 * Minimized with α-expansion (Boykov–Veksler–Zabih) using Dinic's s-t min-cut
 * in pure Kotlin.
 */
internal object SeamSolver {

    /** Residual edges at or below this carry no flow. */
    private const val EPS = 1e-9

    /** A label the current expansion move forbids: switching to it costs this. */
    private const val INF_UNARY = 1e15

    /** Solves the assignment, returning one frame index per grid pixel. */
    fun solve(
        grid: PackedGrid,
        smoothLambda: Double,
        maxPasses: Int = 2,
        onExpansion: () -> Unit = {},
        wrapLongitude: Boolean = false,
    ): IntArray {
        val labels = greedyLabels(grid)

        val order = labelOrder(grid)
        repeat(maxPasses) {
            var changed = false
            for (alpha in order) {
                if (expandTo(grid, smoothLambda, labels, alpha, wrapLongitude)) changed = true
                onExpansion()
            }
            if (!changed) return labels
        }
        return labels
    }

    /** Each node's cheapest label on the data term alone; also the solver's start. */
    internal fun greedyLabels(grid: PackedGrid): IntArray {
        val labels = IntArray(grid.nodeCount) { -1 }
        for (n in 0 until grid.nodeCount) {
            val count = grid.nodeLabelCount[n]
            if (count == 0) continue
            val off = grid.offsets[n]
            var best = grid.nodeLabels[off]
            var bestCost = grid.nodeData[off].toDouble()
            for (j in 1 until count) {
                val cost = grid.nodeData[off + j].toDouble()
                if (cost < bestCost) {
                    bestCost = cost
                    best = grid.nodeLabels[off + j]
                }
            }
            labels[n] = best
        }
        return labels
    }

    /** Labels that cover at least one pixel, largest coverage first. */
    private fun labelOrder(grid: PackedGrid): IntArray {
        val coverage = IntArray(grid.labelCount)
        for (n in 0 until grid.nodeCount) {
            val off = grid.offsets[n]
            for (j in 0 until grid.nodeLabelCount[n]) coverage[grid.nodeLabels[off + j]]++
        }
        return (0 until grid.labelCount)
            .filter { coverage[it] > 0 }
            .sortedByDescending { coverage[it] }
            .toIntArray()
    }

    /**
     * One α-expansion move: every pixel may keep its current frame or switch to
     * [alpha], solved exactly as an s-t min-cut. Returns whether any pixel moved.
     */
    private fun expandTo(
        grid: PackedGrid,
        smoothLambda: Double,
        labels: IntArray,
        alpha: Int,
        wrapLongitude: Boolean = false,
    ): Boolean {
        val active = BooleanArray(grid.nodeCount)
        var anyActive = false
        for (n in 0 until grid.nodeCount) {
            if (grid.nodeLabelCount[n] == 0) continue
            val off = grid.offsets[n]
            for (j in 0 until grid.nodeLabelCount[n]) {
                if (grid.nodeLabels[off + j] == alpha) {
                    active[n] = true
                    anyActive = true
                    break
                }
            }
        }
        if (!anyActive) return false

        val vertexOf = IntArray(grid.nodeCount) { -1 }
        var vertexCount = 0
        for (n in 0 until grid.nodeCount) if (active[n]) vertexOf[n] = vertexCount++

        val flow = MaxFlow(vertexCount + 2)
        val source = vertexCount
        val sink = vertexCount + 1

        val u0 = DoubleArray(vertexCount)
        val u1 = DoubleArray(vertexCount)
        for (n in 0 until grid.nodeCount) {
            val v = vertexOf[n]
            if (v == -1) continue
            u0[v] = dataOf(grid, n, labels[n])
            u1[v] = dataOf(grid, n, alpha)
        }

        val curColor = FloatArray(grid.nodeCount * 3)
        val alphaColor = FloatArray(grid.nodeCount * 3)
        for (n in 0 until grid.nodeCount) {
            if (grid.nodeLabelCount[n] == 0) continue
            colorOf(grid, n, labels[n], curColor, n * 3)
            colorOf(grid, n, alpha, alphaColor, n * 3)
        }

        val scratch = FloatArray(6)
        for (r in 0 until grid.height) {
            for (c in 0 until grid.width) {
                val n = r * grid.width + c
                if (grid.nodeLabelCount[n] == 0) continue
                if (c + 1 < grid.width) {
                    val m = n + 1
                    if (grid.nodeLabelCount[m] > 0) {
                        addEdgeTerm(
                            grid, smoothLambda, labels, active, vertexOf, flow,
                            n, m, curColor, alphaColor, scratch, u0, u1,
                        )
                    }
                }
                if (wrapLongitude && c == 0 && grid.width > 1) {
                    val m = n + grid.width - 1
                    if (grid.nodeLabelCount[m] > 0) {
                        addEdgeTerm(
                            grid, smoothLambda, labels, active, vertexOf, flow,
                            n, m, curColor, alphaColor, scratch, u0, u1,
                        )
                    }
                }
                if (r + 1 < grid.height) {
                    val m = n + grid.width
                    if (grid.nodeLabelCount[m] > 0) {
                        addEdgeTerm(
                            grid, smoothLambda, labels, active, vertexOf, flow,
                            n, m, curColor, alphaColor, scratch, u0, u1,
                        )
                    }
                }
            }
        }

        for (n in 0 until grid.nodeCount) {
            val v = vertexOf[n]
            if (v == -1) continue
            val offset = max(0.0, max(-u0[v], -u1[v]))
            if (u1[v] + offset > 0.0) flow.addEdge(source, v, u1[v] + offset)
            if (u0[v] + offset > 0.0) flow.addEdge(v, sink, u0[v] + offset)
        }

        flow.maxflow(source, sink)
        val keep = flow.reachableFrom(source)

        var changed = false
        for (n in 0 until grid.nodeCount) {
            val v = vertexOf[n]
            if (v == -1) continue
            if (!keep[v] && labels[n] != alpha) {
                labels[n] = alpha
                changed = true
            }
        }
        return changed
    }

    private fun addEdgeTerm(
        grid: PackedGrid,
        smoothLambda: Double,
        labels: IntArray,
        active: BooleanArray,
        vertexOf: IntArray,
        flow: MaxFlow,
        n: Int,
        m: Int,
        curColor: FloatArray,
        alphaColor: FloatArray,
        scratch: FloatArray,
        u0: DoubleArray,
        u1: DoubleArray,
    ) {
        val activeN = active[n]
        val activeM = active[m]
        if (!activeN && !activeM) return

        val fp = labels[n]
        val fq = labels[m]
        colorOf(grid, n, fq, scratch, 0)
        colorOf(grid, m, fp, scratch, 3)

        val a = smoothLambda * (dist(curColor, n * 3, scratch, 0) + dist(scratch, 3, curColor, m * 3))
        val b = smoothLambda * (dist(curColor, n * 3, alphaColor, n * 3) + dist(scratch, 3, alphaColor, m * 3))
        val c = smoothLambda * (dist(alphaColor, n * 3, scratch, 0) + dist(alphaColor, m * 3, curColor, m * 3))

        when {
            activeN && activeM -> {
                val vN = vertexOf[n]
                val vM = vertexOf[m]
                val cutWeight = max(0.0, (b + c - a) / 2.0)
                u1[vN] += c - a - cutWeight
                u1[vM] += b - a - cutWeight
                if (cutWeight > 0.0) flow.addUndirectedEdge(vN, vM, cutWeight)
            }
            activeN -> {
                val vN = vertexOf[n]
                u0[vN] += a
                u1[vN] += c
            }
            else -> {
                val vM = vertexOf[m]
                u0[vM] += a
                u1[vM] += b
            }
        }
    }

    private fun dataOf(grid: PackedGrid, node: Int, label: Int): Double {
        val off = grid.offsets[node]
        for (j in 0 until grid.nodeLabelCount[node]) {
            if (grid.nodeLabels[off + j] == label) return grid.nodeData[off + j].toDouble()
        }
        return INF_UNARY
    }

    private fun colorOf(
        grid: PackedGrid,
        node: Int,
        label: Int,
        out: FloatArray,
        outOffset: Int,
    ) {
        val off = grid.offsets[node]
        for (j in 0 until grid.nodeLabelCount[node]) {
            if (grid.nodeLabels[off + j] == label) {
                val base = (off + j) * 3
                out[outOffset] = grid.nodeColors[base]
                out[outOffset + 1] = grid.nodeColors[base + 1]
                out[outOffset + 2] = grid.nodeColors[base + 2]
                return
            }
        }
        val base = node * 3
        out[outOffset] = grid.nodeMean[base]
        out[outOffset + 1] = grid.nodeMean[base + 1]
        out[outOffset + 2] = grid.nodeMean[base + 2]
    }

    private fun dist(a: FloatArray, aOffset: Int, b: FloatArray, bOffset: Int): Double {
        val d0 = a[aOffset] - b[bOffset]
        val d1 = a[aOffset + 1] - b[bOffset + 1]
        val d2 = a[aOffset + 2] - b[bOffset + 2]
        return sqrt((d0 * d0 + d1 * d1 + d2 * d2).toDouble())
    }

    internal class MaxFlow(private val vertexCount: Int) {

        private val head = IntArray(vertexCount) { -1 }
        private var to = IntArray(16)
        private var cap = DoubleArray(16)
        private var next = IntArray(16)
        private var edgeCount = 0

        private val level = IntArray(vertexCount)
        private val queue = IntArray(vertexCount)
        private val iter = IntArray(vertexCount)

        private fun addArc(u: Int, v: Int, c: Double) {
            if (edgeCount >= to.size) {
                val grown = to.size * 2
                to = to.copyOf(grown)
                cap = cap.copyOf(grown)
                next = next.copyOf(grown)
            }
            to[edgeCount] = v
            cap[edgeCount] = c
            next[edgeCount] = head[u]
            head[u] = edgeCount
            edgeCount++
        }

        fun addEdge(u: Int, v: Int, c: Double) {
            if (c <= 0.0) return
            addArc(u, v, c)
            addArc(v, u, 0.0)
        }

        fun addUndirectedEdge(u: Int, v: Int, c: Double) {
            if (c <= 0.0) return
            addArc(u, v, c)
            addArc(v, u, c)
        }

        private fun bfs(source: Int, sink: Int): Boolean {
            level.fill(-1)
            var read = 0
            var write = 0
            level[source] = 0
            queue[write++] = source
            while (read < write) {
                val u = queue[read++]
                var e = head[u]
                while (e != -1) {
                    val v = to[e]
                    if (cap[e] > EPS && level[v] < 0) {
                        level[v] = level[u] + 1
                        queue[write++] = v
                    }
                    e = next[e]
                }
            }
            return level[sink] >= 0
        }

        private fun dfs(u: Int, sink: Int, pushed: Double): Double {
            if (u == sink) return pushed
            var e = iter[u]
            while (e != -1) {
                val v = to[e]
                if (cap[e] > EPS && level[v] == level[u] + 1) {
                    val flow = dfs(v, sink, min(pushed, cap[e]))
                    if (flow > EPS) {
                        cap[e] -= flow
                        cap[e xor 1] += flow
                        return flow
                    }
                }
                e = next[e]
                iter[u] = e
            }
            return 0.0
        }

        fun maxflow(source: Int, sink: Int): Double {
            var total = 0.0
            while (bfs(source, sink)) {
                System.arraycopy(head, 0, iter, 0, vertexCount)
                while (true) {
                    val flow = dfs(source, sink, Double.MAX_VALUE / 4)
                    if (flow <= EPS) break
                    total += flow
                }
            }
            return total
        }

        fun reachableFrom(source: Int): BooleanArray {
            val seen = BooleanArray(vertexCount)
            var read = 0
            var write = 0
            queue[write++] = source
            seen[source] = true
            while (read < write) {
                val u = queue[read++]
                var e = head[u]
                while (e != -1) {
                    val v = to[e]
                    if (cap[e] > EPS && !seen[v]) {
                        seen[v] = true
                        queue[write++] = v
                    }
                    e = next[e]
                }
            }
            return seen
        }
    }
}
