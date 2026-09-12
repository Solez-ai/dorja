package com.example.ui.navigation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.DorjaApp
import com.example.ui.account.AccountScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.chat.ChatThreadScreen
import com.example.ui.chat.InboxScreen
import com.example.ui.detail.PropertyDetailScreen
import com.example.ui.explore.ExploreScreen
import com.example.ui.handover.HandoverPassportScreen
import com.example.ui.listing.CreateListingScreen
import com.example.ui.pass.ViewingPassScreen
import com.example.ui.relocation.RelocationModeScreen
import com.example.ui.seller.HostListingsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.splash.SplashScreen
import com.example.ui.components.DorjaLogo
import com.example.ui.i18n.L
import com.example.ui.theme.DorjaColors
import com.example.ui.tour.TourViewerScreen
import com.example.ui.scanner.RoomScannerScreen
import com.example.ui.visits.VisitsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Auth : Screen("auth")
    object Main : Screen("main")
    object PropertyDetail : Screen("property_detail/{listingId}") {
        fun createRoute(listingId: String) = "property_detail/$listingId"
    }
    object TourViewer : Screen("tour_viewer/{listingId}") {
        fun createRoute(listingId: String) = "tour_viewer/$listingId"
    }
    object CreateListing : Screen("create_listing")
    object ChatThread : Screen("chat_thread/{conversationId}") {
        fun createRoute(conversationId: String) = "chat_thread/$conversationId"
    }
    object ViewingPass : Screen("viewing_pass/{viewingId}") {
        fun createRoute(viewingId: String) = "viewing_pass/$viewingId"
    }
    object HandoverPassport : Screen("handover_passport/{listingId}") {
        fun createRoute(listingId: String) = "handover_passport/$listingId"
    }
    object RoomScanner : Screen("room_scanner/{listingId}") {
        fun createRoute(listingId: String) = "room_scanner/$listingId"
    }
    object GuidedCapture : Screen("guided_capture/{listingId}") {
        fun createRoute(listingId: String) = "guided_capture/$listingId"
    }
    object RelocationMode : Screen("relocation_mode?origin={origin}&dest={dest}") {
        fun createRoute(origin: String, destination: String) = "relocation_mode?origin=$origin&dest=$destination"
    }
}

enum class HostTab(val titleKey: String, val icon: ImageVector, val tag: String) {
    PROPERTIES("tab_properties", Icons.Default.Home, "nav_tab_properties"),
    VISITS("tab_visits", Icons.Default.QrCode, "nav_tab_visits"),
    INBOX("tab_inbox", Icons.AutoMirrored.Filled.Chat, "nav_tab_inbox"),
    ACCOUNT("tab_account", Icons.Default.Person, "nav_tab_account"),
    SETTINGS("tab_settings", Icons.Default.Settings, "nav_tab_settings")
}

enum class BuyerTab(val titleKey: String, val icon: ImageVector, val tag: String) {
    EXPLORE("tab_explore", Icons.Default.Explore, "nav_tab_explore"),
    VISITS("tab_visits", Icons.Default.QrCode, "nav_tab_visits"),
    INBOX("tab_inbox", Icons.AutoMirrored.Filled.Chat, "nav_tab_inbox"),
    ACCOUNT("tab_account", Icons.Default.Person, "nav_tab_account"),
    SETTINGS("tab_settings", Icons.Default.Settings, "nav_tab_settings")
}

/** Cubic-bezier(0.16, 1, 0.3, 1) — the smooth "expo out" easing requested for the drawer. */
private val DrawerEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private const val DRAWER_DURATION_MS = 350
private const val DRAWER_SCALE = 0.82f
private const val DRAWER_RADIUS_PX = 28f
private const val SIDEBAR_WIDTH_FRACTION = 0.78f

