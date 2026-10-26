package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.delay

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
    var isGuestMode by remember { mutableStateOf(false) }
    var isAppLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(600)
        isAppLoading = false
    }

    Crossfade(
        targetState = isAppLoading,
        animationSpec = tween(400),
        label = "splashCrossfade"
    ) { loading ->
        if (loading) {
            AppLoadingSplashScreen()
        } else {
            val showMain = isLoggedIn || isGuestMode
            Crossfade(
                targetState = showMain,
                animationSpec = tween(300),
                label = "authCrossfade"
            ) { mainVisible ->
                if (!mainVisible) {
                    AuthScreen(
                        onAuthSuccess = {
                            isGuestMode = false
                        },
                        onDismiss = {
                            isGuestMode = true
                        }
                    )
                } else {
                    MainAppContent(
                        onRequestLogin = {
                            isGuestMode = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Premium in-app loading splash screen featuring the official app icon.
 */
@Composable
fun AppLoadingSplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF120B13)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color.Black,
                shadowElevation = 16.dp,
                modifier = Modifier
                    .size(112.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(26.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_bookswap_logo),
                    contentDescription = "BookSwap App Icon",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "BookSwap",
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 0.6.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Campus Book Marketplace",
                fontSize = 13.sp,
                color = Color(0xFFA1A1AA),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(30.dp))

            CircularProgressIndicator(
                color = Color(0xFFE11D48),
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp
            )
        }
    }
}

@Composable
private fun MainAppContent(
    onRequestLogin: () -> Unit = {}
) {
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
                                        onRequestLogin()
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
                                        onRequestLogin()
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
