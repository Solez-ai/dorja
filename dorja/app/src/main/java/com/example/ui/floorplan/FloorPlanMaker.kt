package com.example.ui.floorplan

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import kotlin.math.hypot

/** One wall segment in normalized canvas coordinates (0f..1f). */
data class FloorPlanWall(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float
)

/** A text label anchored at a normalized canvas position. */
data class FloorPlanRoomLabel(
    val name: String,
    val cx: Float,
    val cy: Float
)

/** Full floor plan payload persisted as JSON in [com.example.data.model.Listing.floorPlanJson]. */
data class FloorPlanData(
    val walls: List<FloorPlanWall> = emptyList(),
    val rooms: List<FloorPlanRoomLabel> = emptyList()
) {
    fun toJson(): String = Gson().toJson(this)

    companion object {
        fun fromJson(json: String?): FloorPlanData? {
            if (json.isNullOrBlank()) return null
            return runCatching { Gson().fromJson(json, FloorPlanData::class.java) }.getOrNull()
        }
    }
}

/** Grid resolution for snapping: every coordinate snaps to a multiple of 1/[GRID_STEPS]. */
private const val GRID_STEPS = 24

private fun snap(v: Float): Float =
    (kotlin.math.round(v * GRID_STEPS) / GRID_STEPS).coerceIn(0f, 1f)

/** Small erase hit-range in normalized units. */
private const val ERASE_RADIUS = 0.035f

/**
 * Full-screen interactive floor plan editor. Sellers sketch walls with their
 * finger (snapped to a grid), drop room labels, erase mistakes, and save the
 * plan as compact JSON. Rendered as a full-screen dialog so no navigation
 * changes are needed.
 *
 * @param initialJson previously saved plan, or null for a blank canvas
 * @param onDone called with the serialized plan on save, or null on discard
 */
@Composable
fun FloorPlanMakerOverlay(
    initialJson: String?,
    onDone: (String?) -> Unit
) {
    val initial = remember { FloorPlanData.fromJson(initialJson) ?: FloorPlanData() }
    val walls = remember { mutableStateListOf<FloorPlanWall>().apply { addAll(initial.walls) } }
    val rooms = remember { mutableStateListOf<FloorPlanRoomLabel>().apply { addAll(initial.rooms) } }

    // WALL | ROOM | ERASE
    var mode by remember { mutableStateOf("WALL") }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }
    var pendingRoomPoint by remember { mutableStateOf<Offset?>(null) }

    fun normalized(offset: Offset): Offset = Offset(
        x = (offset.x / canvasSize.width.coerceAtLeast(1)).coerceIn(0f, 1f),
        y = (offset.y / canvasSize.height.coerceAtLeast(1)).coerceIn(0f, 1f)
    )

    Dialog(
        onDismissRequest = { onDone(null) },            properties = DialogProperties(
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
                    IconButton(onClick = { onDone(FloorPlanData(walls.toList(), rooms.toList()).toJson()) }) {
                        Icon(Icons.Default.Check, contentDescription = L("common_save"), tint = DorjaColors.Jol600)
                    }
                }

                // ── Tool row ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DorjaChip(
                        selected = mode == "WALL",
                        label = L("floorplan_tool_wall"),
                        onClick = { mode = "WALL" }
                    )
                    DorjaChip(
                        selected = mode == "ROOM",
                        label = L("floorplan_tool_room"),
                        onClick = { mode = "ROOM" }
                    )
                    DorjaChip(
                        selected = mode == "ERASE",
                        label = L("floorplan_tool_erase"),
                        onClick = { mode = "ERASE" }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            if (walls.isNotEmpty()) walls.removeAt(walls.size - 1)
                        },
                        enabled = walls.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = L("floorplan_undo"), tint = DorjaColors.Gray700)
                    }
                    IconButton(
                        onClick = {
                            walls.clear()
                            rooms.clear()
                        },
                        enabled = walls.isNotEmpty() || rooms.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = L("floorplan_clear"), tint = DorjaColors.Error)
                    }
                }

                // Colors resolved in composition — DorjaColors getters are
                // @Composable and cannot be read inside the Canvas draw lambda.
                val editorWallColor = DorjaColors.Ink950
                val editorGridColor = DorjaColors.Sand300
                val editorAccentColor = DorjaColors.Jol600
                val editorLabelColor = DorjaColors.Gray700

                // ── Canvas ──
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = DorjaColors.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DorjaColors.BentoCardBorder)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { canvasSize = it }
                            .pointerInput(mode) {
                                when (mode) {
                                    "WALL" -> detectDragGestures(
                                        onDragStart = { offset ->
                                            dragStart = Offset(snap(offset.x / size.width), snap(offset.y / size.height))
                                            dragEnd = null
                                        },
                                        onDrag = { change, _ ->
                                            dragEnd = Offset(snap(change.position.x / size.width), snap(change.position.y / size.height))
                                        },
                                        onDragEnd = {
                                            val s = dragStart
                                            val e = dragEnd
                                            if (s != null && e != null && hypot((e.x - s.x).toDouble(), (e.y - s.y).toDouble()) > 0.02) {
                                                walls.add(FloorPlanWall(s.x, s.y, e.x, e.y))
                                            }
                                            dragStart = null
                                            dragEnd = null
                                        }
                                    )
                                    "ROOM" -> detectTapGestures { offset ->
                                        pendingRoomPoint = normalized(offset)
                                    }
                                    "ERASE" -> detectTapGestures { offset ->
                                        val p = normalized(offset)
                                        val wallHit = walls.indexOfFirst { w ->
                                            distToSegment(p, w) < ERASE_RADIUS
                                        }
                                        if (wallHit >= 0) {
                                            walls.removeAt(wallHit)
                                        } else {
                                            val roomHit = rooms.indexOfFirst { r ->
                                                hypot((r.cx - p.x).toDouble(), (r.cy - p.y).toDouble()) < ERASE_RADIUS * 2
                                            }
                                            if (roomHit >= 0) rooms.removeAt(roomHit)
                                        }
                                    }
                                }
                            }
                    ) {
                        drawFloorPlan(
                            walls = walls,
                            rooms = rooms,
                            gridSteps = GRID_STEPS,
                            wallColor = editorWallColor,
                            gridColor = editorGridColor,
                            accentColor = editorAccentColor,
                            labelColor = editorLabelColor,
                            activeStart = dragStart,
                            activeEnd = dragEnd
                        )
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
                        border = androidx.compose.foundation.BorderStroke(1.dp, DorjaColors.BentoCardBorder),
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
                                        rooms.add(
                                            FloorPlanRoomLabel(
                                                name = name.ifBlank { "Room ${rooms.size + 1}" },
                                                cx = point.x,
                                                cy = point.y
                                            )
                                        )
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
                        onClick = { onDone(FloorPlanData(walls.toList(), rooms.toList()).toJson()) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

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
        drawFloorPlan(
            walls = plan.walls,
            rooms = plan.rooms,
            gridSteps = GRID_STEPS,
            wallColor = wallColor,
            gridColor = gridColor,
            accentColor = Color.Transparent,
            labelColor = labelColor,
            activeStart = null,
            activeEnd = null
        )
    }
}

