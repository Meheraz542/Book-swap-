package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MarketplaceRepository
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

/**
 * Super Admin & Developer Management Console.
 *
 * Provides full-spectrum campus administration:
 * - Live App Traffic & Daily Visitor Analytics (DAU, Visits, Book Checks, Peak Hours).
 * - Comprehensive User Directory (Name, Phone, Email, College, Department, Semester).
 * - Immediate Moderation Actions: Suspend, Block, or Reactivate any student.
 * - Complete Book Listing Moderation (Status, Seller info, Remove, Restore, Delete).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allUsers by MarketplaceRepository.allUsers.collectAsState()
    val bookSets by MarketplaceRepository.bookSets.collectAsState()
    val megaDeals by MarketplaceRepository.megaDeals.collectAsState()
    val trafficStats by MarketplaceRepository.trafficStats.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Traffic & Analytics, 1: Users, 2: Listings
    var userSearchQuery by remember { mutableStateOf("") }
    var userFilterStatus by remember { mutableStateOf("ALL") } // ALL, ACTIVE, SUSPENDED, BLOCKED
    var listingSearchQuery by remember { mutableStateOf("") }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var showAddListingDialog by remember { mutableStateOf(false) }

    val filteredUsers = remember(allUsers, userSearchQuery, userFilterStatus) {
        allUsers.filter { user ->
            val matchesQuery = userSearchQuery.isBlank() ||
                    user.name.contains(userSearchQuery, ignoreCase = true) ||
                    user.email.contains(userSearchQuery, ignoreCase = true) ||
                    user.phone.contains(userSearchQuery, ignoreCase = true) ||
                    user.college.contains(userSearchQuery, ignoreCase = true) ||
                    user.technology.displayName.contains(userSearchQuery, ignoreCase = true)

            val matchesStatus = when (userFilterStatus) {
                "ACTIVE" -> user.accountStatus == "ACTIVE"
                "SUSPENDED" -> user.accountStatus == "SUSPENDED"
                "BLOCKED" -> user.accountStatus == "BLOCKED" || user.accountStatus == "BANNED"
                else -> true
            }
            matchesQuery && matchesStatus
        }
    }

    val filteredListings = remember(bookSets, listingSearchQuery) {
        bookSets.filter { set ->
            listingSearchQuery.isBlank() ||
                    set.title.contains(listingSearchQuery, ignoreCase = true) ||
                    set.sellerName.contains(listingSearchQuery, ignoreCase = true) ||
                    set.sellerPhone.contains(listingSearchQuery, ignoreCase = true) ||
                    set.technology.displayName.contains(listingSearchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BgCanvas,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassIconButton(
                        onClick = onNavigateBack,
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tone = GlassIconTone.FROSTED_WHITE,
                        size = 40.dp,
                        testTag = "admin_btn_back"
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "🛡️ Admin Control Panel",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Super Admin • mdmaheraz65@gmail.com",
                            fontSize = 11.5.sp,
                            color = PrimaryPurple,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = PrimaryPurple,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Traffic & Analytics", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Users (${allUsers.size})", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Listings (${bookSets.size})", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp) }
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // ========================================================
                    // 1. LIVE APP TRAFFIC & PERFORMANCE ANALYTICS
                    // ========================================================
                    item {
                        Text(
                            text = "📊 Today's Campus Traffic & Telemetry",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AdminMetricCard(
                                title = "Daily Active Students",
                                value = "${trafficStats.dailyActiveUsers}",
                                subtitle = "Online across FPI & FCI",
                                icon = Icons.Outlined.People,
                                color = PrimaryPurple,
                                modifier = Modifier.weight(1f)
                            )
                            AdminMetricCard(
                                title = "App Sessions / Visits",
                                value = "${trafficStats.totalVisitsToday}",
                                subtitle = "Total app launches today",
                                icon = Icons.Outlined.TrendingUp,
                                color = AccentBlue,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AdminMetricCard(
                                title = "Book Searches / Views",
                                value = "${trafficStats.bookChecksToday}",
                                subtitle = "Checked books & guides",
                                icon = Icons.Outlined.Search,
                                color = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                            AdminMetricCard(
                                title = "Active Chat Inquiries",
                                value = "${trafficStats.chatInquiriesToday}",
                                subtitle = "Buyer/Seller messages",
                                icon = Icons.Outlined.Forum,
                                color = Color(0xFFF59E0B),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Campus Traffic Share Card
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "🏫 Campus Traffic Distribution",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Feni Polytechnic Institute (FPI)", fontSize = 12.5.sp, color = TextSecondary)
                                Text("${trafficStats.fpiTrafficShare}%", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { trafficStats.fpiTrafficShare / 100f },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = PrimaryPurple,
                                trackColor = Color(0xFFE2E8F0)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Feni Computer Institute (FCI)", fontSize = 12.5.sp, color = TextSecondary)
                                Text("${trafficStats.fciTrafficShare}%", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { trafficStats.fciTrafficShare / 100f },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = AccentBlue,
                                trackColor = Color(0xFFE2E8F0)
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.AccessTime, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Peak Campus Activity: ${trafficStats.peakHour} (Class Break & Tiffin)",
                                        fontSize = 11.5.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // System Health Card
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = CircleShape, color = Color(0xFF10B981), modifier = Modifier.size(10.dp)) {}
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Firebase & Local Room Services Online", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                }
                                Text("100% HEALTHY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            }
                        }
                    }

                    // Live Telemetry Controls
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚡ Live Telemetry Controls",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = TextPrimary
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF1F5F9)
                                ) {
                                    Text(
                                        text = "Interaction-Driven",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PrimaryPurple,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Telemetry counters strictly increment with actual student interactions (real visits, book searches & views, chat messages, and student signups).",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            FrostedGlassButton(
                                text = "Reset All Counters to Zero",
                                onClick = { MarketplaceRepository.resetTrafficStats() },
                                textColor = Color(0xFFEF4444),
                                icon = Icons.Outlined.RestartAlt,
                                modifier = Modifier.fillMaxWidth(),
                                height = 40.dp,
                                testTag = "btn_reset_telemetry_counters"
                            )
                        }
                    }
                }

                1 -> {
                    // ========================================================
                    // 2. USER MANAGEMENT DIRECTORY & ACTIONS
                    // ========================================================
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Campus Student Directory",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            GlassButton(
                                text = "+ Add Student",
                                onClick = { showAddUserDialog = true },
                                height = 34.dp
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = userSearchQuery,
                            onValueChange = { userSearchQuery = it },
                            placeholder = { Text("Search by name, phone, email, college...") },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = PrimaryPurple) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("ALL", "ACTIVE", "SUSPENDED", "BLOCKED").forEach { status ->
                                val isSelected = userFilterStatus == status
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { userFilterStatus = status },
                                    label = { Text(status, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }
                    }

                    if (filteredUsers.isEmpty()) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text("No students found matching your search.", fontSize = 13.sp, color = TextMuted)
                            }
                        }
                    }

                    items(filteredUsers, key = { it.id }) { user ->
                        StudentAdminCard(
                            user = user,
                            onCallUser = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${user.phone}")
                                }
                                context.startActivity(intent)
                            },
                            onSuspend = { MarketplaceRepository.adminSuspendUser(user.id) },
                            onBlock = { MarketplaceRepository.adminBlockUser(user.id) },
                            onRestore = { MarketplaceRepository.adminRestoreUser(user.id) },
                            onDelete = { MarketplaceRepository.adminDeleteUser(user.id) }
                        )
                    }
                }

                2 -> {
                    // ========================================================
                    // 3. BOOK SET LISTINGS MODERATION
                    // ========================================================
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Marketplace Book Listings",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            GlassButton(
                                text = "+ Add Listing",
                                onClick = { showAddListingDialog = true },
                                height = 34.dp
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = listingSearchQuery,
                            onValueChange = { listingSearchQuery = it },
                            placeholder = { Text("Search by book title, seller, phone...") },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = PrimaryPurple) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (filteredListings.isEmpty()) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text("No book listings found matching your search.", fontSize = 13.sp, color = TextMuted)
                            }
                        }
                    }

                    items(filteredListings, key = { it.id }) { set ->
                        ListingAdminCard(
                            set = set,
                            onCallSeller = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${set.sellerPhone}")
                                }
                                context.startActivity(intent)
                            },
                            onRemove = { MarketplaceRepository.adminRemoveSet(set.id) },
                            onRestore = { MarketplaceRepository.adminRestoreSet(set.id) },
                            onDelete = { MarketplaceRepository.adminDeleteSet(set.id) }
                        )
                    }
                }
            }
        }

        if (showAddUserDialog) {
            AddStudentDialog(
                onDismiss = { showAddUserDialog = false },
                onAddStudent = { name, email, phone, college, tech, sem ->
                    MarketplaceRepository.adminCreateUser(
                        name = name,
                        email = email,
                        phone = phone,
                        college = college,
                        technology = tech,
                        semester = sem
                    )
                    showAddUserDialog = false
                }
            )
        }

        if (showAddListingDialog) {
            AddListingDialog(
                onDismiss = { showAddListingDialog = false },
                onAddListing = { title, subject, tech, sem, college, price, sellerName, sellerPhone ->
                    MarketplaceRepository.publishSet(
                        title = title,
                        subject = subject,
                        technology = tech,
                        semester = sem,
                        college = college,
                        totalPrice = price,
                        overallCondition = SetCondition.GOOD,
                        description = "Verified campus semester book set",
                        includedBooks = listOf(
                            IncludedBook("bk_1", "", "$title (Book 1)", subject, "Standard Author", "Latest", "SUB-101")
                        )
                    )
                    showAddListingDialog = false
                }
            )
        }
    }
}

/**
 * Metric Card for App Traffic & Telemetry.
 */
