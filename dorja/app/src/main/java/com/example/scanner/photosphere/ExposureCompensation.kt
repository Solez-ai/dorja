package com.example.scanner.photosphere

import kotlin.math.exp
import kotlin.math.ln

internal object ExposureCompensation {

    private const val ITERATIONS = 12
    const val MAX_GAIN: Float = 1.6f
    const val MIN_GAIN: Float = 1f / MAX_GAIN
    private const val MIN_USABLE_LUMA = 1e-6

    fun solveGains(meanLuma: DoubleArray, edges: List<Pair<Int, Int>>): FloatArray {
        val size = meanLuma.size
        if (size == 0) return FloatArray(0)

        val degree = IntArray(size)
        val usable = BooleanArray(size) { meanLuma[it] > MIN_USABLE_LUMA }
        edges.forEach { (a, b) ->
            if (a in 0 until size && b in 0 until size && a != b && usable[a] && usable[b]) {
                degree[a]++
                degree[b]++
            }
        }

        val offsets = IntArray(size + 1)
        for (i in 0 until size) offsets[i + 1] = offsets[i] + degree[i]
        val neighbours = IntArray(offsets[size])
        val cursor = offsets.copyOf(size)
        edges.forEach { (a, b) ->
            if (a in 0 until size && b in 0 until size && a != b && usable[a] && usable[b]) {
                neighbours[cursor[a]++] = b
                neighbours[cursor[b]++] = a
            }
        }

        val logLuma = DoubleArray(size) { if (usable[it]) ln(meanLuma[it]) else 0.0 }
        val logGain = DoubleArray(size)

        repeat(ITERATIONS) {
            for (i in 0 until size) {
                val start = offsets[i]
                val end = offsets[i + 1]
                if (end <= start) continue
                var sum = 0.0
                for (slot in start until end) {
                    val j = neighbours[slot]
                    sum += logGain[j] + logLuma[j] - logLuma[i]
                }
                logGain[i] = sum / (end - start)
            }
        }

        var logSum = 0.0
        var connectedCount = 0
        for (i in 0 until size) {
            if (offsets[i + 1] > offsets[i]) {
                logSum += logGain[i]
                connectedCount++
            }
        }
        val offset = if (connectedCount > 0) logSum / connectedCount else 0.0

        val maxLog = ln(MAX_GAIN.toDouble())
        return FloatArray(size) { i ->
            if (offsets[i + 1] <= offsets[i]) {
                1f
            } else {
                exp((logGain[i] - offset).coerceIn(-maxLog, maxLog)).toFloat()
            }
        }
    }
}
