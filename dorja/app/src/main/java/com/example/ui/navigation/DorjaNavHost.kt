package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.example.ui.splash.SplashScreen
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
    object RelocationMode : Screen("relocation_mode?origin={origin}&dest={dest}") {
        fun createRoute(origin: String, destination: String) = "relocation_mode?origin=$origin&dest=$destination"
    }
}

enum class HostTab(val title: String, val icon: ImageVector, val tag: String) {
    PROPERTIES("Properties", Icons.Default.Home, "nav_tab_properties"),
    VISITS("Visits", Icons.Default.QrCode, "nav_tab_visits"),
    INBOX("Inbox", Icons.AutoMirrored.Filled.Chat, "nav_tab_inbox"),
    ACCOUNT("Account", Icons.Default.Person, "nav_tab_account")
}

enum class BuyerTab(val title: String, val icon: ImageVector, val tag: String) {
    EXPLORE("Explore", Icons.Default.Explore, "nav_tab_explore"),
    VISITS("Visits", Icons.Default.QrCode, "nav_tab_visits"),
    INBOX("Inbox", Icons.AutoMirrored.Filled.Chat, "nav_tab_inbox"),
    ACCOUNT("Account", Icons.Default.Person, "nav_tab_account")
}

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
    onNavigateToRelocation: (String, String) -> Unit = { _, _ -> }
) {
    val repository = DorjaApp.instance.repository
    val currentUser by repository.currentUser.collectAsState()
    val isHost = currentUser?.role == "SELLER"

    var currentHostTab by remember { mutableStateOf(HostTab.PROPERTIES) }
    var currentBuyerTab by remember { mutableStateOf(BuyerTab.EXPLORE) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = DorjaColors.White,
                tonalElevation = 0.dp
            ) {
                if (isHost) {
                    HostTab.values().forEach { tab ->
                        val isSelected = currentHostTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentHostTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DorjaColors.BentoBlueIcon,
                                selectedTextColor = DorjaColors.BentoBlueText,
                                indicatorColor = DorjaColors.BentoBlueBg,
                                unselectedIconColor = DorjaColors.Gray500,
                                unselectedTextColor = DorjaColors.Gray500
                            ),
                            modifier = Modifier.testTag(tab.tag)
                        )
                    }
                } else {
                    BuyerTab.values().forEach { tab ->
                        val isSelected = currentBuyerTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentBuyerTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DorjaColors.BentoBlueIcon,
                                selectedTextColor = DorjaColors.BentoBlueText,
                                indicatorColor = DorjaColors.BentoBlueBg,
                                unselectedIconColor = DorjaColors.Gray500,
                                unselectedTextColor = DorjaColors.Gray500
                            ),
                            modifier = Modifier.testTag(tab.tag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
                }
            }
        }
    }
}
