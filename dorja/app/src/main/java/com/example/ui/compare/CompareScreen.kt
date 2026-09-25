package com.example.ui.compare

import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.DorjaApp
import com.example.data.model.Listing
import com.example.data.model.RoomItem
import com.example.ui.components.DorjaLogo
import com.example.ui.i18n.L
import com.example.ui.theme.DorjaColors
import com.example.ui.theme.LocalDarkTheme
import com.example.ui.util.Formatters
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.tan
import kotlinx.coroutines.launch

/**
 * Buyer-only property comparison. Opens landscape-locked with the source
 * property on one side and a redesigned explore feed on the other; picking a
 * property enters the split-screen comparison view where ONE universal
 * scroller moves both panes together and both photo carousels swipe in sync.
 */
@Composable
fun CompareScreen(
    listingId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    // Landscape is the whole point of the comparison stage. Restoring the
    // previous orientation on dispose returns the app to whatever it was.
    DisposableEffectCompat(activity)

    val repository = DorjaApp.instance.repository
    val leftListing by repository.observeListingById(listingId).collectAsState(initial = null)
    val leftRooms by repository.getRoomsByListing(listingId).collectAsState(initial = emptyList())

    var compareTargetId by remember { mutableStateOf<String?>(null) }

    if (compareTargetId == null) {
        ComparePickStage(
            leftListing = leftListing,
            leftRooms = leftRooms,
            excludeId = listingId,
            onPick = { compareTargetId = it },
            onBack = onBack
        )
    } else {
        val targetId = compareTargetId!!
        val rightListing by repository.observeListingById(targetId).collectAsState(initial = null)
        val rightRooms by repository.getRoomsByListing(targetId).collectAsState(initial = emptyList())
        CompareSplitStage(
            leftListing = leftListing,
            leftRooms = leftRooms,
            rightListing = rightListing,
            rightRooms = rightRooms,
            onBack = onBack
        )
    }
}

@Composable
private fun DisposableEffectCompat(activity: ComponentActivity?) {
    androidx.compose.runtime.DisposableEffect(activity) {
        val original = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation =
                original ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}

/** iOS-flavours squircle stand-in: generous continuous-feel corner percent. */
private val SquircleShape = RoundedCornerShape(percent = 30)

/** Virtual scroll travel of the comparison panes, in pixels. */
private const val COMPARE_SCROLL_RANGE_PX = 1600f

/** Virtual scroll travel of the picker-stage feed, in pixels. */
private const val PICK_SCROLL_RANGE_PX = 900f

private const val MIN_FEED_ROWS = 4

// ═══════════════════════════════════════════════════════════════════════
//  Stage 1 — pick a property to compare against
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ComparePickStage(
    leftListing: Listing?,
    leftRooms: List<RoomItem>,
    excludeId: String,
    onPick: (String) -> Unit,
    onBack: () -> Unit
) {
    val repository = DorjaApp.instance.repository
    val allListings by repository.getAllListings().collectAsState(initial = emptyList())
    val dark = LocalDarkTheme.current

    // Same trust gate as the explore feed: listings from identity-unverified
    // owners never appear as comparison candidates.
    val verifiedOwnerIds = produceVerifiedOwners(allListings)
    val candidates = remember(allListings, verifiedOwnerIds, excludeId) {
        allListings.filter { it.id != excludeId && verifiedOwnerIds.contains(it.ownerId) }
    }

    // One shared scroller anchors the picker stage too, so the hand position
    // for "scrub both sides" carries over from the very first frame.
    val sharedScroll = remember { Animatable(0f) }
    val scrollVelocity = remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    DecayVelocity(scrollVelocity)

    val leftFeed = remember(leftListing, leftRooms) { buildCompareFeed(leftListing, leftRooms) }

    Box(
        Modifier
            .fillMaxSize()
            .background(if (dark) Color(0xFF0B1118) else DorjaColors.CanvasBg)
    ) {
        Row(Modifier.fillMaxSize()) {
            ComparePropertyPane(
                listing = leftListing,
                rooms = leftRooms,
                feed = leftFeed,
                sharedScroll = sharedScroll,
                scrollRangePx = PICK_SCROLL_RANGE_PX,
                scrollVelocity = scrollVelocity,
                photoIndex = 0,
                onPhotoSwipe = {},
                scrollPxScale = 0.9f,
                modifier = Modifier.weight(1f)
            )
            Box(
                Modifier
                    .width(22.dp)
                    .fillMaxHeight()
                    .background(if (dark) Color(0xFF05080C) else DorjaColors.Ink950.copy(alpha = 0.92f))
            )
            ComparePickFeedPane(
                candidates = candidates,
                sharedScroll = sharedScroll,
                onPick = onPick,
                modifier = Modifier.weight(1f)
            )
        }

        CompareTopBar(
            title = L("compare_pick_title"),
            onBack = onBack
        )
    }
}

/** Trust gate shared with ExploreScreen: only identity-verified owners' listings. */
@Composable
private fun produceVerifiedOwners(allListings: List<Listing>): Set<String> {
    val repository = DorjaApp.instance.repository
    val state = produceState(initialValue = emptySet<String>(), key1 = allListings) {
        val verified = mutableSetOf<String>()
        allListings.map { it.ownerId }.distinct().forEach { ownerId ->
            repository.getUserById(ownerId)?.let { user ->
                if (user.isIdentityVerified) verified.add(ownerId)
            }
        }
        value = verified
    }
    return state.value
}

/** Decays the scroll-velocity tracker every frame so the motion blur fades out. */
@Composable
private fun DecayVelocity(velocity: MutableState<Float>) {
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { }
            val current = velocity.value
            if (current > 0f) {
                val next = current * 0.86f
                velocity.value = if (next < 0.4f) 0f else next
            }
        }
    }
}

