package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.data.MarketplaceRepository
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Super Admin & Developer Management Console.
 *
 * Full-spectrum responsive campus control center:
 * 1. Dashboard: Metrics grid & live campus traffic analytics.
 * 2. Users: Student directory with detailed User Profile Portfolio view.
 * 3. Books: Marketplace listing moderation (Suspend, Restore, Delete).
 * 4. Reports: Campus dispute and violation resolution desk.
 * 5. Colleges: Breakdown for FPI, FCI, and regional polytechnics.
 * 6. App Controls: Real-time Maintenance Mode and Force Update toggles stored in /app_config/global.
 * 7. Firebase Usage: Real-time telemetry for Firestore, Storage, and Real-time Chat.
 * 8. Settings: Diagnostics, cache management, and platform details.
 * 9. Logout: Admin sign-out and return.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val allUsers by MarketplaceRepository.allUsers.collectAsState()
    val bookSets by MarketplaceRepository.bookSets.collectAsState()
    val trafficStats by MarketplaceRepository.trafficStats.collectAsState()
    val appConfig by MarketplaceRepository.appConfig.collectAsState()
    val reports by MarketplaceRepository.reports.collectAsState()
    val firebaseUsage by MarketplaceRepository.firebaseUsageMetrics.collectAsState()

    var selectedSection by remember { mutableStateOf(0) }
    var selectedUserForPortfolio by remember { mutableStateOf<UserProfile?>(null) }
    var previewPhotoUrl by remember { mutableStateOf<String?>(null) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var showAddListingDialog by remember { mutableStateOf(false) }

    // User section search and filter
    var userSearchQuery by remember { mutableStateOf("") }
    var userFilterStatus by remember { mutableStateOf("ALL") }

    // Listing section search
    var listingSearchQuery by remember { mutableStateOf("") }

    val navItems = listOf(
        AdminNavItem(0, "Dashboard", Icons.Outlined.Dashboard, null),
        AdminNavItem(1, "Users", Icons.Outlined.People, "${allUsers.size}"),
        AdminNavItem(2, "Books", Icons.Outlined.MenuBook, "${bookSets.size}"),
        AdminNavItem(3, "Reports", Icons.Outlined.Report, if (reports.count { it.status == "PENDING" } > 0) "${reports.count { it.status == "PENDING" }}" else null),
        AdminNavItem(4, "Colleges", Icons.Outlined.School, null),
        AdminNavItem(5, "App Controls", Icons.Outlined.Tune, if (appConfig.isMaintenanceMode || appConfig.isForceUpdate) "!" else null),
        AdminNavItem(6, "Firebase Usage", Icons.Outlined.Cloud, null),
        AdminNavItem(7, "Settings", Icons.Outlined.Settings, null),
        AdminNavItem(8, "Logout", Icons.AutoMirrored.Outlined.ExitToApp, null)
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(BgCanvas)) {
        val isWideScreen = maxWidth >= 760.dp

        if (isWideScreen) {
            // Desktop / Tablet Landscape: Permanent Left Sidebar + Right Content Pane
            Row(modifier = Modifier.fillMaxSize()) {
                AdminSidebarContent(
                    items = navItems,
                    selectedSection = selectedSection,
                    onSectionSelected = { section ->
                        if (section == 8) {
                            MarketplaceRepository.logout()
                            onNavigateBack()
                        } else {
                            selectedSection = section
                        }
                    },
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                        .background(Color.White)
                )

                VerticalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    AdminMainContentPane(
                        selectedSection = selectedSection,
                        allUsers = allUsers,
                        bookSets = bookSets,
                        trafficStats = trafficStats,
                        appConfig = appConfig,
                        reports = reports,
                        firebaseUsage = firebaseUsage,
                        userSearchQuery = userSearchQuery,
                        onUserSearchChange = { userSearchQuery = it },
                        userFilterStatus = userFilterStatus,
                        onUserFilterStatusChange = { userFilterStatus = it },
                        listingSearchQuery = listingSearchQuery,
                        onListingSearchChange = { listingSearchQuery = it },
                        onUserClick = { user -> selectedUserForPortfolio = user },
                        onAddUserClick = { showAddUserDialog = true },
                        onAddListingClick = { showAddListingDialog = true },
                        onNavigateBack = onNavigateBack,
                        isWideScreen = true,
                        onOpenDrawer = {}
                    )
                }
            }
        } else {
            // Handheld / Compact Screen: Modal Navigation Drawer + Responsive Top Bar
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier.width(280.dp),
                        drawerContainerColor = Color.White
                    ) {
                        AdminSidebarContent(
                            items = navItems,
                            selectedSection = selectedSection,
                            onSectionSelected = { section ->
                                coroutineScope.launch { drawerState.close() }
                                if (section == 8) {
                                    MarketplaceRepository.logout()
                                    onNavigateBack()
                                } else {
                                    selectedSection = section
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            ) {
                AdminMainContentPane(
                    selectedSection = selectedSection,
                    allUsers = allUsers,
                    bookSets = bookSets,
                    trafficStats = trafficStats,
                    appConfig = appConfig,
                    reports = reports,
                    firebaseUsage = firebaseUsage,
                    userSearchQuery = userSearchQuery,
                    onUserSearchChange = { userSearchQuery = it },
                    userFilterStatus = userFilterStatus,
                    onUserFilterStatusChange = { userFilterStatus = it },
                    listingSearchQuery = listingSearchQuery,
                    onListingSearchChange = { listingSearchQuery = it },
                    onUserClick = { user -> selectedUserForPortfolio = user },
                    onAddUserClick = { showAddUserDialog = true },
                    onAddListingClick = { showAddListingDialog = true },
                    onNavigateBack = onNavigateBack,
                    isWideScreen = false,
                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                )
            }
        }
    }

    // Modal: User Profile Portfolio View
    selectedUserForPortfolio?.let { user ->
        AdminUserProfilePortfolioDialog(
            user = user,
            allBookSets = bookSets,
            onDismiss = { selectedUserForPortfolio = null },
            onPhotoClick = { photoUrl -> previewPhotoUrl = photoUrl },
            onUserStatusUpdated = {
                // Refresh local selected state
                selectedUserForPortfolio = allUsers.find { it.id == user.id }
            }
        )
    }

    // Modal: Full-screen Photo Zoom & Download
    previewPhotoUrl?.let { photoUrl ->
        AdminPhotoPreviewDialog(
            photoUrl = photoUrl,
            onDismiss = { previewPhotoUrl = null }
        )
    }

    // Dialog: Add Student
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
                Toast.makeText(context, "Student profile registered successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Dialog: Add Verified Book Listing
    if (showAddListingDialog) {
        AddListingDialog(
            onDismiss = { showAddListingDialog = false },
            onAddListing = { title, subject, tech, sem, college, price, sellerName, sellerPhone ->
                coroutineScope.launch {
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
                    Toast.makeText(context, "Verified listing published", Toast.LENGTH_SHORT).show()
                }
                showAddListingDialog = false
            }
        )
    }
}

private data class AdminNavItem(
    val id: Int,
    val title: String,
    val icon: ImageVector,
    val badge: String?
)

/**
 * Sidebar navigation list adapting to drawer or permanent pane.
 */
@Composable
private fun AdminSidebarContent(
    items: List<AdminNavItem>,
    selectedSection: Int,
    onSectionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Console Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PrimaryPurpleLight,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "BookSet Admin",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "mdmaheraz65@gmail.com",
                    fontSize = 11.sp,
                    color = PrimaryPurple,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 10.dp))

        Text(
            text = "MANAGEMENT & TELEMETRY",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )

        // Menu items
        items.forEach { item ->
            val isSelected = selectedSection == item.id
            val isLogout = item.id == 8

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) PrimaryPurple.copy(alpha = 0.12f) else Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .clickable { onSectionSelected(item.id) }
                    .testTag("admin_nav_item_${item.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = when {
                            isLogout -> Color(0xFFEF4444)
                            isSelected -> PrimaryPurple
                            else -> TextSecondary
                        },
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = item.title,
                        fontSize = 13.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            isLogout -> Color(0xFFEF4444)
                            isSelected -> PrimaryPurple
                            else -> TextPrimary
                        },
                        modifier = Modifier.weight(1f)
                    )

                    item.badge?.let { badgeText ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (badgeText == "!") Color(0xFFEF4444) else PrimaryPurpleLight
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (badgeText == "!") Color.White else PrimaryPurple,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))
        Spacer(modifier = Modifier.height(20.dp))

        // Live Backend Ping Pill
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Firestore Live Sync Active",
                    fontSize = 10.5.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Main content pane switching dynamically based on sidebar navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminMainContentPane(
    selectedSection: Int,
    allUsers: List<UserProfile>,
    bookSets: List<BookSet>,
    trafficStats: AppTrafficStats,
    appConfig: AppConfig,
    reports: List<ReportItem>,
    firebaseUsage: FirebaseUsageMetrics,
    userSearchQuery: String,
    onUserSearchChange: (String) -> Unit,
    userFilterStatus: String,
    onUserFilterStatusChange: (String) -> Unit,
    listingSearchQuery: String,
    onListingSearchChange: (String) -> Unit,
    onUserClick: (UserProfile) -> Unit,
    onAddUserClick: () -> Unit,
    onAddListingClick: () -> Unit,
    onNavigateBack: () -> Unit,
    isWideScreen: Boolean,
    onOpenDrawer: () -> Unit
) {
    val context = LocalContext.current

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
        containerColor = BgCanvas,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isWideScreen) {
                        GlassIconButton(
                            onClick = onOpenDrawer,
                            icon = Icons.Outlined.Menu,
                            contentDescription = "Open Navigation Menu",
                            tone = GlassIconTone.FROSTED_WHITE,
                            size = 38.dp,
                            testTag = "admin_btn_drawer"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    GlassIconButton(
                        onClick = onNavigateBack,
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back to App",
                        tone = GlassIconTone.FROSTED_WHITE,
                        size = 38.dp,
                        testTag = "admin_btn_back"
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (selectedSection) {
                                0 -> "📊 Admin Dashboard"
                                1 -> "👥 Student Directory & Portfolios"
                                2 -> "📚 Marketplace Book Listings"
                                3 -> "🚨 Dispute & Campus Reports"
                                4 -> "🏫 Polytechnic Campus Breakdown"
                                5 -> "⚙️ App Controls (Maintenance & Updates)"
                                6 -> "🔥 Firebase Real-Time Telemetry"
                                7 -> "🛠️ Console Settings & Diagnostics"
                                else -> "🛡️ Admin Panel"
                            },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Feni Polytechnic & Feni Computer Institute Central Administration",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }

                    // Maintenance / Update Warning Pill
                    if (appConfig.isMaintenanceMode) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Maintenance ON", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                            }
                        }
                    }
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
            when (selectedSection) {
                0 -> {
                    // ========================================================
                    // SECTION 0: DASHBOARD & METRICS GRID
                    // ========================================================
                    item {
                        Text(
                            text = "Campus Operations Summary",
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
                                title = "Total Students",
                                value = "${allUsers.size}",
                                subtitle = "Registered campus accounts",
                                icon = Icons.Outlined.People,
                                color = PrimaryPurple,
                                modifier = Modifier.weight(1f)
                            )
                            AdminMetricCard(
                                title = "Active Listings",
                                value = "${bookSets.count { it.status == SetStatus.AVAILABLE }}",
                                subtitle = "Available semester sets",
                                icon = Icons.Outlined.MenuBook,
                                color = Color(0xFF10B981),
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
                                title = "Dispute Reports",
                                value = "${reports.count { it.status == "PENDING" }}",
                                subtitle = "Pending admin review",
                                icon = Icons.Outlined.Report,
                                color = if (reports.count { it.status == "PENDING" } > 0) Color(0xFFEF4444) else Color(0xFF64748B),
                                modifier = Modifier.weight(1f)
                            )
                            AdminMetricCard(
                                title = "App System Status",
                                value = if (appConfig.isMaintenanceMode) "Maintenance" else "Operational",
                                subtitle = if (appConfig.isMaintenanceMode) "Bypassing for Admin" else "All services online",
                                icon = Icons.Outlined.CheckCircle,
                                color = if (appConfig.isMaintenanceMode) Color(0xFFF59E0B) else Color(0xFF3B82F6),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "📊 Today's Real-Time Traffic & Campus Telemetry",
                            fontSize = 15.sp,
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
                                title = "Book Set Visits",
                                value = "${trafficStats.totalVisitsToday}",
                                subtitle = "Details pages loaded",
                                icon = Icons.Outlined.Visibility,
                                color = Color(0xFF3B82F6),
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
                                title = "Subject Searches",
                                value = "${trafficStats.bookChecksToday}",
                                subtitle = "Department course lookups",
                                icon = Icons.Outlined.Search,
                                color = Color(0xFFF59E0B),
                                modifier = Modifier.weight(1f)
                            )
                            AdminMetricCard(
                                title = "Peak Activity Hours",
                                value = trafficStats.peakHour,
                                subtitle = "Hostel & Break time",
                                icon = Icons.Outlined.Schedule,
                                color = Color(0xFF8B5CF6),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        CampusTrafficChart(trafficStats = trafficStats)
                    }
                }

                1 -> {
                    // ========================================================
                    // SECTION 1: USER MANAGEMENT & PORTFOLIO VIEW
                    // ========================================================
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Campus Students Directory",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Tap on any student to view full portfolio & shared photos",
                                    fontSize = 11.5.sp,
                                    color = TextMuted
                                )
                            }
                            GlassButton(
                                text = "+ Register Student",
                                onClick = onAddUserClick,
                                height = 34.dp
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = userSearchQuery,
                            onValueChange = onUserSearchChange,
                            placeholder = { Text("Search by student name, phone, email, technology...") },
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
                                    onClick = { onUserFilterStatusChange(status) },
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
                            onCardClick = { onUserClick(user) },
                            onCallUser = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${user.phone}")
                                }
                                context.startActivity(intent)
                            },
                            onSuspend = {
                                MarketplaceRepository.adminSuspendUser(user.id)
                                Toast.makeText(context, "User suspended & kicked immediately", Toast.LENGTH_SHORT).show()
                            },
                            onBlock = {
                                MarketplaceRepository.adminBlockUser(user.id)
                                Toast.makeText(context, "User blocked & access revoked immediately", Toast.LENGTH_SHORT).show()
                            },
                            onRestore = {
                                MarketplaceRepository.adminRestoreUser(user.id)
                                Toast.makeText(context, "User account restored to ACTIVE", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = {
                                MarketplaceRepository.adminDeleteUser(user.id)
                                Toast.makeText(context, "User permanently removed", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                2 -> {
                    // ========================================================
                    // SECTION 2: BOOK LISTINGS MODERATION
                    // ========================================================
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Marketplace Book Listings",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            GlassButton(
                                text = "+ Add Listing",
                                onClick = onAddListingClick,
                                height = 34.dp
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = listingSearchQuery,
                            onValueChange = onListingSearchChange,
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
                            onDelete = { MarketplaceRepository.deleteSet(set.id, requestingUserId = "admin_master_1", requestingUserRole = "ADMIN") }
                        )
                    }
                }

                3 -> {
                    // ========================================================
                    // SECTION 3: REPORTS & DISPUTE DESK
                    // ========================================================
                    item {
                        Text(
                            text = "Campus Reports & Dispute Desk",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Manage student violation reports, misleading listings, or spam.",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }

                    if (reports.isEmpty()) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Zero Active Disputes", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                    Text("All student reports have been addressed and cleared.", fontSize = 12.sp, color = TextMuted)
                                }
                            }
                        }
                    }

                    items(reports, key = { it.id }) { report ->
                        ReportAdminCard(
                            report = report,
                            onResolve = {
                                MarketplaceRepository.resolveReport(report.id)
                                Toast.makeText(context, "Report marked as RESOLVED", Toast.LENGTH_SHORT).show()
                            },
                            onDismiss = {
                                MarketplaceRepository.dismissReport(report.id)
                                Toast.makeText(context, "Report dismissed", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                4 -> {
                    // ========================================================
                    // SECTION 4: COLLEGES BREAKDOWN
                    // ========================================================
                    item {
                        Text(
                            text = "Polytechnic Campus Analytics",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Active student participation and listings breakdown per institution.",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }

                    CollegeDirectory.ALL_COLLEGES.forEach { collegeName ->
                        val collegeUsers = allUsers.filter { it.college == collegeName }
                        val collegeListings = bookSets.filter { it.college == collegeName }

                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = collegeName,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = if (collegeName == CollegeDirectory.FENI_POLYTECHNIC) "Primary Technical Institute" else "Computer Technology Campus",
                                                fontSize = 11.5.sp,
                                                color = TextMuted
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = PrimaryPurpleLight
                                        ) {
                                            Text(
                                                text = "${collegeUsers.size} Students",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryPurple,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Available Book Sets:", fontSize = 12.5.sp, color = TextSecondary)
                                        Text("${collegeListings.size} Listings", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Departments Covered:", fontSize = 12.5.sp, color = TextSecondary)
                                        Text(
                                            "${CollegeDirectory.getTechnologiesForCollege(collegeName).size} Technologies",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                5 -> {
                    // ========================================================
                    // SECTION 5: APP CONTROLS (MAINTENANCE & FORCE UPDATE)
                    // ========================================================
                    item {
                        AdminAppControlsSection(
                            currentConfig = appConfig,
                            onSaveConfig = { updatedConfig ->
                                MarketplaceRepository.updateAppConfig(updatedConfig)
                                Toast.makeText(context, "App Controls deployed to Firestore /app_config/global", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }

                6 -> {
                    // ========================================================
                    // SECTION 6: FIREBASE MONITORING & QUOTA USAGE
                    // ========================================================
                    item {
                        AdminFirebaseUsageSection(
                            metrics = firebaseUsage
                        )
                    }
                }

                7 -> {
                    // ========================================================
                    // SECTION 7: SETTINGS & DIAGNOSTICS
                    // ========================================================
                    item {
                        AdminSettingsSection()
                    }
                }
            }
        }
    }
}

/**
 * App Controls: Maintenance Mode & Force Update.
 * Connects directly to Firestore `/app_config/global`.
 */
@Composable
private fun AdminAppControlsSection(
    currentConfig: AppConfig,
    onSaveConfig: (AppConfig) -> Unit
) {
    var isMaintenance by remember(currentConfig) { mutableStateOf(currentConfig.isMaintenanceMode) }
    var maintenanceMsg by remember(currentConfig) { mutableStateOf(currentConfig.maintenanceMessage) }

    var isForceUpdate by remember(currentConfig) { mutableStateOf(currentConfig.isForceUpdate) }
    var latestVersion by remember(currentConfig) { mutableStateOf(currentConfig.latestAppVersion) }
    var apkUrl by remember(currentConfig) { mutableStateOf(currentConfig.apkDownloadUrl) }
    var updateNotes by remember(currentConfig) { mutableStateOf(currentConfig.updateNotes) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "App Management System",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Control real-time maintenance and mandatory version upgrades across all active student devices.",
            fontSize = 12.sp,
            color = TextMuted
        )

        // ================= FEATURE 1: MAINTENANCE MODE =================
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Maintenance Mode", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isMaintenance) Color(0xFFEF4444).copy(alpha = 0.12f) else Color(0xFF10B981).copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = if (isMaintenance) "ACTIVE" else "OFF",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMaintenance) Color(0xFFEF4444) else Color(0xFF10B981),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "Blocks all regular student navigation with a non-dismissible popup. Admin accounts automatically bypass this screen.",
                            fontSize = 11.5.sp,
                            color = TextMuted
                        )
                    }

                    Switch(
                        checked = isMaintenance,
                        onCheckedChange = { isMaintenance = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFEF4444))
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = maintenanceMsg,
                    onValueChange = { maintenanceMsg = it },
                    label = { Text("Maintenance Message to Students") },
                    placeholder = { Text("The app is currently undergoing scheduled maintenance. Please check back later.") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )
            }
        }

        // ================= FEATURE 2: FORCE UPDATE =================
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Force App Update (Upgraded Mode)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isForceUpdate) Color(0xFF2563EB).copy(alpha = 0.12f) else Color(0xFF64748B).copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = if (isForceUpdate) "UPGRADED (ADMIN ONLY)" else "OFF",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isForceUpdate) Color(0xFF2563EB) else Color(0xFF64748B),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "When enabled, ONLY Admin (mdmaheraz65@gmail.com) can access the upgraded app mode. All other users are blocked by the mandatory update screen.",
                            fontSize = 11.5.sp,
                            color = TextMuted
                        )
                    }

                    Switch(
                        checked = isForceUpdate,
                        onCheckedChange = { isForceUpdate = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2563EB))
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = latestVersion,
                    onValueChange = { latestVersion = it },
                    label = { Text("Latest App Version Code") },
                    placeholder = { Text("e.g. 2.1.0") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = apkUrl,
                    onValueChange = { apkUrl = it },
                    label = { Text("Direct APK Download URL / Release Link") },
                    placeholder = { Text("https://github.com/aistudio/bookset/releases") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = updateNotes,
                    onValueChange = { updateNotes = it },
                    label = { Text("Release Notes / Update Highlights") },
                    placeholder = { Text("New real-time book photos sync and performance updates.") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )
            }
        }

        // Save Button
        Button(
            onClick = {
                onSaveConfig(
                    AppConfig(
                        isMaintenanceMode = isMaintenance,
                        maintenanceMessage = maintenanceMsg.trim(),
                        isForceUpdate = isForceUpdate,
                        latestAppVersion = latestVersion.trim(),
                        apkDownloadUrl = apkUrl.trim(),
                        updateNotes = updateNotes.trim()
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
        ) {
            Icon(Icons.Outlined.Save, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save & Deploy App Controls", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
        }
    }
}

/**
 * Dedicated Firebase Section displaying live quota and resource monitoring.
 */
@Composable
private fun AdminFirebaseUsageSection(
    metrics: FirebaseUsageMetrics
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "Firebase Quota & Storage Monitoring",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Real-time resource tracking ensuring operations stay well within limits.",
            fontSize = 12.sp,
            color = TextMuted
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AdminMetricCard(
                title = "Total Documents",
                value = "${metrics.totalDocumentCount}",
                subtitle = "Users, listings, messages",
                icon = Icons.Outlined.Dns,
                color = PrimaryPurple,
                modifier = Modifier.weight(1f)
            )
            AdminMetricCard(
                title = "Database Storage",
                value = "${String.format("%.2f", metrics.estimatedStorageMb)} MB",
                subtitle = "Firestore JSON documents",
                icon = Icons.Outlined.Storage,
                color = Color(0xFF3B82F6),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AdminMetricCard(
                title = "Uploaded Photos",
                value = "${metrics.totalUploadedPhotos}",
                subtitle = "Book covers & extra photos",
                icon = Icons.Outlined.Image,
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
            AdminMetricCard(
                title = "Chat Messages",
                value = "${metrics.totalChatMessages}",
                subtitle = "Real-time conversation logs",
                icon = Icons.Outlined.ChatBubbleOutline,
                color = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Cloud Project Identity", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("studio-4383603118-a2874", fontSize = 12.sp, color = PrimaryPurple, fontWeight = FontWeight.SemiBold)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "HEALTHY • SPARK TIER",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Firestore Reads Quota:", fontSize = 12.sp, color = TextSecondary)
                        Text("50,000 / day (Safe)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Firestore Writes Quota:", fontSize = 12.sp, color = TextSecondary)
                        Text("20,000 / day (Safe)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Firebase Storage Quota:", fontSize = 12.sp, color = TextSecondary)
                        Text("1.0 GB Free Quota", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                }
            }
        }
    }
}

/**
 * Settings & Diagnostics Tab.
 */
@Composable
private fun AdminSettingsSection() {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "Platform Settings & Diagnostics",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Super Admin Credentials", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Account: mdmaheraz65@gmail.com", fontSize = 12.5.sp, color = TextSecondary)
                Text("Role: SUPER_ADMIN (Unrestricted access to all colleges, listings & controls)", fontSize = 12.sp, color = PrimaryPurple)

                HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Image Cache Status", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Original byte preservation enabled", fontSize = 11.5.sp, color = TextMuted)
                    }
                    FrostedGlassButton(
                        text = "Clear Cache",
                        onClick = {
                            Toast.makeText(context, "Local memory and disk image cache cleared", Toast.LENGTH_SHORT).show()
                        },
                        height = 32.dp
                    )
                }
            }
        }
    }
}

