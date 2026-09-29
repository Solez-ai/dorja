package com.example.ui.floorplan

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.DorjaButton
import com.example.ui.components.DorjaChip
import com.example.ui.components.DorjaOutlinedButton
import com.example.ui.i18n.L
import com.example.ui.theme.DorjaColors
import com.google.gson.Gson
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToLong
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// Model (v2): walls and rooms live in METERS on an architectural plan.
// v1 stored normalized 0..1 coordinates; fromJson() migrates those x10.
// ─────────────────────────────────────────────────────────────────────────────

/** One wall segment in meters. [thickness] is wall width in meters. */
data class FloorPlanWall(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val thickness: Float = 0.15f,
    val type: String = "interior" // "interior" | "exterior"
)

/** A text label anchored at a plan position in meters. */
data class FloorPlanRoomLabel(
    val name: String,
    val cx: Float,
    val cy: Float
)

/** Full floor plan payload persisted as JSON in [com.example.data.model.Listing.floorPlanJson]. */
data class FloorPlanData(
    val walls: List<FloorPlanWall> = emptyList(),
    val rooms: List<FloorPlanRoomLabel> = emptyList(),
    val version: Int = 2
) {
    fun toJson(): String = Gson().toJson(this)

    companion object {
        /** v1 coordinates were normalized to a 0..1 canvas ≈ 10 m across. */
        private const val LEGACY_SPAN = 10f

        fun fromJson(json: String?): FloorPlanData? {
            if (json.isNullOrBlank()) return null
            return runCatching {
                val raw = Gson().fromJson(json, FloorPlanData::class.java)
                    ?: return@runCatching null
                val s = if (raw.version < 2) LEGACY_SPAN else 1f
                FloorPlanData(
                    walls = raw.walls.map {
                        FloorPlanWall(
                            x1 = it.x1 * s,
                            y1 = it.y1 * s,
                            x2 = it.x2 * s,
                            y2 = it.y2 * s,
                            // Gson bypasses Kotlin defaults: normalize blanks.
                            thickness = if (it.thickness > 0f) it.thickness
                            else if (it.type == "exterior") 0.25f else 0.15f,
                            type = it.type ?: "interior"
                        )
                    },
                    rooms = raw.rooms.map {
                        FloorPlanRoomLabel(it.name, it.cx * s, it.cy * s)
                    },
                    version = 2
                )
            }.getOrNull()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Plan constants
// ─────────────────────────────────────────────────────────────────────────────

/** Editable plan extent in meters (grid runs 0..EXTENT on both axes). */
private const val PLAN_EXTENT = 40f

/** Initial visible span (meters) when the editor opens. */
private const val BASE_SPAN = 12f

/** Snap quantum: 0.25 m per grid sub-cell. */
private const val SNAP_M = 0.25f

/** Minimum committed wall length in meters. */
private const val MIN_WALL_LEN = 0.30f

/** Zoom bounds in px-per-meter, refined by canvas width at runtime. */
private const val MAX_SCALE = 480f

/** Wall thickness presets (meters). */
private val THICKNESS_OPTIONS = listOf(0.10f, 0.15f, 0.20f, 0.25f, 0.30f)
private const val INTERIOR_DEFAULT = 0.15f
private const val EXTERIOR_DEFAULT = 0.25f

private enum class PlanTool { WALL, ROOM, ERASE, PAN }

/** Pan/zoom viewport: screen = plan * scale + offset. */
private data class Viewport(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float
)

private data class EditorSnapshot(
    val walls: List<FloorPlanWall>,
    val rooms: List<FloorPlanRoomLabel>
)

private fun snapToGrid(v: Float): Float =
    (Math.round(v / SNAP_M) * SNAP_M).coerceIn(0f, PLAN_EXTENT)

private fun snapPoint(p: Offset, walls: List<FloorPlanWall>, threshold: Float): Offset {
    // Snap-to-corner: existing endpoints win over the grid.
    var best: Offset? = null
    var bestD = threshold
    walls.forEach { w ->
        listOf(Offset(w.x1, w.y1), Offset(w.x2, w.y2)).forEach { corner ->
            val d = hypot(p.x - corner.x, p.y - corner.y)
            if (d < bestD) {
                bestD = d
                best = corner
            }
        }
    }
    best?.let { return it }
    return Offset(snapToGrid(p.x), snapToGrid(p.y))
}

/** Orthogonal / 45° angle constraint for the in-progress wall. */
private fun constrainAngle(start: Offset, end: Offset, ortho: Boolean): Offset {
    val dx = end.x - start.x
    val dy = end.y - start.y
    if (ortho) {
        return if (abs(dx) >= abs(dy)) Offset(end.x, start.y) else Offset(start.x, end.y)
    }
    val len = hypot(dx, dy)
    if (len < 0.2f) return end
    val angle = atan2(dy, dx)
    val quadrant = (kotlin.math.PI / 4)
    val target = kotlin.math.round(angle / quadrant) * quadrant
    if (abs(angle - target) < kotlin.math.PI / 22.5) { // within ~8°
        return start + Offset((cos(target) * len).toFloat(), (sin(target) * len).toFloat())
    }
    return end
}

private fun distPointToSegment(p: Offset, a: Offset, b: Offset): Float {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val lenSq = dx * dx + dy * dy
    val t = if (lenSq == 0f) 0f
    else (((p.x - a.x) * dx + (p.y - a.y) * dy) / lenSq).coerceIn(0f, 1f)
    return hypot(p.x - (a.x + t * dx), p.y - (a.y + t * dy))
}

// ─────────────────────────────────────────────────────────────────────────────
// Editor overlay
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen CAD-style floor plan editor: infinite pan/pinch-zoom canvas,
 * 1 m grid with 0.25 m snap, snap-to-corner, orthogonal and 45° angle
 * constraints, wall thickness/type control, room labels, erase, undo/redo,
 * live dimension readout and a scale bar. Saved as metric v2 JSON.
 *
 * @param initialJson previously saved plan (v1 or v2), or null for a blank canvas
 * @param onDone called with the serialized plan on save, or null on discard
 */
@Composable
fun FloorPlanMakerOverlay(
    initialJson: String?,
    onDone: (String?) -> Unit
) {
    val initial = remember { FloorPlanData.fromJson(initialJson) ?: FloorPlanData() }
    var plan by remember {
        mutableStateOf(EditorSnapshot(initial.walls, initial.rooms))
    }
    val undoStack = remember { mutableStateListOf<EditorSnapshot>() }
    val redoStack = remember { mutableStateListOf<EditorSnapshot>() }

    fun mutate(transform: (EditorSnapshot) -> EditorSnapshot) {
        undoStack.add(plan)
        if (undoStack.size > 60) undoStack.removeAt(0)
        redoStack.clear()
        plan = transform(plan)
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(plan)
            plan = undoStack.removeAt(undoStack.lastIndex)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(plan)
            plan = redoStack.removeAt(redoStack.lastIndex)
        }
    }

    var tool by remember { mutableStateOf(PlanTool.WALL) }
    var orthoLock by remember { mutableStateOf(true) }
    var wallType by remember { mutableStateOf("interior") }
    var wallThickness by remember { mutableStateOf(INTERIOR_DEFAULT) }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var viewport by remember { mutableStateOf(Viewport(60f, 0f, 0f)) }
    var viewportInitialized by remember { mutableStateOf(false) }
    var wallPreview by remember { mutableStateOf<Pair<Offset, Offset>?>(null) }
    var pendingRoomPoint by remember { mutableStateOf<Offset?>(null) }
    var eraseHit by remember { mutableStateOf(-1) }

    val density = LocalDensity.current
    val touchPx = with(density) { 26.dp.toPx() }
    val minScale: () -> Float = {
        canvasSize.width.coerceAtLeast(1) / (PLAN_EXTENT * 1.15f)
    }

    // Center the base span once the canvas has a real size.
    LaunchedEffect(canvasSize) {
        if (!viewportInitialized && canvasSize != IntSize.Zero) {
            viewportInitialized = true
            val s = canvasSize.width / (BASE_SPAN * 1.12f)
            val center = Offset(BASE_SPAN / 2f, BASE_SPAN / 2f)
            viewport = Viewport(
                scale = s,
                offsetX = canvasSize.width / 2f - center.x * s,
                offsetY = canvasSize.height / 2f - center.y * s
            )
        }
    }

    fun screenToPlan(screen: Offset): Offset = Offset(
        x = (screen.x - viewport.offsetX) / viewport.scale,
        y = (screen.y - viewport.offsetY) / viewport.scale
    )

    fun fitToPlan() {
        val walls = plan.walls
        if (walls.isEmpty()) {
            val s = canvasSize.width / (BASE_SPAN * 1.12f)
            viewport = Viewport(
                scale = s,
                offsetX = canvasSize.width / 2f - BASE_SPAN / 2f * s,
                offsetY = canvasSize.height / 2f - BASE_SPAN / 2f * s
            )
            return
        }
        val minX = walls.minOf { minOf(it.x1, it.x2) } - 1f
        val maxX = walls.maxOf { maxOf(it.x1, it.x2) } + 1f
        val minY = walls.minOf { minOf(it.y1, it.y2) } - 1f
        val maxY = walls.maxOf { maxOf(it.y1, it.y2) } + 1f
        val w = (maxX - minX).coerceAtLeast(2f)
        val h = (maxY - minY).coerceAtLeast(2f)
        val s = minOf(
            canvasSize.width.coerceAtLeast(1) / w,
            canvasSize.height.coerceAtLeast(1) / h
        )
        viewport = Viewport(
            scale = s,
            offsetX = canvasSize.width / 2f - (minX + w / 2f) * s,
            offsetY = canvasSize.height / 2f - (minY + h / 2f) * s
        )
    }

    fun zoomBy(factor: Float) {
        val c = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
        val newScale = (viewport.scale * factor).coerceIn(minScale(), MAX_SCALE)
        val k = newScale / viewport.scale
        viewport = Viewport(
            scale = newScale,
            offsetX = c.x * (1 - k) + viewport.offsetX * k,
            offsetY = c.y * (1 - k) + viewport.offsetY * k
        )
    }

    fun eraseAtPlan(p: Offset) {
        val hitPx = touchPx / viewport.scale
        val walls = plan.walls
        var found = -1
        var bestD = Float.MAX_VALUE
        walls.forEachIndexed { i, w ->
            val a = Offset(w.x1, w.y1)
            val b = Offset(w.x2, w.y2)
            val d = distPointToSegment(p, a, b) - w.thickness / 2f
            if (d < hitPx && d < bestD) {
                bestD = d
                found = i
            }
        }
        eraseHit = found
        if (found >= 0) {
            mutate { snapshot ->
                snapshot.copy(walls = snapshot.walls.filterIndexed { idx, _ -> idx != found })
            }
        }
    }

    Dialog(
        onDismissRequest = { onDone(null) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            // Edge-to-edge dialog so imePadding below receives real IME
            // insets instead of relying on the system resizing the window.
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DorjaColors.CanvasBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // ── Header ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onDone(null) }) {
                        Icon(Icons.Default.Close, contentDescription = L("common_cancel"), tint = DorjaColors.Ink950)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = L("floorplan_title"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DorjaColors.Ink950
                        )
                        Text(
                            text = L("floorplan_subtitle"),
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray600
                        )
                    }
                    IconButton(onClick = ::undo, enabled = undoStack.isNotEmpty()) {
                        Icon(Icons.Default.Undo, contentDescription = L("floorplan_undo"), tint = DorjaColors.Gray700)
                    }
                    IconButton(onClick = ::redo, enabled = redoStack.isNotEmpty()) {
                        Icon(Icons.Default.Redo, contentDescription = L("floorplan_redo"), tint = DorjaColors.Gray700)
                    }
                    IconButton(
                        onClick = { mutate { EditorSnapshot(emptyList(), emptyList()) } },
                        enabled = plan.walls.isNotEmpty() || plan.rooms.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = L("floorplan_clear"), tint = DorjaColors.Error)
                    }
                    IconButton(
                        onClick = {
                            onDone(FloorPlanData(plan.walls, plan.rooms, version = 2).toJson())
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = L("common_save"), tint = DorjaColors.Jol600)
                    }
                }

                // ── Tool row ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DorjaChip(
                        selected = tool == PlanTool.WALL,
                        label = L("floorplan_tool_wall"),
                        onClick = { tool = PlanTool.WALL }
                    )
                    DorjaChip(
                        selected = tool == PlanTool.ROOM,
                        label = L("floorplan_tool_room"),
                        onClick = { tool = PlanTool.ROOM }
                    )
                    DorjaChip(
                        selected = tool == PlanTool.ERASE,
                        label = L("floorplan_tool_erase"),
                        onClick = { tool = PlanTool.ERASE }
                    )
                    DorjaChip(
                        selected = tool == PlanTool.PAN,
                        label = L("floorplan_tool_pan"),
                        onClick = { tool = PlanTool.PAN }
                    )
                }

                // ── Context row: drawing controls ──
                if (tool == PlanTool.WALL) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DorjaChip(
                            selected = orthoLock,
                            label = L("floorplan_ortho"),
                            onClick = { orthoLock = !orthoLock }
                        )
                        DorjaChip(
                            selected = wallType == "interior",
                            label = L("floorplan_type_interior"),
                            onClick = {
                                wallType = "interior"
                                wallThickness = INTERIOR_DEFAULT
                            }
                        )
                        DorjaChip(
                            selected = wallType == "exterior",
                            label = L("floorplan_type_exterior"),
                            onClick = {
                                wallType = "exterior"
                                wallThickness = EXTERIOR_DEFAULT
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        DorjaChip(
                            selected = false,
                            label = L("floorplan_thickness") + " " +
                                formatMeters(wallThickness),
                            onClick = {
                                val idx = THICKNESS_OPTIONS.indexOf(wallThickness)
                                wallThickness = THICKNESS_OPTIONS[(idx + 1).mod(THICKNESS_OPTIONS.size)]
                            }
                        )
                    }
                }

                // Colors resolved in composition — DorjaColors getters are
                // @Composable and cannot be read inside the Canvas draw lambda.
                val editorWallColor = DorjaColors.Ink950
                val editorGridColor = DorjaColors.Sand300
                val editorGridMajor = DorjaColors.Gray500.copy(alpha = 0.55f)
                val editorAccentColor = DorjaColors.Jol600
                val editorLabelColor = DorjaColors.Gray700
                val editorEraseColor = DorjaColors.Error

                // ── Canvas ──
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = DorjaColors.White,
                    border = BorderStroke(1.dp, DorjaColors.BentoCardBorder)
                ) {
                    Box {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .onSizeChanged { canvasSize = it }
                                .pointerInput(tool, orthoLock) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown()
                                        val downScreen = down.position
                                        var lastScreen = downScreen
                                        var drawStart: Offset? = null
                                        if (tool == PlanTool.WALL) {
                                            drawStart = snapPoint(
                                                screenToPlan(downScreen),
                                                plan.walls,
                                                touchPx / viewport.scale
                                            )
                                        }
                                        var multi = false
                                        var prevCentroid = downScreen
                                        var prevSpan = 0f
                                        if (tool == PlanTool.PAN || tool == PlanTool.ERASE) {
                                            down.consume()
                                        }

                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val pressed = event.changes.filter { it.pressed }
                                            if (pressed.isEmpty()) break

                                            if (pressed.size >= 2) {
                                                // Two fingers → pan + pinch zoom.
                                                multi = true
                                                drawStart = null
                                                wallPreview = null
                                                eraseHit = -1
                                                val a = pressed[0]
                                                val b = pressed[1]
                                                val centroid = (a.position + b.position) / 2f
                                                val span = hypot(
                                                    a.position.x - b.position.x,
                                                    a.position.y - b.position.y
                                                )
                                                if (prevSpan > 0f) {
                                                    // Scale around the centroid...
                                                    val newScale =
                                                        (viewport.scale * (span / prevSpan))
                                                            .coerceIn(minScale(), MAX_SCALE)
                                                    val k = newScale / viewport.scale
                                                    viewport = Viewport(
                                                        scale = newScale,
                                                        offsetX = centroid.x * (1 - k) + viewport.offsetX * k,
                                                        offsetY = centroid.y * (1 - k) + viewport.offsetY * k
                                                    )
                                                    // ...then pan by centroid motion.
                                                    val delta = centroid - prevCentroid
                                                    viewport = viewport.copy(
                                                        offsetX = viewport.offsetX + delta.x,
                                                        offsetY = viewport.offsetY + delta.y
                                                    )
                                                }
                                                // First multi-touch frame only sets
                                                // the baseline — applying its delta
                                                // would make the canvas jump.
                                                prevCentroid = centroid
                                                prevSpan = span
                                                pressed.forEach { it.consume() }
                                            } else {
                                                val change = pressed[0]
                                                lastScreen = change.position
                                                when (tool) {
                                                    PlanTool.PAN -> {
                                                        val d = change.positionChange()
                                                        viewport = viewport.copy(
                                                            offsetX = viewport.offsetX + d.x,
                                                            offsetY = viewport.offsetY + d.y
                                                        )
                                                        change.consume()
                                                    }
                                                    PlanTool.WALL -> {
                                                        if (!multi) {
                                                            val raw = snapPoint(
                                                                screenToPlan(change.position),
                                                                plan.walls,
                                                                touchPx / viewport.scale
                                                            )
                                                            drawStart?.let { start ->
                                                                wallPreview = constrainAngle(start, raw, orthoLock)
                                                            }
                                                            change.consume()
                                                        }
                                                    }
                                                    PlanTool.ERASE -> {
                                                        if (!multi) {
                                                            eraseAtPlan(screenToPlan(change.position))
                                                            change.consume()
                                                        }
                                                    }
                                                    PlanTool.ROOM -> {
                                                        // Decided on release (tap).
                                                    }
                                                }
                                            }
                                        }

                                        // ── Gesture ended ──
                                        if (tool == PlanTool.WALL && !multi) {
                                            wallPreview?.let { (s, e) ->
                                                if (hypot(e.x - s.x, e.y - s.y) >= MIN_WALL_LEN) {
                                                    val wall = FloorPlanWall(
                                                        x1 = s.x.coerceIn(0f, PLAN_EXTENT),
                                                        y1 = s.y.coerceIn(0f, PLAN_EXTENT),
                                                        x2 = e.x.coerceIn(0f, PLAN_EXTENT),
                                                        y2 = e.y.coerceIn(0f, PLAN_EXTENT),
                                                        thickness = wallThickness,
                                                        type = wallType
                                                    )
                                                    mutate { it.copy(walls = it.walls + wall) }
                                                }
                                            }
                                            wallPreview = null
                                        }
                                        if (tool == PlanTool.ROOM && !multi) {
                                            val travel = hypot(
                                                lastScreen.x - downScreen.x,
                                                lastScreen.y - downScreen.y
                                            )
                                            if (travel < touchPx * 0.6f) {
                                                pendingRoomPoint = screenToPlan(lastScreen)
                                            }
                                        }
                                        eraseHit = -1
                                    }
                                }
                        ) {
                            drawPlanBody(
                                walls = plan.walls,
                                scale = viewport.scale,
                                offset = Offset(viewport.offsetX, viewport.offsetY),
                                gridColor = editorGridColor,
                                gridMajorColor = editorGridMajor,
                                wallColor = editorWallColor,
                                accentColor = editorAccentColor,
                                eraseColor = editorEraseColor,
                                preview = wallPreview,
                                eraseHitIndex = eraseHit,
                                showGrid = true
                            )

                            // Screen-space overlays: room labels, live
                            // dimension, scale bar (constant px sizes).
                            drawOverlays(
                                rooms = plan.rooms,
                                preview = wallPreview,
                                scale = viewport.scale,
                                offset = Offset(viewport.offsetX, viewport.offsetY),
                                labelColor = editorLabelColor,
                                accentColor = editorAccentColor
                            )
                        }

                        // Zoom controls (CAD corner cluster).
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OverlayToolButton(icon = Icons.Default.Add, desc = L("floorplan_zoom_in")) {
                                zoomBy(1.35f)
                            }
                            OverlayToolButton(icon = Icons.Default.Remove, desc = L("floorplan_zoom_out")) {
                                zoomBy(1f / 1.35f)
                            }
                            OverlayToolButton(icon = Icons.Default.CenterFocusStrong, desc = L("floorplan_zoom_fit")) {
                                fitToPlan()
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── Room name prompt — inline panel, no nested dialog ──
                // Placed ABOVE the save bar so it is never the bottom-most
                // element; combined with imePadding on the root Column it
                // always stays clear of the keyboard and a thumb's reach.
                pendingRoomPoint?.let { point ->
                    var name by remember(point) { mutableStateOf("") }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DorjaColors.White,
                        border = BorderStroke(1.dp, DorjaColors.BentoCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = L("floorplan_room_name"),
                                style = MaterialTheme.typography.labelSmall,
                                color = DorjaColors.Gray700
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            androidx.compose.material3.OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                singleLine = true,
                                placeholder = { Text(L("floorplan_room_hint")) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                DorjaButton(
                                    text = L("common_save"),
                                    onClick = {
                                        mutate { snapshot ->
                                            snapshot.copy(
                                                rooms = snapshot.rooms + FloorPlanRoomLabel(
                                                    name = name.ifBlank { "Room ${snapshot.rooms.size + 1}" },
                                                    cx = point.x.coerceIn(0f, PLAN_EXTENT),
                                                    cy = point.y.coerceIn(0f, PLAN_EXTENT)
                                                )
                                            )
                                        }
                                        pendingRoomPoint = null
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                DorjaOutlinedButton(
                                    text = L("common_cancel"),
                                    onClick = { pendingRoomPoint = null },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // ── Save bar ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DorjaButton(
                        text = L("floorplan_save"),
                        onClick = {
                            onDone(FloorPlanData(plan.walls, plan.rooms, version = 2).toJson())
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun OverlayToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = DorjaColors.White,
        border = BorderStroke(1.dp, DorjaColors.BentoCardBorder),
        shadowElevation = 2.dp
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = desc, tint = DorjaColors.Ink950, modifier = Modifier.size(18.dp))
        }
    }
}

/** "4.25 m" formatting without trailing zeros. */
private fun formatMeters(v: Float): String {
    val rounded = (v * 100).roundToLong() / 100.0
    return if (rounded == rounded.toLong().toDouble()) {
        "${rounded.toLong()} m"
    } else {
        String.format(java.util.Locale.US, "%.2f m", rounded)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Drawing
// ─────────────────────────────────────────────────────────────────────────────

/** Plan-space pass: grid + walls + preview, under the caller's transform. */
private fun DrawScope.drawPlanBody(
    walls: List<FloorPlanWall>,
    scale: Float,
    offset: Offset,
    gridColor: Color,
    gridMajorColor: Color,
    wallColor: Color,
    accentColor: Color,
    eraseColor: Color,
    preview: Pair<Offset, Offset>?,
    eraseHitIndex: Int,
    showGrid: Boolean
) {
    if (showGrid) {
        val minor = 1f / scale // 1 px after transform
        val visibleMinX = -offset.x / scale
        val visibleMaxX = (size.width - offset.x) / scale
        val visibleMinY = -offset.y / scale
        val visibleMaxY = (size.height - offset.y) / scale
        val fromX = visibleMinX.toInt().coerceIn(0, PLAN_EXTENT.toInt())
        val toX = (visibleMaxX.toInt() + 1).coerceIn(0, PLAN_EXTENT.toInt())
        val fromY = visibleMinY.toInt().coerceIn(0, PLAN_EXTENT.toInt())
        val toY = (visibleMaxY.toInt() + 1).coerceIn(0, PLAN_EXTENT.toInt())
        for (m in fromX..toX) {
            val major = m % 5 == 0
            drawLine(
                color = if (major) gridMajorColor else gridColor,
                start = Offset(m.toFloat(), 0f),
                end = Offset(m.toFloat(), PLAN_EXTENT),
                strokeWidth = if (major) minor * 1.6f else minor
            )
        }
        for (m in fromY..toY) {
            val major = m % 5 == 0
            drawLine(
                color = if (major) gridMajorColor else gridColor,
                start = Offset(0f, m.toFloat()),
                end = Offset(PLAN_EXTENT, m.toFloat()),
                strokeWidth = if (major) minor * 1.6f else minor
            )
        }
    }

    // Walls — strokeWidth is in meters; the transform scales it to px.
    walls.forEachIndexed { i, w ->
        drawLine(
            color = if (i == eraseHitIndex) eraseColor else wallColor,
            start = Offset(w.x1, w.y1),
            end = Offset(w.x2, w.y2),
            strokeWidth = w.thickness,
            cap = StrokeCap.Round
        )
    }

    // In-progress wall + endpoint handles.
    preview?.let { (s, e) ->
        drawLine(
            color = accentColor,
            start = s,
            end = e,
            strokeWidth = 0.08f,
            cap = StrokeCap.Round
        )
        drawCircle(accentColor, radius = 0.10f, center = s)
        drawCircle(accentColor, radius = 0.10f, center = e)
    }
}

/** Screen-space pass: labels, live dimension text, scale bar. */
private fun DrawScope.drawOverlays(
    rooms: List<FloorPlanRoomLabel>,
    preview: Pair<Offset, Offset>?,
    scale: Float,
    offset: Offset,
    labelColor: Color,
    accentColor: Color
) {
    fun toScreen(p: Offset): Offset = Offset(p.x * scale + offset.x, p.y * scale + offset.y)

    val textSize = 13.dp.toPx()
    val labelPaint = Paint().apply {
        color = android.graphics.Color.argb(
            (labelColor.alpha * 255).toInt(),
            (labelColor.red * 255).toInt(),
            (labelColor.green * 255).toInt(),
            (labelColor.blue * 255).toInt()
        )
        textSize = textSize
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    drawIntoCanvas { canvas ->
        rooms.forEach { room ->
            val sp = toScreen(Offset(room.cx, room.cy))
            if (sp.x in -100f..size.width + 100f && sp.y in -100f..size.height + 100f) {
                canvas.nativeCanvas.drawText(room.name, sp.x, sp.y + textSize / 3f, labelPaint)
            }
        }
    }

    // Live dimension while drawing.
    preview?.let { (s, e) ->
        val len = hypot(e.x - s.x, e.y - s.y)
        if (len > 0.05f) {
            val mid = toScreen(Offset((s.x + e.x) / 2f, (s.y + e.y) / 2f))
            val dimPaint = Paint().apply {
                color = android.graphics.Color.argb(
                    (accentColor.alpha * 255).toInt(),
                    (accentColor.red * 255).toInt(),
                    (accentColor.green * 255).toInt(),
                    (accentColor.blue * 255).toInt()
                )
                textSize = 12.dp.toPx()
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(formatMeters(len), mid.x, mid.y - 14.dp.toPx(), dimPaint)
            }
        }
    }

    // Scale bar: 1 m at current zoom, bottom-start.
    val barY = size.height - 18.dp.toPx()
    val startX = 16.dp.toPx()
    val endX = startX + scale // scale == px per meter
    val barPaint = Paint().apply {
        color = android.graphics.Color.argb(
            (labelColor.alpha * 255).toInt(),
            (labelColor.red * 255).toInt(),
            (labelColor.green * 255).toInt(),
            (labelColor.blue * 255).toInt()
        )
        strokeWidth = 2f
        isAntiAlias = true
    }
    drawIntoCanvas { canvas ->
        val n = canvas.nativeCanvas
        n.drawLine(startX, barY, endX, barY, barPaint)
        n.drawLine(startX, barY - 5f, startX, barY + 5f, barPaint)
        n.drawLine(endX, barY - 5f, endX, barY + 5f, barPaint)
        n.drawText("1 m", (startX + endX) / 2f, barY - 9.dp.toPx(), labelPaint)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Read-only preview
// ─────────────────────────────────────────────────────────────────────────────

/** Read-only floor plan renderer for listing detail cards. */
@Composable
fun FloorPlanPreview(
    planJson: String,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 4f / 3f
) {
    val plan = remember(planJson) { FloorPlanData.fromJson(planJson) ?: FloorPlanData() }
    // Resolved in composition (see note in FloorPlanMakerOverlay).
    val wallColor = DorjaColors.Ink950
    val gridColor = DorjaColors.Sand300
    val labelColor = DorjaColors.Gray700

    Canvas(modifier = modifier.fillMaxWidth().aspectRatio(aspectRatio)) {
        // Fit plan bounds (or the base span) into the card.
        val walls = plan.walls
        val minX: Float
        val maxX: Float
        val minY: Float
        val maxY: Float
        if (walls.isEmpty()) {
            minX = 0f; minY = 0f; maxX = BASE_SPAN; maxY = BASE_SPAN
        } else {
            minX = walls.minOf { minOf(it.x1, it.x2) } - 0.8f
            maxX = walls.maxOf { maxOf(it.x1, it.x2) } + 0.8f
            minY = walls.minOf { minOf(it.y1, it.y2) } - 0.8f
            maxY = walls.maxOf { maxOf(it.y1, it.y2) } + 0.8f
        }
        val w = (maxX - minX).coerceAtLeast(1f)
        val h = (maxY - minY).coerceAtLeast(1f)
        val s = minOf(size.width / w, size.height / h)
        val offset = Offset(
            (size.width - w * s) / 2f - minX * s,
            (size.height - h * s) / 2f - minY * s
        )

        drawPlanBody(
            walls = walls,
            scale = s,
            offset = offset,
            gridColor = gridColor,
            gridMajorColor = gridColor,
            wallColor = wallColor,
            accentColor = Color.Transparent,
            eraseColor = Color.Transparent,
            preview = null,
            eraseHitIndex = -1,
            showGrid = true
        )
        drawOverlays(
            rooms = plan.rooms,
            preview = null,
            scale = s,
            offset = offset,
            labelColor = labelColor,
            accentColor = Color.Transparent
        )
    }
}
