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
import androidx.compose.runtime.*
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

    Crossfade(
        targetState = isLoggedIn,
        animationSpec = tween(300),
        label = "authCrossfade"
    ) { loggedIn ->
        if (!loggedIn) {
            AuthScreen(
                onAuthSuccess = {
                    // User authenticated, main app will display smoothly
                },
                onDismiss = null
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

    // Active bottom navigation tab
    var currentTab by remember { mutableStateOf(BottomNavTab.HOME) }

    // Navigation state stack (Main vs sub-screens)
    var currentDestination by remember { mutableStateOf<AppDestination>(AppDestination.Main) }

    // Pre-filter state for search screen
    var searchTechFilter by remember { mutableStateOf<Technology?>(null) }
    var searchSemFilter by remember { mutableStateOf<Semester?>(null) }

    // Fast, responsive 60fps/120fps tab navigation
    val onSelectTab: (BottomNavTab) -> Unit = { tab ->
        if (tab != BottomNavTab.SEARCH) {
            searchTechFilter = null
            searchSemFilter = null
        }
        currentTab = tab
    }

    // Back handling
    BackHandler(enabled = currentDestination !is AppDestination.Main || currentTab != BottomNavTab.HOME) {
        if (currentDestination !is AppDestination.Main) {
            currentDestination = AppDestination.Main
        } else if (currentTab != BottomNavTab.HOME) {
            currentTab = BottomNavTab.HOME
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
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(160)) togetherWith fadeOut(animationSpec = tween(120))
                        },
                        label = "tabTransition",
                        modifier = Modifier.fillMaxSize()
                    ) { tab ->
                        when (tab) {
                            BottomNavTab.HOME -> {
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
                            BottomNavTab.SEARCH -> {
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
                            BottomNavTab.SELL -> {
                                SellScreen(
                                    onPublishSuccess = { newSet ->
                                        currentDestination = AppDestination.SetDetails(newSet)
                                    },
                                    onRequireLogin = {
                                        MarketplaceRepository.logout()
                                    }
                                )
                            }
                            BottomNavTab.INBOX -> {
                                InboxScreen(
                                    onNavigateToChat = { conv ->
                                        currentDestination = AppDestination.Chat(conv.id)
                                    },
                                    onNavigateToProfile = {
                                        onSelectTab(BottomNavTab.PROFILE)
                                    }
                                )
                            }
                            BottomNavTab.PROFILE -> {
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
