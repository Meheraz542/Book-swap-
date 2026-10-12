package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.data.MarketplaceRepository
import com.example.model.BookSet
import com.example.model.Conversation
import com.example.model.Semester
import com.example.model.Technology
import com.example.ui.components.BottomNavTab
import com.example.ui.components.FloatingBottomNavBar
import com.example.ui.components.LiquidGlassBackdrop
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

sealed class AppDestination {
    data object Main : AppDestination()
    data class SetDetails(val set: BookSet) : AppDestination()
    data class Chat(val conversationId: String) : AppDestination()
    data object AdminPanel : AppDestination()
    data object Notifications : AppDestination()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val isLoggedIn by MarketplaceRepository.isLoggedIn.collectAsState()

    AnimatedContent(
        targetState = isLoggedIn,
        transitionSpec = {
            fadeIn(animationSpec = tween(350)) togetherWith fadeOut(animationSpec = tween(250))
        },
        label = "authTransition"
    ) { loggedIn ->
        if (!loggedIn) {
            AuthScreen(
                onAuthSuccess = {
                    // User authenticated, main app will display smoothly
                },
                onDismiss = {
                    MarketplaceRepository.restoreSession()
                }
            )
        } else {
            MainAppContent()
        }
    }
}

@Composable
private fun MainAppContent() {
    val conversations by MarketplaceRepository.conversations.collectAsState()
    val bookSets by MarketplaceRepository.bookSets.collectAsState()
    val notifications by MarketplaceRepository.notifications.collectAsState()

    val totalUnreadInbox = remember(conversations) {
        conversations.sumOf { it.unreadCount }
    }

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 5 })

    // Navigation state stack (Main vs sub-screens)
    var currentDestination by remember { mutableStateOf<AppDestination>(AppDestination.Main) }

    // Pre-filter state for search screen
    var searchTechFilter by remember { mutableStateOf<Technology?>(null) }
    var searchSemFilter by remember { mutableStateOf<Semester?>(null) }

    // Active bottom navigation tab directly derived from the pager's target or current page
    val currentTab = when (pagerState.currentPage) {
        0 -> BottomNavTab.HOME
        1 -> BottomNavTab.SEARCH
        2 -> BottomNavTab.SELL
        3 -> BottomNavTab.INBOX
        4 -> BottomNavTab.PROFILE
        else -> BottomNavTab.HOME
    }

    // Function to smoothly animate between tabs with iOS liquid spring physics
    val onSelectTab: (BottomNavTab) -> Unit = { tab ->
        val targetPage = when (tab) {
            BottomNavTab.HOME -> 0
            BottomNavTab.SEARCH -> 1
            BottomNavTab.SELL -> 2
            BottomNavTab.INBOX -> 3
            BottomNavTab.PROFILE -> 4
        }
        if (tab != BottomNavTab.SEARCH) {
            searchTechFilter = null
            searchSemFilter = null
        }
        coroutineScope.launch {
            pagerState.animateScrollToPage(
                page = targetPage,
                animationSpec = spring(
                    dampingRatio = 0.85f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    // Back handling
    BackHandler(enabled = currentDestination !is AppDestination.Main || pagerState.currentPage != 0) {
        if (currentDestination !is AppDestination.Main) {
            currentDestination = AppDestination.Main
        } else if (pagerState.currentPage != 0) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(
                    page = 0,
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                )
            }
        }
    }

    val showBottomBar = currentDestination is AppDestination.Main

    LiquidGlassBackdrop(modifier = Modifier.fillMaxSize()) {
        // Fluid Animated Screen Transitions across sub-screens
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = {
                val isOpeningSub = targetState !is AppDestination.Main && initialState is AppDestination.Main
                val isClosingSub = initialState !is AppDestination.Main && targetState is AppDestination.Main

                if (isOpeningSub) {
                    (slideInHorizontally(
                        initialOffsetX = { (it * 0.35f).toInt() },
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                    ) + fadeIn(animationSpec = tween(220)))
                        .togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { -(it * 0.15f).toInt() },
                                animationSpec = tween(180)
                            ) + fadeOut(animationSpec = tween(160))
                        )
                } else if (isClosingSub) {
                    (slideInHorizontally(
                        initialOffsetX = { -(it * 0.15f).toInt() },
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                    ) + fadeIn(animationSpec = tween(220)))
                        .togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { (it * 0.35f).toInt() },
                                animationSpec = tween(180)
                            ) + fadeOut(animationSpec = tween(160))
                        )
                } else {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(160))
                }
            },
            label = "screenDestinationTransition",
            modifier = Modifier.fillMaxSize()
        ) { dest ->
            when (dest) {
                is AppDestination.Main -> {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = false,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> {
                                HomeScreen(
                                    onNavigateToSearch = { tech, sem ->
                                        searchTechFilter = tech
                                        searchSemFilter = sem
                                        onSelectTab(BottomNavTab.SEARCH)
                                    },
                                    onNavigateToSetDetails = { set ->
                                        currentDestination = AppDestination.SetDetails(set)
                                    },
                                    onNavigateToProfile = {
                                        onSelectTab(BottomNavTab.PROFILE)
                                    },
                                    onOpenNotifications = {
                                        currentDestination = AppDestination.Notifications
                                    },
                                    onNavigateToSell = {
                                        onSelectTab(BottomNavTab.SELL)
                                    }
                                )
                            }
                            1 -> {
                                SearchScreen(
                                    initialTechnology = searchTechFilter,
                                    initialSemester = searchSemFilter,
                                    onNavigateBack = {
                                        onSelectTab(BottomNavTab.HOME)
                                    },
                                    onNavigateToSetDetails = { set ->
                                        currentDestination = AppDestination.SetDetails(set)
                                    }
                                )
                            }
                            2 -> {
                                SellScreen(
                                    onPublishSuccess = { newSet ->
                                        currentDestination = AppDestination.SetDetails(newSet)
                                    },
                                    onRequireLogin = {
                                        MarketplaceRepository.logout()
                                    }
                                )
                            }
                            3 -> {
                                InboxScreen(
                                    onNavigateToChat = { conv ->
                                        currentDestination = AppDestination.Chat(conv.id)
                                    },
                                    onNavigateToProfile = {
                                        onSelectTab(BottomNavTab.PROFILE)
                                    }
                                )
                            }
                            4 -> {
                                ProfileScreen(
                                    onNavigateToSetDetails = { set ->
                                        currentDestination = AppDestination.SetDetails(set)
                                    },
                                    onNavigateToAdminPanel = {
                                        currentDestination = AppDestination.AdminPanel
                                    },
                                    onRequireLogin = {
                                        MarketplaceRepository.logout()
                                    }
                                )
                            }
                        }
                    }
                }
                is AppDestination.SetDetails -> {
                    val liveSet = bookSets.find { it.id == dest.set.id } ?: dest.set
                    SetDetailsScreen(
                        bookSet = liveSet,
                        onNavigateBack = {
                            currentDestination = AppDestination.Main
                        },
                        onMessageSeller = { set ->
                            val convId = MarketplaceRepository.startOrGetConversation(set)
                            currentDestination = AppDestination.Chat(convId)
                        },
                        onMarkAsSold = { set ->
                            MarketplaceRepository.markSetAsSold(null, set.id)
                        }
                    )
                }
                is AppDestination.Chat -> {
                    ChatScreen(
                        conversationId = dest.conversationId,
                        onNavigateBack = {
                            currentDestination = AppDestination.Main
                            onSelectTab(BottomNavTab.INBOX)
                        },
                        onNavigateToSetDetails = { set ->
                            currentDestination = AppDestination.SetDetails(set)
                        }
                    )
                }
                is AppDestination.AdminPanel -> {
                    AdminPanelScreen(
                        onNavigateBack = {
                            currentDestination = AppDestination.Main
                            onSelectTab(BottomNavTab.PROFILE)
                        }
                    )
                }
                is AppDestination.Notifications -> {
                    NotificationScreen(
                        onNavigateBack = {
                            currentDestination = AppDestination.Main
                        },
                        onNavigateToSetDetails = { setId ->
                            val set = bookSets.find { it.id == setId }
                            if (set != null) {
                                currentDestination = AppDestination.SetDetails(set)
                            }
                        }
                    )
                }
            }
        }

        // Floating Neumorphic Bottom Navigation Bar with Spring Enter/Exit Animation
        AnimatedVisibility(
            visible = showBottomBar,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(animationSpec = tween(180)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(160)
            ) + fadeOut(animationSpec = tween(140)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            FloatingBottomNavBar(
                currentTab = currentTab,
                onTabSelected = onSelectTab,
                inboxUnreadCount = totalUnreadInbox
            )
        }
    }
}
