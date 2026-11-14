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
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
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
import com.example.ui.components.InAppNotificationBanner
import com.example.ui.components.LiquidGlassBackdrop
import com.example.ui.screens.*
import com.example.ui.theme.*
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
    val currentUser by MarketplaceRepository.currentUser.collectAsState()
    val appConfig by MarketplaceRepository.appConfig.collectAsState()
    val revocationNotice by MarketplaceRepository.revocationNotice.collectAsState()
    var isAppLoading by remember { mutableStateOf(true) }

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        delay(250)
        isAppLoading = false
    }

    // Account Revocation Dialog (instant notice if admin suspended, blocked, or deleted user account)
    if (revocationNotice != null) {
        AlertDialog(
            onDismissRequest = { MarketplaceRepository.clearRevocationNotice() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Block,
                        contentDescription = null,
                        tint = Color(0xFFEF4444)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Access Revoked", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                }
            },
            text = { Text(revocationNotice ?: "Your account has been deactivated by the administrator.") },
            confirmButton = {
                Button(
                    onClick = { MarketplaceRepository.clearRevocationNotice() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Understood", color = Color.White)
                }
            }
        )
    }

    val isAdmin = currentUser.role == "ADMIN" || currentUser.email.equals("mdmaheraz65@gmail.com", ignoreCase = true)
    
    // When forcefully update option is given, ONLY Admin (mdmaheraz65@gmail.com) can access upgraded mode; all other users are blocked.
    val isForceUpdateActive = appConfig.isForceUpdate && !isAdmin
    val isMaintenanceActive = appConfig.isMaintenanceMode && !isAdmin
    var showAdminLoginDialog by remember { mutableStateOf(false) }

    // Admin Upgraded Mode Access Dialog
    if (showAdminLoginDialog) {
        AlertDialog(
            onDismissRequest = { showAdminLoginDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = Color(0xFF2563EB)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Admin Upgraded Access", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Upgraded Mode is active. Only administrator (mdmaheraz65@gmail.com) has access while force update is enabled for all other users.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PrimaryPurpleLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Admin Account: mdmaheraz65@gmail.com",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        MarketplaceRepository.loginAsAdminDirectly()
                        showAdminLoginDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("Enter as Admin", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminLoginDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Crossfade(
        targetState = isAppLoading,
        animationSpec = tween(400),
        label = "splashCrossfade"
    ) { loading ->
        if (loading) {
            AppLoadingSplashScreen()
        } else if (isForceUpdateActive) {
            ForceUpdateScreen(
                appConfig = appConfig,
                onUpdateClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(appConfig.apkDownloadUrl))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Failed to open update URL", e)
                    }
                },
                onAdminLoginClick = {
                    showAdminLoginDialog = true
                }
            )
        } else if (isMaintenanceActive) {
            MaintenanceModeScreen(
                appConfig = appConfig,
                onRetryClick = {
                    // Trigger refresh
                }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                // Strict Startup Auth Check: If session exists -> Home screen; otherwise -> AuthScreen
                Crossfade(
                    targetState = isLoggedIn,
                    animationSpec = tween(300),
                    label = "authCrossfade"
                ) { authenticated ->
                    if (!authenticated) {
                        AuthScreen(
                            onAuthSuccess = {
                                // Authenticated state is handled reactively by isLoggedIn StateFlow
                            },
                            onDismiss = null // Strict wall: no guest bypass
                        )
                    } else {
                        MainAppContent(
                            onRequestLogin = {
                                MarketplaceRepository.logout()
                            }
                        )
                    }
                }

                // If Admin accesses upgraded mode or bypasses maintenance, display top notification pill
                if (isAdmin && (appConfig.isMaintenanceMode || appConfig.isForceUpdate)) {
                    val isUpgradeMode = appConfig.isForceUpdate
                    val pillColor = if (isUpgradeMode) Color(0xFF2563EB) else Color(0xFFF59E0B)
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = pillColor,
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isUpgradeMode) Icons.Outlined.Shield else Icons.Outlined.Build,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isUpgradeMode) "Upgraded Mode Active • Admin Access (mdmaheraz65@gmail.com)" else "Maintenance Mode Active • Admin Bypass",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MaintenanceModeScreen(
    appConfig: com.example.model.AppConfig,
    onRetryClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .windowInsetsPadding(WindowInsets.systemBars),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(90.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Build,
                        contentDescription = "Maintenance",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Under Maintenance",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Campus Book Marketplace System Upgrade",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E293B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Text(
                    text = appConfig.maintenanceMessage,
                    color = Color(0xFFE2E8F0),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(18.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:mdmaheraz65@gmail.com?subject=BookSwap%20Maintenance%20Query")
                        }
                        try { context.startActivity(emailIntent) } catch (_: Exception) {}
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
                ) {
                    Text("Support", color = Color(0xFF94A3B8), fontSize = 13.sp)
                }

                Button(
                    onClick = onRetryClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Check Status", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ForceUpdateScreen(
    appConfig: com.example.model.AppConfig,
    onUpdateClick: () -> Unit,
    onAdminLoginClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .windowInsetsPadding(WindowInsets.systemBars),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(90.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = Color(0xFF3B82F6).copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF3B82F6).copy(alpha = 0.4f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.SystemUpdate,
                        contentDescription = "Update Required",
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Update Required",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF3B82F6).copy(alpha = 0.2f)
            ) {
                Text(
                    text = "Version ${appConfig.latestAppVersion} Available",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF60A5FA),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E293B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "What's New:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = appConfig.updateNotes,
                        color = Color(0xFFE2E8F0),
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onUpdateClick,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Update Now", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onAdminLoginClick,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Admin Upgraded Access (mdmaheraz65@gmail.com)", color = Color(0xFF94A3B8), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
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
    val activeInAppAlert by MarketplaceRepository.activeInAppAlert.collectAsState()

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
                        },
                        onNavigateToChat = { convId ->
                            currentDestination = AppDestination.Chat(convId)
                        }
                    )
                }
            }
        }

        // Real-Time In-App Visual Alert Banner for incoming chat messages
        InAppNotificationBanner(
            alert = activeInAppAlert,
            onOpenChat = { convId ->
                currentDestination = AppDestination.Chat(convId)
            },
            onDismiss = {
                MarketplaceRepository.dismissInAppAlert()
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )

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