@Composable
private fun AdminMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(18.dp), ambientColor = SoftShadowColor),
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = color.copy(alpha = 0.12f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                    }
                }
                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(subtitle, fontSize = 11.sp, color = TextMuted)
        }
    }
}

/**
 * Student User Card with complete details and moderation actions.
 */
@Composable
private fun StudentAdminCard(
    user: UserProfile,
    onCallUser: () -> Unit,
    onSuspend: () -> Unit,
    onBlock: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = PrimaryPurpleLight,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = user.name.take(1).uppercase(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(user.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        if (user.role == "ADMIN") {
                            Surface(shape = RoundedCornerShape(6.dp), color = PrimaryPurpleLight) {
                                Text("ADMIN", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text(user.email, fontSize = 12.sp, color = TextMuted)
                }

                // Account Status Pill
                val statusColor = when (user.accountStatus) {
                    "ACTIVE" -> Color(0xFF10B981)
                    "SUSPENDED" -> Color(0xFFF59E0B)
                    else -> Color(0xFFEF4444)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = user.accountStatus,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 10.dp))

            // Student Academic & Contact Details
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.School, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = user.college, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Engineering, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "${user.technology.displayName} • ${user.currentSemester.displayName}", fontSize = 12.sp, color = TextSecondary)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Phone, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = user.phone, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }

                    // Direct Call Button
                    FrostedGlassButton(
                        text = "Call Student",
                        onClick = onCallUser,
                        textColor = PrimaryPurple,
                        height = 32.dp
                    )
                }
            }

            // Moderation Actions for Non-Admin Users
            if (user.role != "ADMIN") {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (user.accountStatus == "ACTIVE") {
                        FrostedGlassButton(
                            text = "Suspend",
                            onClick = onSuspend,
                            textColor = Color(0xFFF59E0B),
                            height = 32.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        FrostedGlassButton(
                            text = "Block",
                            onClick = onBlock,
                            textColor = Color(0xFFEF4444),
                            height = 32.dp
                        )
                    } else {
                        GlassButton(
                            text = "Reactivate",
                            onClick = onRestore,
                            height = 32.dp,
                            gradient = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    FrostedGlassButton(
                        text = "Delete",
                        onClick = onDelete,
                        textColor = Color(0xFFEF4444),
                        height = 32.dp
                    )
                }
            }
        }
    }
}