@Composable
fun DorjaNavHost() {
    val navController = rememberNavController()
    val repository = DorjaApp.instance.repository
    val currentUser by repository.currentUser.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onTimeout = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Auth.route) {
            AuthScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainContainer(
                onNavigateToDetail = { listingId ->
                    navController.navigate(Screen.PropertyDetail.createRoute(listingId))
                },
                onNavigateToTour = { listingId ->
                    navController.navigate(Screen.TourViewer.createRoute(listingId))
                },
                onNavigateToCreateListing = {
                    navController.navigate(Screen.CreateListing.route)
                },
                onNavigateToScanner = { listingId ->
                    navController.navigate(Screen.RoomScanner.createRoute(listingId))
                },
                onNavigateToChatThread = { conversationId ->
                    navController.navigate(Screen.ChatThread.createRoute(conversationId))
                },
                onNavigateToPass = { viewingId ->
                    navController.navigate(Screen.ViewingPass.createRoute(viewingId))
                },
                onNavigateToHandover = { listingId ->
                    navController.navigate(Screen.HandoverPassport.createRoute(listingId))
                },
                onNavigateToRelocation = { origin, dest ->
                    navController.navigate(Screen.RelocationMode.createRoute(origin, dest))
                },
                onLogout = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.PropertyDetail.route,
            arguments = listOf(navArgument("listingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId") ?: "l1"
            val scope = rememberCoroutineScope()
            PropertyDetailScreen(
                listingId = listingId,
                onBack = { navController.popBackStack() },
                onOpen3DTour = { id -> navController.navigate(Screen.TourViewer.createRoute(id)) },
                onOpenScanner = { id -> navController.navigate(Screen.RoomScanner.createRoute(id)) },
                onChatWithSeller = { id, seekerId, hostId ->
                    scope.launch(Dispatchers.IO) {
                        val conv = DorjaApp.instance.repository.getOrCreateConversation(id, seekerId, hostId)
                        withContext(Dispatchers.Main) {
                            navController.navigate(Screen.ChatThread.createRoute(conv.id))
                        }
                    }
                },
                onViewHandoverPassport = { id -> navController.navigate(Screen.HandoverPassport.createRoute(id)) }
            )
        }

        composable(
            route = Screen.TourViewer.route,
            arguments = listOf(navArgument("listingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId") ?: "l1"
            TourViewerScreen(
                listingId = listingId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.RoomScanner.route,
            arguments = listOf(navArgument("listingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId") ?: "l1"
            RoomScannerScreen(
                listingId = listingId,
                onBack = { navController.popBackStack() },
                onScanComplete = { _, _ ->
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.GuidedCapture.route,
            arguments = listOf(navArgument("listingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId") ?: "l1"
            GuidedCaptureScreen(
                listingId = listingId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CreateListing.route) {
            CreateListingScreen(
                onBack = { navController.popBackStack() },
                onListingCreated = { newListingId ->
                    navController.popBackStack()
                    navController.navigate(Screen.PropertyDetail.createRoute(newListingId))
                },
                onScanRooms = { listingId ->
                    navController.navigate(Screen.RoomScanner.createRoute(listingId))
                }
            )
        }

        composable(
            route = Screen.ChatThread.route,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: "c1"
            ChatThreadScreen(
                conversationId = conversationId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ViewingPass.route,
            arguments = listOf(navArgument("viewingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val viewingId = backStackEntry.arguments?.getString("viewingId") ?: "v1"
            ViewingPassScreen(
                viewingId = viewingId,
                isHost = currentUser?.role == "SELLER",
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.HandoverPassport.route,
            arguments = listOf(navArgument("listingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId") ?: "l3"
            HandoverPassportScreen(
                listingId = listingId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.RelocationMode.route,
            arguments = listOf(
                navArgument("origin") { type = NavType.StringType; defaultValue = "BD" },
                navArgument("dest") { type = NavType.StringType; defaultValue = "BD" }
            )
        ) { backStackEntry ->
            val origin = backStackEntry.arguments?.getString("origin") ?: "BD"
            val dest = backStackEntry.arguments?.getString("dest") ?: "BD"
            RelocationModeScreen(
                initialOrigin = origin,
                initialDestination = dest,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun MainContainer(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToTour: (String) -> Unit,
    onNavigateToCreateListing: () -> Unit,
    onNavigateToScanner: (String) -> Unit,
    onNavigateToChatThread: (String) -> Unit,
    onNavigateToPass: (String) -> Unit,
    onNavigateToHandover: (String) -> Unit,
    onNavigateToRelocation: (String, String) -> Unit = { _, _ -> },
    onLogout: () -> Unit = {}
) {
    val repository = DorjaApp.instance.repository
    val currentUser by repository.currentUser.collectAsState()
    val isHost = currentUser?.role == "SELLER"

    var currentHostTab by remember { mutableStateOf(HostTab.PROPERTIES) }
    var currentBuyerTab by remember { mutableStateOf(BuyerTab.EXPLORE) }
    var drawerOpen by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val progress by animateFloatAsState(
        targetValue = if (drawerOpen) 1f else 0f,
        animationSpec = tween(durationMillis = DRAWER_DURATION_MS, easing = DrawerEasing),
        label = "drawerProgress"
    )

    val activeTabTitle = L(if (isHost) currentHostTab.titleKey else currentBuyerTab.titleKey)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DorjaColors.DrawerBackdrop)
            .pointerInput(Unit) {
                // Edge-swipe: drag right starting within 48dp of the left edge
                // opens the drawer. Vertical scrolls are ignored because their
                // deltas arrive consumed by the scrolling child.
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var totalX = 0f
                    var openedThisGesture = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!event.changes.any { it.isConsumed }) {
                            totalX += change.positionChange().x
                        }
                        if (!openedThisGesture && !drawerOpen &&
                            down.position.x < 48.dp.toPx() &&
                            totalX > 72.dp.toPx()
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            drawerOpen = true
                            openedThisGesture = true
                        }
                        if (event.changes.none { it.pressed }) break
                    }
                }
            }
    ) {
        // ── Sidebar layer (sits behind the app card) ─────────────────────────
        DrawerSidebar(
            userName = currentUser?.displayName ?: "DORJA User",
            activeLabel = activeTabTitle,
            items = if (isHost) {
                HostTab.values().map { DrawerItem(L(it.titleKey), it.icon, it.tag) }
            } else {
                BuyerTab.values().map { DrawerItem(L(it.titleKey), it.icon, it.tag) }
            },
            onNavigate = { tag ->
                if (isHost) {
                    HostTab.values().firstOrNull { it.tag == tag }?.let { currentHostTab = it }
                } else {
                    BuyerTab.values().firstOrNull { it.tag == tag }?.let { currentBuyerTab = it }
                }
                drawerOpen = false
            },
            onClose = { drawerOpen = false },
            onLogout = {
                drawerOpen = false
                repository.logout()
                onLogout()
            }
        )

        // ── Main app card (scales down + slides right to reveal the drawer) ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val slide = size.width * SIDEBAR_WIDTH_FRACTION
                    translationX = slide * progress
                    val scale = 1f - (1f - DRAWER_SCALE) * progress
                    scaleX = scale
                    scaleY = scale
                    // Keep the card centered on the visible column while scaled.
                    translationX -= (size.width * (1f - scale) / 2f) * progress
                    shadowElevation = 24f * progress
                    shape = RoundedCornerShape((DRAWER_RADIUS_PX * progress).dp)
                    clip = progress > 0.01f
                }
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { if (drawerOpen) drawerOpen = false }
                .testTag("main_screen_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DorjaColors.CanvasBg)
            ) {
                // Compact top bar with hamburger replacing the old bottom nav bar.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            drawerOpen = true
                        },
                        modifier = Modifier.testTag("drawer_open_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = L("nav_open_menu"),
                            tint = DorjaColors.Ink950
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    DorjaLogo(modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DORJA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = DorjaColors.Ink950
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = activeTabTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = DorjaColors.Gray500,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (isHost) {
                        when (currentHostTab) {
                            HostTab.PROPERTIES -> HostListingsScreen(
                                onCreateListing = onNavigateToCreateListing,
                                onOpenListingDetail = onNavigateToDetail,
                                onScan3DRooms = onNavigateToScanner
                            )
                            HostTab.VISITS -> VisitsScreen(onOpenPass = onNavigateToPass)
                            HostTab.INBOX -> InboxScreen(onOpenConversation = onNavigateToChatThread)
                            HostTab.ACCOUNT -> AccountScreen(
                                onNavigateToSellerSuite = onNavigateToCreateListing,
                                onNavigateToRelocation = onNavigateToRelocation
                            )
                            HostTab.SETTINGS -> SettingsScreen()
                        }
                    } else {
                        when (currentBuyerTab) {
                            BuyerTab.EXPLORE -> ExploreScreen(onSelectListing = onNavigateToDetail)
                            BuyerTab.VISITS -> VisitsScreen(onOpenPass = onNavigateToPass)
                            BuyerTab.INBOX -> InboxScreen(onOpenConversation = onNavigateToChatThread)
                            BuyerTab.ACCOUNT -> AccountScreen(
                                onNavigateToSellerSuite = onNavigateToCreateListing,
                                onNavigateToRelocation = onNavigateToRelocation
                            )
                            BuyerTab.SETTINGS -> SettingsScreen()
                        }
                    }
                }

                // Touch-dismiss scrim inside the card: dims content and blocks
                // taps on the scaled-down screen while the drawer is open.
                if (progress > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = progress }
                            .background(Color.Black.copy(alpha = 0.45f))
                            .clickable(
                                indication = null,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            ) { drawerOpen = false }
                    )
                }
            }
        }
    }
}

private data class DrawerItem(
    val title: String,
    val icon: ImageVector,
    val tag: String
)

@Composable
private fun DrawerSidebar(
    userName: String,
    activeLabel: String,
    items: List<DrawerItem>,
    onNavigate: (String) -> Unit,
    onClose: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(SIDEBAR_WIDTH_FRACTION)
            .background(DorjaColors.DrawerSidebar)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 18.dp)
            .testTag("drawer_sidebar")
    ) {
        // Top row: logo + close button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(DorjaColors.DrawerSidebarSoft),
                contentAlignment = Alignment.Center
            ) {
                DorjaLogo(modifier = Modifier.size(28.dp), outlined = true)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = L("nav_close_menu"),
                    tint = DorjaColors.DrawerCream
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = userName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = DorjaColors.DrawerCream
        )
        Text(
            text = L("nav_verified_account"),
            style = MaterialTheme.typography.bodySmall,
            color = DorjaColors.DrawerMuted
        )

        Spacer(modifier = Modifier.height(26.dp))

        // Vertical navigation list
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEach { item ->
                val selected = item.title == activeLabel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) DorjaColors.DrawerSidebarSoft else Color.Transparent)
                        .clickable { onNavigate(item.tag) }
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .testTag(item.tag),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = if (selected) DorjaColors.DrawerAccent else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) DorjaColors.DrawerAccent else Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Logout action row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onLogout)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .testTag("drawer_logout_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = L("auth_logout"),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Footer brand mark
        Text(
            text = "DORJA",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
            color = DorjaColors.DrawerMuted
        )
        Text(
            text = L("nav_tagline"),
            style = MaterialTheme.typography.labelSmall,
            color = DorjaColors.DrawerMuted
        )
    }
}