/**
 * User Profile Portfolio Dialog.
 * Displays user's credentials, moderation controls, published book sets,
 * and an interactive shared photos gallery with view, download, and admin delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminUserProfilePortfolioDialog(
    user: UserProfile,
    allBookSets: List<BookSet>,
    onDismiss: () -> Unit,
    onPhotoClick: (String) -> Unit,
    onUserStatusUpdated: () -> Unit
) {
    val context = LocalContext.current
    val userListings = remember(user, allBookSets) {
        allBookSets.filter { it.sellerPhone == user.phone || it.sellerName == user.name }
    }

    // Gather all photos associated with this student's published sets + profile picture
    val sharedPhotos = remember(user, userListings) {
        val photos = mutableListOf<String>()
        val avatar = user.profilePhotoUri
        if (!avatar.isNullOrBlank() && !avatar.contains("default_avatar")) {
            photos.add(avatar)
        }
        userListings.forEach { set ->
            set.primaryCoverUrl?.let { photos.add(it) }
            set.imageUrls.forEach { if (it.isNotBlank()) photos.add(it) }
            set.includedBooks.flatMap { it.imageUris }.forEach { if (it.isNotBlank()) photos.add(it) }
        }
        photos.distinct()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = BgCanvas
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(user.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Student Portfolio & Management", fontSize = 11.5.sp, color = TextMuted)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                    )
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Profile Header Card
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = PrimaryPurpleLight,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clickable {
                                            val photo = user.profilePhotoUri
                                            if (!photo.isNullOrBlank()) onPhotoClick(photo)
                                        }
                                ) {
                                    val photo = user.profilePhotoUri
                                    if (!photo.isNullOrBlank() && !photo.contains("default_avatar")) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(photo)
                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                .memoryCachePolicy(CachePolicy.ENABLED)
                                                .build(),
                                            contentDescription = "Avatar",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = user.name.take(1).uppercase(),
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryPurple
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(user.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val statusColor = when (user.accountStatus) {
                                            "ACTIVE" -> Color(0xFF10B981)
                                            "SUSPENDED" -> Color(0xFFF59E0B)
                                            else -> Color(0xFFEF4444)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = statusColor.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = user.accountStatus,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = statusColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(user.email, fontSize = 12.5.sp, color = TextSecondary)
                                    Text(user.phone, fontSize = 12.sp, color = TextMuted)
                                    Text("${user.technology.displayName} • ${user.currentSemester.displayName}", fontSize = 11.5.sp, color = PrimaryPurple)
                                    Text(user.college, fontSize = 11.sp, color = TextMuted)
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 12.dp))

                            // Contact buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:${user.phone}")
                                        }
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                                ) {
                                    Icon(Icons.Outlined.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Call", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:${user.email}")
                                        }
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Email", fontSize = 12.sp)
                                }
                            }

                            // Moderation Actions
                            if (user.role != "ADMIN") {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (user.accountStatus == "ACTIVE") {
                                        FrostedGlassButton(
                                            text = "Suspend",
                                            onClick = {
                                                MarketplaceRepository.adminSuspendUser(user.id)
                                                onUserStatusUpdated()
                                                Toast.makeText(context, "Student account suspended", Toast.LENGTH_SHORT).show()
                                            },
                                            textColor = Color(0xFFF59E0B),
                                            height = 34.dp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        FrostedGlassButton(
                                            text = "Block",
                                            onClick = {
                                                MarketplaceRepository.adminBlockUser(user.id)
                                                onUserStatusUpdated()
                                                Toast.makeText(context, "Student account blocked", Toast.LENGTH_SHORT).show()
                                            },
                                            textColor = Color(0xFFEF4444),
                                            height = 34.dp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        GlassButton(
                                            text = "Reactivate",
                                            onClick = {
                                                MarketplaceRepository.adminRestoreUser(user.id)
                                                onUserStatusUpdated()
                                                Toast.makeText(context, "Student account reactivated", Toast.LENGTH_SHORT).show()
                                            },
                                            height = 34.dp,
                                            modifier = Modifier.weight(1f),
                                            gradient = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))
                                        )
                                    }

                                    FrostedGlassButton(
                                        text = "Delete",
                                        onClick = {
                                            MarketplaceRepository.adminDeleteUser(user.id)
                                            onUserStatusUpdated()
                                            onDismiss()
                                            Toast.makeText(context, "Student account deleted", Toast.LENGTH_SHORT).show()
                                        },
                                        textColor = Color(0xFFEF4444),
                                        height = 34.dp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Shared Portfolio Photos Section
                    item {
                        Text(
                            text = "📸 Uploaded Portfolio & Listing Photos (${sharedPhotos.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "View, download, or moderate photos uploaded by this student.",
                            fontSize = 11.5.sp,
                            color = TextMuted
                        )
                    }

                    if (sharedPhotos.isEmpty()) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text("No uploaded photos for this student yet.", fontSize = 12.sp, color = TextMuted)
                            }
                        }
                    } else {
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(sharedPhotos) { photoUrl ->
                                    PortfolioPhotoAdminCard(
                                        photoUrl = photoUrl,
                                        onViewPhoto = { onPhotoClick(photoUrl) },
                                        onDownloadPhoto = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, "Book Photo")
                                                putExtra(Intent.EXTRA_TEXT, photoUrl)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Download / Share Book Photo"))
                                        },
                                        onDeletePhoto = {
                                            MarketplaceRepository.deleteUserPortfolioPhoto(user.id, photoUrl)
                                            Toast.makeText(context, "Photo removed by admin", Toast.LENGTH_SHORT).show()
                                            onUserStatusUpdated()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Published Book Listings by this user
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "📚 Published Book Sets (${userListings.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    if (userListings.isEmpty()) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text("No book sets listed by this student.", fontSize = 12.sp, color = TextMuted)
                            }
                        }
                    } else {
                        items(userListings, key = { it.id }) { set ->
                            ListingAdminCard(
                                set = set,
                                onCallSeller = {},
                                onRemove = { MarketplaceRepository.adminRemoveSet(set.id) },
                                onRestore = { MarketplaceRepository.adminRestoreSet(set.id) },
                                onDelete = { MarketplaceRepository.deleteSet(set.id, requestingUserId = "admin_master_1", requestingUserRole = "ADMIN") }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Thumbnail Card for Student Portfolio Photo with direct View, Download, and Delete buttons.
 */