/**
 * Book Listing Card for moderation with actions.
 */
@Composable
private fun ListingAdminCard(
    set: com.example.model.BookSet,
    onCallSeller: () -> Unit,
    onRemove: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(set.title, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = TextPrimary)
                    Text("${set.semester.displayName} • ${set.technology.shortName} • ${set.college}", fontSize = 11.5.sp, color = TextMuted)
                }

                Text(
                    text = "৳ ${set.totalPrice}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Seller: ${set.sellerName} (${set.sellerPhone})",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                val statusColor = when (set.status) {
                    SetStatus.AVAILABLE -> Color(0xFF10B981)
                    SetStatus.SOLD -> Color(0xFF3B82F6)
                    SetStatus.REMOVED -> Color(0xFFEF4444)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = set.status.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FrostedGlassButton(
                    text = "Call",
                    onClick = onCallSeller,
                    textColor = TextSecondary,
                    height = 30.dp
                )
                Spacer(modifier = Modifier.width(6.dp))

                if (set.status == SetStatus.AVAILABLE) {
                    FrostedGlassButton(
                        text = "Suspend",
                        onClick = onRemove,
                        textColor = Color(0xFFEF4444),
                        height = 30.dp
                    )
                } else {
                    GlassButton(
                        text = "Restore",
                        onClick = onRestore,
                        height = 30.dp,
                        gradient = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))

                FrostedGlassButton(
                    text = "Delete",
                    onClick = onDelete,
                    textColor = Color(0xFFEF4444),
                    height = 30.dp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStudentDialog(
    onDismiss: () -> Unit,
    onAddStudent: (name: String, email: String, phone: String, college: String, tech: Technology, sem: Semester) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var college by remember { mutableStateOf(CollegeDirectory.FENI_POLYTECHNIC) }
    var tech by remember { mutableStateOf(Technology.CIVIL) }
    var sem by remember { mutableStateOf(Semester.SEM_1) }
    var collegeDropdown by remember { mutableStateOf(false) }
    var techDropdown by remember { mutableStateOf(false) }
    var semDropdown by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val availableTechs = remember(college) {
        CollegeDirectory.getTechnologiesForCollege(college)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "➕ Register Campus Student",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (error != null) {
                    Text(text = error ?: "", color = Color(0xFFEF4444), fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Student Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; error = null },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; error = null },
                    label = { Text("Email Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // College Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = college,
                        onValueChange = {},
                        enabled = false,
                        readOnly = true,
                        label = { Text("Polytechnic College") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = TextPrimary,
                            disabledBorderColor = Color(0xFFCBD5E1),
                            disabledLabelColor = TextSecondary
                        )
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { collegeDropdown = true }
                    )
                    DropdownMenu(
                        expanded = collegeDropdown,
                        onDismissRequest = { collegeDropdown = false }
                    ) {
                        CollegeDirectory.ALL_COLLEGES.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c, fontSize = 13.sp) },
                                onClick = {
                                    college = c
                                    collegeDropdown = false
                                }
                            )
                        }
                    }
                }

                // Technology Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = tech.displayName,
                        onValueChange = {},
                        enabled = false,
                        readOnly = true,
                        label = { Text("Technology / Department") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = TextPrimary,
                            disabledBorderColor = Color(0xFFCBD5E1),
                            disabledLabelColor = TextSecondary
                        )
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { techDropdown = true }
                    )
                    DropdownMenu(
                        expanded = techDropdown,
                        onDismissRequest = { techDropdown = false }
                    ) {
                        availableTechs.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.displayName, fontSize = 13.sp) },
                                onClick = {
                                    tech = t
                                    techDropdown = false
                                }
                            )
                        }
                    }
                }

                // Semester Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = sem.displayName,
                        onValueChange = {},
                        enabled = false,
                        readOnly = true,
                        label = { Text("Current Semester") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = TextPrimary,
                            disabledBorderColor = Color(0xFFCBD5E1),
                            disabledLabelColor = TextSecondary
                        )
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { semDropdown = true }
                    )
                    DropdownMenu(
                        expanded = semDropdown,
                        onDismissRequest = { semDropdown = false }
                    ) {
                        Semester.entries.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.displayName, fontSize = 13.sp) },
                                onClick = {
                                    sem = s
                                    semDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || phone.isBlank() || email.isBlank()) {
                        error = "Please fill in all student details."
                        return@Button
                    }
                    onAddStudent(name.trim(), email.trim(), phone.trim(), college, tech, sem)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Text("Register Student", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddListingDialog(
    onDismiss: () -> Unit,
    onAddListing: (title: String, subject: String, tech: Technology, sem: Semester, college: String, price: Int, sellerName: String, sellerPhone: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("500") }
    var sellerName by remember { mutableStateOf("Campus Seller") }
    var sellerPhone by remember { mutableStateOf("01712-345678") }
    var college by remember { mutableStateOf(CollegeDirectory.FENI_POLYTECHNIC) }
    var tech by remember { mutableStateOf(Technology.CIVIL) }
    var sem by remember { mutableStateOf(Semester.SEM_1) }
    var collegeDropdown by remember { mutableStateOf(false) }
    var techDropdown by remember { mutableStateOf(false) }
    var semDropdown by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val availableTechs = remember(college) {
        CollegeDirectory.getTechnologiesForCollege(college)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "📚 Post Verified Book Set",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (error != null) {
                    Text(text = error ?: "", color = Color(0xFFEF4444), fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = null },
                    label = { Text("Book Set Title") },
                    placeholder = { Text("e.g. Civil 4th Sem Complete Set") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it; error = null },
                    label = { Text("Subject / Department Course") },
                    placeholder = { Text("e.g. Structural Engineering") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it; error = null },
                    label = { Text("Price (৳ BDT)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sellerName,
                    onValueChange = { sellerName = it },
                    label = { Text("Seller Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sellerPhone,
                    onValueChange = { sellerPhone = it },
                    label = { Text("Seller Phone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // College Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = college,
                        onValueChange = {},
                        enabled = false,
                        readOnly = true,
                        label = { Text("College") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = TextPrimary,
                            disabledBorderColor = Color(0xFFCBD5E1),
                            disabledLabelColor = TextSecondary
                        )
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { collegeDropdown = true }
                    )
                    DropdownMenu(
                        expanded = collegeDropdown,
                        onDismissRequest = { collegeDropdown = false }
                    ) {
                        CollegeDirectory.ALL_COLLEGES.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c, fontSize = 13.sp) },
                                onClick = {
                                    college = c
                                    collegeDropdown = false
                                }
                            )
                        }
                    }
                }

                // Tech Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = tech.displayName,
                        onValueChange = {},
                        enabled = false,
                        readOnly = true,
                        label = { Text("Technology") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = TextPrimary,
                            disabledBorderColor = Color(0xFFCBD5E1),
                            disabledLabelColor = TextSecondary
                        )
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { techDropdown = true }
                    )
                    DropdownMenu(
                        expanded = techDropdown,
                        onDismissRequest = { techDropdown = false }
                    ) {
                        availableTechs.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.displayName, fontSize = 13.sp) },
                                onClick = {
                                    tech = t
                                    techDropdown = false
                                }
                            )
                        }
                    }
                }

                // Semester Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = sem.displayName,
                        onValueChange = {},
                        enabled = false,
                        readOnly = true,
                        label = { Text("Semester") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = TextPrimary,
                            disabledBorderColor = Color(0xFFCBD5E1),
                            disabledLabelColor = TextSecondary
                        )
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { semDropdown = true }
                    )
                    DropdownMenu(
                        expanded = semDropdown,
                        onDismissRequest = { semDropdown = false }
                    ) {
                        Semester.entries.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.displayName, fontSize = 13.sp) },
                                onClick = {
                                    sem = s
                                    semDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceStr.trim().toIntOrNull() ?: 500
                    if (title.isBlank()) {
                        error = "Please enter book set title."
                        return@Button
                    }
                    onAddListing(title.trim(), subject.ifBlank { "General Semester Books" }.trim(), tech, sem, college, price, sellerName.trim(), sellerPhone.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Text("Publish Listing", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
