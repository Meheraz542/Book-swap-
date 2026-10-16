package com.aistudio.bookset.market

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.bookset.market.data.model.BookSet
import com.aistudio.bookset.market.data.repository.MarketplaceRepository
import com.aistudio.bookset.market.ui.screens.AdminPanelScreen
import com.aistudio.bookset.market.ui.screens.AuthScreen
import com.aistudio.bookset.market.ui.screens.AuthViewModel
import com.aistudio.bookset.market.ui.screens.ChatScreen
import com.aistudio.bookset.market.ui.screens.DealsScreen
import com.aistudio.bookset.market.ui.screens.DetailScreen
import com.aistudio.bookset.market.ui.screens.HomeScreen
import com.aistudio.bookset.market.ui.screens.ProfileScreen
import com.aistudio.bookset.market.ui.theme.BookSetMarketTheme
import com.aistudio.bookset.market.ui.theme.NeumorphicBackground
import com.aistudio.bookset.market.ui.theme.NeumorphicDarkShadow
import com.aistudio.bookset.market.ui.theme.NeumorphicSurface
import com.aistudio.bookset.market.ui.theme.PrimaryBlue
import com.aistudio.bookset.market.ui.theme.TextMuted
import com.aistudio.bookset.market.ui.theme.TextPrimary

class MainActivity : ComponentActivity() {

    private lateinit var repository: MarketplaceRepository
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = MarketplaceRepository(applicationContext)
        authViewModel = AuthViewModel(application, repository)

        setContent {
            BookSetMarketTheme {
                MainApp(repository = repository, authViewModel = authViewModel)
            }
        }
    }
}

@Composable
fun MainApp(
    repository: MarketplaceRepository,
    authViewModel: AuthViewModel
) {
    val isLoggedIn by repository.isLoggedIn.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedBookSet by remember { mutableStateOf<BookSet?>(null) }
    var activeChatSeller by remember { mutableStateOf<String?>(null) }

    Crossfade(
        targetState = isLoggedIn,
        animationSpec = tween(300),
        label = "authCrossfade"
    ) { loggedIn ->
        if (!loggedIn) {
            AuthScreen(
                viewModel = authViewModel,
                onAuthSuccess = {
                    // Logged in
                }
            )
        } else {
            // Main Dashboard Flow
            if (activeChatSeller != null) {
                BackHandler { activeChatSeller = null }
                ChatScreen(
                    recipientName = activeChatSeller!!,
                    repository = repository,
                    onBack = { activeChatSeller = null }
                )
            } else if (selectedBookSet != null) {
                BackHandler { selectedBookSet = null }
                DetailScreen(
                    bookSet = selectedBookSet!!,
                    repository = repository,
                    onBack = { selectedBookSet = null },
                    onNavigateToChat = { seller ->
                        activeChatSeller = seller
                    }
                )
            } else {
                BackHandler(enabled = selectedTab != 0) {
                    selectedTab = 0
                }

                Scaffold(
                    bottomBar = {
                        NeumorphicBottomNav(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )
                    },
                    containerColor = NeumorphicBackground
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .statusBarsPadding()
                    ) {
                        when (selectedTab) {
                            0 -> HomeScreen(
                                repository = repository,
                                onBookSetClick = { selectedBookSet = it },
                                onViewDealsClick = { selectedTab = 1 }
                            )
                            1 -> DealsScreen(
                                repository = repository,
                                onBookSetClick = { selectedBookSet = it }
                            )
                            2 -> AdminPanelScreen(
                                repository = repository,
                                onBack = { selectedTab = 0 }
                            )
                            3 -> ProfileScreen(
                                repository = repository,
                                onNavigateToAdmin = { selectedTab = 2 },
                                onSignOut = {
                                    repository.signOut()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NeumorphicBottomNav(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = NeumorphicDarkShadow.copy(alpha = 0.5f),
                spotColor = NeumorphicDarkShadow.copy(alpha = 0.6f)
            )
            .background(NeumorphicSurface, RoundedCornerShape(26.dp))
            .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(26.dp))
            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                icon = Icons.Default.Home,
                label = "Home",
                isSelected = selectedTab == 0,
                testTag = "nav_home",
                onClick = { onTabSelected(0) }
            )
            NavItem(
                icon = Icons.Default.LocalOffer,
                label = "Deals",
                isSelected = selectedTab == 1,
                testTag = "nav_deals",
                onClick = { onTabSelected(1) }
            )
            NavItem(
                icon = Icons.Default.AdminPanelSettings,
                label = "Admin",
                isSelected = selectedTab == 2,
                testTag = "nav_admin",
                onClick = { onTabSelected(2) }
            )
            NavItem(
                icon = Icons.Default.Person,
                label = "Profile",
                isSelected = selectedTab == 3,
                testTag = "nav_profile",
                onClick = { onTabSelected(3) }
            )
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isSelected) {
                    Modifier
                        .shadow(2.dp, RoundedCornerShape(16.dp))
                        .background(PrimaryBlue.copy(alpha = 0.12f))
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ) { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) PrimaryBlue else TextMuted,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) PrimaryBlue else TextMuted
            )
        }
    }
}