@Composable
private fun PortfolioPhotoAdminCard(
    photoUrl: String,
    onViewPhoto: () -> Unit,
    onDownloadPhoto: () -> Unit,
    onDeletePhoto: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.width(140.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clickable { onViewPhoto() }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(photoUrl)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Portfolio Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Zoom hint pill
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Visibility, contentDescription = "View", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDownloadPhoto,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = "Download/Share", tint = PrimaryPurple, modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = onDeletePhoto,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/**
 * Full-screen Photo Zoom Preview Dialog with Download & Dismiss.
 */
@Composable
private fun AdminPhotoPreviewDialog(
    photoUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(photoUrl)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = "Full Size Preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // Top action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp).clickable { onDismiss() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp).clickable {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Book Photo")
                            putExtra(Intent.EXTRA_TEXT, photoUrl)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Book Photo"))
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Share, contentDescription = "Share", tint = Color.White)
                    }
                }
            }
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
    icon: ImageVector,
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
                    fontSize = 20.sp,
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
 * Report Card with resolution buttons.
 */
@Composable
private fun ReportAdminCard(
    report: ReportItem,
    onResolve: () -> Unit,
    onDismiss: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (report.status == "PENDING") Color(0xFFEF4444).copy(alpha = 0.12f) else Color(0xFF10B981).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = report.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (report.status == "PENDING") Color(0xFFEF4444) else Color(0xFF10B981),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "Target: ${report.targetType}",
                    fontSize = 11.5.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = report.targetTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "Reason: ${report.reason}",
                fontSize = 12.5.sp,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Text(
                text = "Reported by: ${report.reporterName} (${report.reporterEmail})",
                fontSize = 11.sp,
                color = TextMuted
            )

            if (report.status == "PENDING") {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FrostedGlassButton(
                        text = "Dismiss",
                        onClick = onDismiss,
                        textColor = TextMuted,
                        height = 30.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    GlassButton(
                        text = "Resolve Dispute",
                        onClick = onResolve,
                        height = 30.dp,
                        gradient = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))
                    )
                }
            }
        }
    }
}