private fun DrawScope.drawFloorPlan(
    walls: List<FloorPlanWall>,
    rooms: List<FloorPlanRoomLabel>,
    gridSteps: Int,
    wallColor: Color,
    gridColor: Color,
    accentColor: Color,
    labelColor: Color,
    activeStart: Offset?,
    activeEnd: Offset?
) {
    val w = size.width
    val h = size.height

    // Grid
    val gridStroke = 1f
    for (i in 1 until gridSteps) {
        val fx = i / gridSteps.toFloat()
        drawLine(gridColor, Offset(fx * w, 0f), Offset(fx * w, h), gridStroke)
        drawLine(gridColor, Offset(0f, fx * h), Offset(w, fx * h), gridStroke)
    }

    // Walls
    val stroke = w * 0.012f
    walls.forEach { wall ->
        drawLine(
            color = wallColor,
            start = Offset(wall.x1 * w, wall.y1 * h),
            end = Offset(wall.x2 * w, wall.y2 * h),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }

    // In-progress wall
    val s = activeStart
    val e = activeEnd
    if (s != null && e != null) {
        drawLine(
            color = accentColor,
            start = Offset(s.x * w, s.y * h),
            end = Offset(e.x * w, e.y * h),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    } else if (s != null) {
        drawCircle(accentColor, radius = stroke, center = Offset(s.x * w, s.y * h))
    }

    // Room labels
    val density = 42f * (w / 1000f).coerceIn(0.7f, 1.6f)
    val textPaint = Paint().apply {
        color = android.graphics.Color.argb(
            (labelColor.alpha * 255).toInt(),
            (labelColor.red * 255).toInt(),
            (labelColor.green * 255).toInt(),
            (labelColor.blue * 255).toInt()
        )
        textSize = density
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    drawIntoCanvas { canvas ->
        rooms.forEach { room ->
            canvas.nativeCanvas.drawText(
                room.name,
                room.cx * w,
                room.cy * h + density / 3f,
                textPaint
            )
        }
    }
}

/** Distance from point [p] to segment [seg] in normalized space (aspect-corrected for 4:3). */
private fun distToSegment(p: Offset, seg: FloorPlanWall): Float {
    val ax = seg.x1; val ay = seg.y1
    val bx = seg.x2; val by = seg.y2
    val dx = bx - ax
    val dy = by - ay
    val lenSq = dx * dx + dy * dy
    val raw = if (lenSq == 0f) 0f else ((p.x - ax) * dx + (p.y - ay) * dy) / lenSq
    val t = raw.coerceIn(0f, 1f)
    val cx = ax + t * dx
    val cy = ay + t * dy
    // Weight y more so erase feels consistent on a 4:3-ish canvas
    return hypot(((p.x - cx) * 1.0).toDouble(), ((p.y - cy) * 1.3).toDouble()).toFloat()
}