@Composable
private fun ComparePickFeedPane(
    candidates: List<Listing>,
    sharedScroll: Animatable<Float, AnimationVector1D>,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = LocalDarkTheme.current
    val scope = rememberCoroutineScope()

    Column(
        modifier
            .fillMaxHeight()
            .statusBarsPadding()
    ) {
        Text(
            text = L("compare_pick_choose"),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (dark) Color(0xFFF2F4F7) else DorjaColors.Ink950,
            modifier = Modifier.padding(start = 22.dp, top = 58.dp, bottom = 10.dp)
        )
        Box(
            Modifier
                .weight(1f)
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        val next = (sharedScroll.value - drag.y).coerceIn(0f, PICK_SCROLL_RANGE_PX)
                        scope.launch { sharedScroll.snapTo(next) }
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // The feed rides the same Animatable as the left pane, so
                    // the divider scrubber scrolls both sides together from
                    // the very first screen of the flow.
                    .graphicsLayer { translationY = -sharedScroll.value * 0.9f },
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                candidates.take(7).forEachIndexed { index, listing ->
                    ComparePickCard(
                        listing = listing,
                        ordinal = index,
                        onClick = { onPick(listing.id) }
                    )
                }
                if (candidates.isEmpty()) {
                    Text(
                        text = L("compare_pick_empty"),
                        color = if (dark) Color(0xFF8B93A0) else DorjaColors.Gray600,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 30.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparePickCard(
    listing: Listing,
    ordinal: Int,
    onClick: () -> Unit
) {
    val dark = LocalDarkTheme.current
    val leading = if (ordinal % 2 == 0) 22.dp else 64.dp
    Box(
        Modifier
            .padding(start = leading, end = 18.dp)
            .fillMaxWidth(0.72f)
            .height(120.dp)
            .clip(SquircleShape)
            .background(if (dark) Color(0xFF141C26) else DorjaColors.White)
            .border(1.dp, if (dark) Color(0xFF232E3C) else DorjaColors.BentoCardBorder, SquircleShape)
            .clickable(onClick = onClick)
    ) {
        if (!listing.coverPhotoUrl.isNullOrBlank()) {
            AsyncImage(
                model = listing.coverPhotoUrl,
                contentDescription = listing.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                (if (dark) Color(0xFF0B1118) else DorjaColors.CanvasBg).copy(alpha = 0.92f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        Column(Modifier.padding(16.dp)) {
            Text(
                text = listing.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (dark) Color(0xFFF2F4F7) else DorjaColors.Ink950,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = DorjaColors.Jol600,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = listing.publicArea,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (dark) Color(0xFF8B93A0) else DorjaColors.Gray600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = Formatters.formatPrice(listing.priceAmount, listing.currency, listing.intent),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = DorjaColors.Jol600
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Stage 2 — the split-screen comparison
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun CompareSplitStage(
    leftListing: Listing?,
    leftRooms: List<RoomItem>,
    rightListing: Listing?,
    rightRooms: List<RoomItem>,
    onBack: () -> Unit
) {
    val dark = LocalDarkTheme.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var show3dOverlay by remember { mutableStateOf(false) }
    // One-time coach banner explaining the synced gestures; auto-fades.
    var showCoach by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(6000)
        showCoach = false
    }

    // ── Universal scroll: ONE Animatable drives both panes ──
    val sharedScroll = remember { Animatable(0f) }
    val scrollVelocity = remember { mutableFloatStateOf(0f) }
    DecayVelocity(scrollVelocity)

    val leftFeed = remember(leftListing, leftRooms) { buildCompareFeed(leftListing, leftRooms) }
    val rightFeed = remember(rightListing, rightRooms) { buildCompareFeed(rightListing, rightRooms) }

    // ── Synced photo carousel: ONE index drives both panes ──
    var syncedPhotoIndex by remember { mutableIntStateOf(0) }
    val maxPhotoIndex = remember(leftListing, leftRooms, rightListing, rightRooms) {
        (max(heroPhotoCount(leftListing, leftRooms), heroPhotoCount(rightListing, rightRooms)) - 1)
            .coerceAtLeast(0)
    }
    val swipePhoto: (Int) -> Unit = { delta ->
        syncedPhotoIndex = (syncedPhotoIndex + delta).coerceIn(0, maxPhotoIndex)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val open3d: () -> Unit = { show3dOverlay = true }
    val has3d = (leftListing?.hasScan == true || leftRooms.any { it.has3DScan }) &&
        (rightListing?.hasScan == true || rightRooms.any { it.has3DScan })

    Box(
        Modifier
            .fillMaxSize()
            .background(if (dark) Color(0xFF0B1118) else DorjaColors.CanvasBg)
    ) {
        Row(Modifier.fillMaxSize()) {
            ComparePropertyPane(
                listing = leftListing,
                rooms = leftRooms,
                feed = leftFeed,
                sharedScroll = sharedScroll,
                scrollRangePx = COMPARE_SCROLL_RANGE_PX,
                scrollVelocity = scrollVelocity,
                photoIndex = syncedPhotoIndex,
                onPhotoSwipe = swipePhoto,
                onOpen3d = open3d,
                modifier = Modifier.weight(1f)
            )

            // ── The squircle divider + drag scroller + 3D button ──
            CompareDivider(
                sharedScroll = sharedScroll,
                scrollRangePx = COMPARE_SCROLL_RANGE_PX,
                scrollVelocity = scrollVelocity,
                scope = scope,
                haptic = haptic,
                onOpen3d = open3d,
                has3d = has3d
            )

            ComparePropertyPane(
                listing = rightListing,
                rooms = rightRooms,
                feed = rightFeed,
                sharedScroll = sharedScroll,
                scrollRangePx = COMPARE_SCROLL_RANGE_PX,
                scrollVelocity = scrollVelocity,
                photoIndex = syncedPhotoIndex,
                onPhotoSwipe = swipePhoto,
                onOpen3d = open3d,
                modifier = Modifier.weight(1f)
            )
        }

        CompareTopBar(
            title = L("compare_title"),
            onBack = onBack
        )

        if (showCoach) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.62f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .clickable { showCoach = false }
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = L("compare_sync_scroll"),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = L("compare_sync_photo"),
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        if (show3dOverlay) {
            DualPanoramaOverlay(
                leftListing = leftListing,
                leftRooms = leftRooms,
                rightListing = rightListing,
                rightRooms = rightRooms,
                onClose = { show3dOverlay = false }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Feed model — a shared row vocabulary so both panes line up
// ═══════════════════════════════════════════════════════════════════════

private const val COMPARE_ROW_HERO = 0
private const val COMPARE_ROW_ROOM = 1
private const val COMPARE_ROW_FACTS = 2
private const val COMPARE_ROW_PRICE = 3
private const val COMPARE_ROW_DESCRIPTION = 4

private data class CompareFeedRow(
    val kind: Int,
    val room: RoomItem? = null,
    val ordinal: Int = 0
)

private fun buildCompareFeed(listing: Listing?, rooms: List<RoomItem>): List<CompareFeedRow> {
    if (listing == null) return emptyList()
    val rows = mutableListOf<CompareFeedRow>()
    rows.add(CompareFeedRow(COMPARE_ROW_HERO))
    rooms.sortedBy { it.ordinal }.forEachIndexed { index, room ->
        if (!room.photoPath.isNullOrBlank()) rows.add(CompareFeedRow(COMPARE_ROW_ROOM, room, index))
    }
    rows.add(CompareFeedRow(COMPARE_ROW_FACTS))
    rows.add(CompareFeedRow(COMPARE_ROW_PRICE))
    rows.add(CompareFeedRow(COMPARE_ROW_DESCRIPTION))
    return rows
}

private fun heroPhotoCount(listing: Listing?, rooms: List<RoomItem>): Int {
    if (listing == null) return 0
    var count = if (listing.coverPhotoUrl.isNullOrBlank()) 0 else 1
    rooms.forEach { room -> if (!room.photoPath.isNullOrBlank()) count++ }
    return count
}

// ═══════════════════════════════════════════════════════════════════════
//  One property pane — receives every value from the stage so both panes
//  are guaranteed to move together.
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ComparePropertyPane(
    listing: Listing?,
    rooms: List<RoomItem>,
    feed: List<CompareFeedRow>,
    sharedScroll: Animatable<Float, AnimationVector1D>,
    scrollRangePx: Float,
    scrollVelocity: MutableState<Float>,
    photoIndex: Int,
    onPhotoSwipe: (Int) -> Unit,
    onOpen3d: () -> Unit = {},
    scrollPxScale: Float = 1f,
    modifier: Modifier = Modifier
) {
    val dark = LocalDarkTheme.current
    val scope = rememberCoroutineScope()
    val blur = (scrollVelocity.value / 40f).coerceIn(0f, 10f)
    val scrollProgress = if (scrollRangePx <= 0f) 0f else sharedScroll.value / scrollRangePx

    Box(
        modifier
            .fillMaxHeight()
            .pointerInput(scrollRangePx) {
                detectDragGestures { change, drag ->
                    change.consume()
                    if (scrollRangePx <= 0f) return@detectDragGestures
                    val next = (sharedScroll.value - drag.y).coerceIn(0f, scrollRangePx)
                    scrollVelocity.value = abs(drag.y)
                    scope.launch { sharedScroll.snapTo(next) }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Pure pixel translation keeps the two panes pixel-locked to
                // the finger and to each other.
                .graphicsLayer { translationY = -sharedScroll.value * scrollPxScale }
        ) {
            feed.forEach { row ->
                when (row.kind) {
                    COMPARE_ROW_HERO -> CompareHeroRow(
                        listing = listing,
                        rooms = rooms,
                        photoIndex = photoIndex,
                        onPhotoSwipe = onPhotoSwipe,
                        parallaxPx = sharedScroll.value * 0.35f * scrollPxScale,
                        blur = blur,
                        onOpen3d = onOpen3d
                    )
                    COMPARE_ROW_ROOM -> CompareRoomRow(
                        room = row.room ?: return@forEach,
                        ordinal = row.ordinal,
                        parallaxPx = sharedScroll.value * 0.12f * scrollPxScale,
                        blur = blur * 0.5f
                    )
                    COMPARE_ROW_FACTS -> CompareFactsRow(listing)
                    COMPARE_ROW_PRICE -> ComparePriceRow(listing)
                    COMPARE_ROW_DESCRIPTION -> CompareDescriptionRow(listing)
                }
            }
            Spacer(Modifier.height(120.dp))
        }

        // Progressive bottom veil: the deeper you scroll, the stronger the
        // fade at the pane's foot — part of the parallax feel.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(90.dp)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to (if (dark) Color(0xFF0B1118) else DorjaColors.CanvasBg)
                            .copy(alpha = 0.55f + 0.35f * scrollProgress)
                    )
                )
        )
    }
}

// ── Hero row: the big synced carousel ──

@Composable
private fun CompareHeroRow(
    listing: Listing?,
    rooms: List<RoomItem>,
    photoIndex: Int,
    onPhotoSwipe: (Int) -> Unit,
    parallaxPx: Float,
    blur: Float,
    onOpen3d: () -> Unit
) {
    if (listing == null) return
    val photos = remember(listing, rooms) {
        val list = mutableListOf<String>()
        if (!listing.coverPhotoUrl.isNullOrBlank()) list.add(listing.coverPhotoUrl)
        rooms.sortedBy { it.ordinal }.forEach { room ->
            if (!room.photoPath.isNullOrBlank() && !list.contains(room.photoPath)) {
                list.add(room.photoPath)
            }
        }
        list
    }
    val dark = LocalDarkTheme.current
    val safeIndex = if (photos.isEmpty()) 0 else photoIndex.coerceIn(0, photos.size - 1)
    val has3dScan = listing.hasScan || rooms.any { it.has3DScan }

    Box(
        Modifier
            .fillMaxWidth()
            .height(228.dp)
            .graphicsLayer { translationY = parallaxPx * 0.4f }
    ) {
        if (photos.isNotEmpty()) {
            AsyncImage(
                model = photos[safeIndex],
                contentDescription = listing.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (blur > 0.5f && Build.VERSION.SDK_INT >= 31) {
                            renderEffect = android.graphics.RenderEffect
                                .createBlurEffect(blur, blur, android.graphics.Shader.TileMode.CLAMP)
                                .asComposeRenderEffect()
                        } else {
                            renderEffect = null
                        }
                    }
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(if (dark) Color(0xFF141C26) else DorjaColors.Sand100)
            )
        }
        // Photo counter pill — iOS-style, floating over the photo.
        Surface(
            shape = RoundedCornerShape(50),
            color = Color.Black.copy(alpha = 0.55f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
        ) {
            Text(
                text = "${safeIndex + 1} / ${photos.size}",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
        if (photos.size > 1) {
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                photos.indices.forEach { i ->
                    Box(
                        Modifier
                            .size(if (i == safeIndex) 7.dp else 5.dp)
                            .clip(CircleShape)
                            .background(if (i == safeIndex) Color.White else Color.White.copy(alpha = 0.45f))
                    )
                }
            }
        }
        // "See 3D scan" — pressing it on EITHER side opens BOTH scans.
        if (has3dScan) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                onClick = onOpen3d,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ViewInAr,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = L("compare_see_3d"),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        // Swipe anywhere on the photo — both carousels move together.
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(photos.size) {
                    var accumulated = 0f
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        if (photos.size < 2) return@detectHorizontalDragGestures
                        accumulated += dragAmount
                        if (abs(accumulated) > 48f) {
                            onPhotoSwipe(if (accumulated < 0) 1 else -1)
                            accumulated = 0f
                        }
                    }
                }
        )
    }
}

// ── Room row ──

@Composable
private fun CompareRoomRow(
    room: RoomItem,
    ordinal: Int,
    parallaxPx: Float,
    blur: Float
) {
    val dark = LocalDarkTheme.current
    val leading = if (ordinal % 2 == 0) 20.dp else 56.dp
    Column(
        Modifier
            .padding(start = leading, end = 20.dp, top = 16.dp)
            .fillMaxWidth()
            .graphicsLayer { translationY = parallaxPx }
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(SquircleShape)
                .background(if (dark) Color(0xFF141C26) else DorjaColors.Sand100)
        ) {
            if (!room.photoPath.isNullOrBlank()) {
                AsyncImage(
                    model = room.photoPath,
                    contentDescription = room.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (blur > 0.5f && Build.VERSION.SDK_INT >= 31) {
                                renderEffect = android.graphics.RenderEffect
                                    .createBlurEffect(blur, blur, android.graphics.Shader.TileMode.CLAMP)
                                    .asComposeRenderEffect()
                            } else {
                                renderEffect = null
                            }
                        }
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = room.displayName,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
            if (room.has3DScan) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = DorjaColors.Jol600,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "3D",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
        if (room.dimensions.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = room.dimensions,
                style = MaterialTheme.typography.labelSmall,
                color = if (dark) Color(0xFF8B93A0) else DorjaColors.Gray600
            )
        }
    }
}

// ── Facts row ──

@Composable
private fun CompareFactsRow(listing: Listing?) {
    if (listing == null) return
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CompareFactTile(Icons.Default.Bed, "${listing.bedrooms}", L("detail_bedrooms"), Modifier.weight(1f))
        CompareFactTile(Icons.Default.Bathtub, "${listing.bathrooms}", L("detail_bathrooms"), Modifier.weight(1f))
        CompareFactTile(Icons.Default.SquareFoot, "${listing.sqft}", "sqft", Modifier.weight(1f))
    }
}

@Composable
private fun CompareFactTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val dark = LocalDarkTheme.current
    Column(
        modifier
            .clip(SquircleShape)
            .background(if (dark) Color(0xFF141C26) else DorjaColors.White)
            .border(1.dp, if (dark) Color(0xFF232E3C) else DorjaColors.BentoCardBorder, SquircleShape)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = DorjaColors.Jol600, modifier = Modifier.size(17.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            color = if (dark) Color(0xFFF2F4F7) else DorjaColors.Ink950
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (dark) Color(0xFF8B93A0) else DorjaColors.Gray600
        )
    }
}

// ── Price row ──

@Composable
private fun ComparePriceRow(listing: Listing?) {
    if (listing == null) return
    val dark = LocalDarkTheme.current
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp)) {
        Text(
            text = Formatters.formatPrice(listing.priceAmount, listing.currency, listing.intent),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = DorjaColors.Jol600
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (listing.intent == "RENT") L("detail_for_rent") else L("detail_for_sale"),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (dark) Color(0xFF8B93A0) else DorjaColors.Gray600
        )
    }
}

// ── Description row ──

@Composable
private fun CompareDescriptionRow(listing: Listing?) {
    if (listing == null) return
    val dark = LocalDarkTheme.current
    val text = listing.description.ifBlank { listing.tags }
    if (text.isBlank()) return
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (dark) Color(0xFFC8CDD6) else DorjaColors.Gray700,
        lineHeight = 17.sp,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp)
    )
}

// ═══════════════════════════════════════════════════════════════════════
//  The squircle divider: draggable sync scroller + 3D button
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun CompareDivider(
    sharedScroll: Animatable<Float, AnimationVector1D>,
    scrollRangePx: Float,
    scrollVelocity: MutableState<Float>,
    scope: kotlinx.coroutines.CoroutineScope,
    haptic: HapticFeedback,
    onOpen3d: () -> Unit,
    has3d: Boolean
) {
    val dark = LocalDarkTheme.current
    // Track position 0..1 mirrors the shared scroll so the thumb always shows
    // where you are in the comparison.
    val trackProgress = if (scrollRangePx <= 0f) 0f else sharedScroll.value / scrollRangePx
    val hapticTick = remember { mutableIntStateOf(0) }

    LaunchedEffect(sharedScroll.value) {
        val step = (sharedScroll.value / 60f).toInt()
        if (step != hapticTick.intValue) {
            hapticTick.intValue = step
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Box(
        Modifier
            .width(44.dp)
            .fillMaxHeight()
            .pointerInput(scrollRangePx) {
                detectDragGestures { change, drag ->
                    change.consume()
                    if (scrollRangePx <= 0f) return@detectDragGestures
                    val next = (sharedScroll.value - drag.y).coerceIn(0f, scrollRangePx)
                    scrollVelocity.value = abs(drag.y)
                    scope.launch { sharedScroll.snapTo(next) }
                }
            }
    ) {
        // Squircle track — the "line" between the panes.
        Box(
            Modifier
                .align(Alignment.Center)
                .width(6.dp)
                .fillMaxHeight(0.86f)
                .clip(SquircleShape)
                .background(if (dark) Color(0xFF232E3C) else DorjaColors.Sand300)
        )
        // Draggable thumb with grip marks.
        Box(
            Modifier
                .align(Alignment.Center)
                .offset(y = ((trackProgress - 0.5f) * 240).dp)
                .width(26.dp)
                .height(92.dp)
                .clip(SquircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(DorjaColors.Jol600, if (dark) Color(0xFF2F81F7) else DorjaColors.Jol700)
                    )
                )
                .border(2.dp, Color.White.copy(alpha = 0.85f), SquircleShape)
        ) {
            Column(
                Modifier.align(Alignment.Center),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(3) {
                    Box(
                        Modifier
                            .width(12.dp)
                            .height(2.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.8f))
                    )
                }
            }
        }
        // 3D button, centered on the squircle scroller.
        Surface(
            shape = CircleShape,
            color = if (has3d) DorjaColors.Jol600 else (if (dark) Color(0xFF232E3C) else DorjaColors.Gray300),
            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.9f)),
            shadowElevation = 8.dp,
            onClick = { if (has3d) onOpen3d() },
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 84.dp)
                .size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.ViewInAr,
                    contentDescription = L("compare_open_3d"),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Top bar
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun CompareTopBar(
    title: String,
    onBack: () -> Unit
) {
    val dark = LocalDarkTheme.current
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = if (dark) Color(0xFF141C26).copy(alpha = 0.92f) else DorjaColors.White.copy(alpha = 0.94f),
            shadowElevation = 6.dp,
            onClick = onBack
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = L("common_back"),
                tint = if (dark) Color(0xFFF2F4F7) else DorjaColors.Ink950,
                modifier = Modifier
                    .padding(10.dp)
                    .size(20.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Surface(
            shape = CircleShape,
            color = if (dark) Color(0xFF141C26).copy(alpha = 0.92f) else DorjaColors.White.copy(alpha = 0.94f),
            shadowElevation = 6.dp
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DorjaLogo(modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = if (dark) Color(0xFFF2F4F7) else DorjaColors.Ink950
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Dual 3D overlay — both panoramas, one shared look direction
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun DualPanoramaOverlay(
    leftListing: Listing?,
    leftRooms: List<RoomItem>,
    rightListing: Listing?,
    rightRooms: List<RoomItem>,
    onClose: () -> Unit
) {
    val leftRoom = leftRooms.firstOrNull { it.has3DScan && it.panoramaData.isNotBlank() }
        ?: leftRooms.firstOrNull()
    val rightRoom = rightRooms.firstOrNull { it.has3DScan && it.panoramaData.isNotBlank() }
        ?: rightRooms.firstOrNull()

    // One shared yaw/pitch/fov drives both projections so comparing rooms
    // feels like turning your head in both spaces at once.
    var panYawDeg by remember { mutableFloatStateOf(0f) }
    var panPitchDeg by remember { mutableFloatStateOf(0f) }
    var fovDeg by remember { mutableFloatStateOf(75f) }

    val leftBitmap = rememberPanoramaBitmap(leftRoom)
    val rightBitmap = rememberPanoramaBitmap(rightRoom)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    fovDeg = (fovDeg / zoom).coerceIn(25f, 105f)
                    val pxToDeg = fovDeg / size.width.toFloat()
                    panYawDeg = ((panYawDeg - pan.x * pxToDeg + 540f) % 360f) - 180f
                    panPitchDeg = (panPitchDeg + pan.y * pxToDeg).coerceIn(-85f, 85f)
                }
            }
    ) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxHeight()) {
                PanoramaProjection(
                    bitmap = leftBitmap,
                    panYawDeg = panYawDeg,
                    panPitchDeg = panPitchDeg,
                    fovDeg = fovDeg
                )
                if (leftListing != null) {
                    Text(
                        text = leftListing.title,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(start = 52.dp, top = 6.dp)
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                PanoramaProjection(
                    bitmap = rightBitmap,
                    panYawDeg = panYawDeg,
                    panPitchDeg = panPitchDeg,
                    fovDeg = fovDeg
                )
                if (rightListing != null) {
                    Text(
                        text = rightListing.title,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(start = 14.dp, top = 6.dp)
                    )
                }
            }
        }
        // Thin seam between the two 3D views.
        Box(
            Modifier
                .align(Alignment.Center)
                .width(2.dp)
                .fillMaxHeight()
                .background(Color.White.copy(alpha = 0.65f))
        )
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.55f),
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = L("common_close"),
                tint = Color.White,
                modifier = Modifier
                    .padding(10.dp)
                    .size(20.dp)
            )
        }
        Text(
            text = L("compare_3d_hint"),
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun rememberPanoramaBitmap(room: RoomItem?): ImageBitmap? {
    val path = remember(room) {
        try {
            room?.panoramaData?.let { jsonStr ->
                org.json.JSONObject(jsonStr).optString("stitchedPanorama", null)
            }
        } catch (_: Exception) {
            null
        }
    }
    return remember(path) {
        try {
            path?.let { p ->
                val f = File(p)
                if (!f.exists()) return@let null
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(p, opts)
                val maxW = 2048
                val sample = (opts.outWidth / maxW).coerceAtLeast(1)
                BitmapFactory.decodeFile(p, BitmapFactory.Options().apply { inSampleSize = sample })
                    ?.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Cylindrical column-slice projection — the same math as the full-screen
 * viewer, adapted to render into an arbitrary pane so both panoramas can
 * share one look direction.
 */
@Composable
private fun PanoramaProjection(
    bitmap: ImageBitmap?,
    panYawDeg: Float,
    panPitchDeg: Float,
    fovDeg: Float
) {
    if (bitmap == null) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF10161D)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = L("compare_3d_missing"),
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelMedium
            )
        }
        return
    }
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        val cw = size.width
        val ch = size.height
        val bmpWi = bitmap.width
        val bmpHi = bitmap.height

        val focalLen = (cw / 2f) / tan(Math.toRadians(fovDeg / 2.0)).toFloat()
        val halfPi = (PI / 2f).toFloat()
        val twoPi = (2f * PI).toFloat()

        val edgeLat = atan((ch / 2f) / focalLen)
        val latCenter = Math.toRadians(panPitchDeg.toDouble()).toFloat()
        val latTop = (latCenter + edgeLat).coerceIn(-halfPi, halfPi)
        val latBottom = (latCenter - edgeLat).coerceIn(-halfPi, halfPi)

        val srcTopY = ((halfPi - latTop) / PI.toFloat() * bmpHi).toInt().coerceIn(0, bmpHi - 1)
        val srcBottomY = ((halfPi - latBottom) / PI.toFloat() * bmpHi).toInt().coerceIn(0, bmpHi)
        val sliceH = max(1, srcBottomY - srcTopY)

        val stripW = 3f
        var sx = 0f
        while (sx < cw) {
            val screenAngleX = atan((sx + stripW / 2f - cw / 2f) / focalLen)
            val lonRad = screenAngleX + Math.toRadians(panYawDeg.toDouble()).toFloat()
            val normLon = (lonRad + PI.toFloat()) / twoPi
            var srcX = (normLon * bmpWi).toInt() % bmpWi
            if (srcX < 0) srcX += bmpWi

            drawImage(
                image = bitmap,
                srcOffset = IntOffset(srcX, srcTopY),
                srcSize = IntSize(1, sliceH),
                dstOffset = IntOffset(sx.roundToInt(), 0),
                dstSize = IntSize(stripW.roundToInt() + 1, ch.roundToInt())
            )
            sx += stripW
        }
    }
}