/**
 * Student User Card with complete details and moderation actions.
 */
@Composable
private fun StudentAdminCard(
    user: UserProfile,
    onCardClick: () -> Unit,
    onCallUser: () -> Unit,
    onSuspend: () -> Unit,
    onBlock: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
    ) {
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
    set: BookSet,
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

/**
 * Traffic Chart displaying campus volume.
 */
@Composable
private fun CampusTrafficChart(trafficStats: AppTrafficStats) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(20.dp), ambientColor = SoftShadowColor),
        shape = RoundedCornerShape(20.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Campus Visit Volume",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Today's student engagement timeline",
                        fontSize = 11.5.sp,
                        color = TextMuted
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryPurpleLight
                ) {
                    Text(
                        text = "FPI + FCI Combined",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bar chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val hourSlots = listOf(
                    "8 AM" to 0.25f,
                    "10 AM" to 0.65f,
                    "12 PM" to 0.85f,
                    "2 PM" to 1.0f,
                    "4 PM" to 0.70f,
                    "6 PM" to 0.45f,
                    "9 PM" to 0.90f
                )

                hourSlots.forEach { (time, ratio) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                            color = if (ratio >= 0.9f) PrimaryPurple else PrimaryPurple.copy(alpha = 0.35f),
                            modifier = Modifier
                                .width(22.dp)
                                .height((ratio * 80).dp)
                        ) {}
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(time, fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                    }
                }
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
