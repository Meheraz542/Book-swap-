package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.MarketplaceRepository
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSearch: (technology: Technology?, semester: Semester?) -> Unit,
    onNavigateToSetDetails: (BookSet) -> Unit,
    onNavigateToProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    onNavigateToSell: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentUser by MarketplaceRepository.currentUser.collectAsState()
    val bookSets by MarketplaceRepository.bookSets.collectAsState()
    val notifications by MarketplaceRepository.notifications.collectAsState()
    val unreadNotifs = notifications.count { !it.isRead }

    // Step 1: Selected College (strictly either FPI or FCI)
    var selectedCollege by remember(currentUser.college) {
        mutableStateOf(
            if (currentUser.college.contains("Feni Computer", ignoreCase = true)) {
                CollegeDirectory.FENI_COMPUTER_INSTITUTE
            } else {
                CollegeDirectory.FENI_POLYTECHNIC
            }
        )
    }

    // Step 2: Selected Semester (null = All Semesters)
    var selectedSemester by remember { mutableStateOf<Semester?>(null) }

    // Step 3: Selected Technology (null = All Technologies in selected college)
    var selectedTechnology by remember { mutableStateOf<Technology?>(null) }

    // State for Filter Modal BottomSheet (triggered by the corner filter button in SearchBar)
    var showFilterBottomSheet by remember { mutableStateOf(false) }

    // Dropdown states for quick filter pill bar
    var showCollegeDropdown by remember { mutableStateOf(false) }
    var showTechDropdown by remember { mutableStateOf(false) }
    var showSemDropdown by remember { mutableStateOf(false) }

    // Technologies offered by the currently selected college
    val activeTechnologies = remember(selectedCollege) {
        CollegeDirectory.getTechnologiesForCollege(selectedCollege)
    }

    // Reset selectedTechnology if it does not belong to the selected college
    LaunchedEffect(selectedCollege) {
        if (selectedTechnology != null && !activeTechnologies.contains(selectedTechnology)) {
            selectedTechnology = null
        }
    }

    // Filter books strictly: College -> Semester -> Technology
    val matchingSets = remember(bookSets, selectedCollege, selectedSemester, selectedTechnology) {
        bookSets.filter { set ->
            val matchesStatus = set.status == SetStatus.AVAILABLE
            val matchesCollege = CollegeDirectory.matchesCollege(set.college, selectedCollege)
            val matchesSemester = selectedSemester == null || set.semester == selectedSemester
            val matchesTechnology = selectedTechnology == null || set.technology == selectedTechnology
            matchesStatus && matchesCollege && matchesSemester && matchesTechnology
        }
    }

    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                MarketplaceRepository.refreshBookSets()
                isRefreshing = false
            }
        },
        modifier = modifier
            .fillMaxSize()
            .testTag("home_pull_to_refresh")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(BgCanvasGradientStart, BgCanvasGradientEnd)
                    )
                ),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
        // Top Header
        item {
            HomeHeader(
                userName = currentUser.name.split(" ").firstOrNull() ?: "Student",
                avatarRes = currentUser.profilePhotoRes,
                avatarUri = currentUser.profilePhotoUri,
                unreadNotifications = unreadNotifs,
                onAvatarClick = onNavigateToProfile,
                onNotificationClick = onOpenNotifications
            )
        }

        // Search Bar with Corner Filter Option (opens Filter Dialog/BottomSheet)
        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                GlassSearchBar(
                    query = "",
                    onQueryChange = {},
                    onFilterClick = { showFilterBottomSheet = true },
                    readOnly = true,
                    onClick = { onNavigateToSearch(selectedTechnology, selectedSemester) }
                )
            }
        }

        // Quick Dropdown Filters Bar (Instant selection matching user request - no horizontal scrolling needed)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. College Dropdown Pill
                Box(modifier = Modifier.weight(1.1f)) {
                    Surface(
                        onClick = { showCollegeDropdown = true },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.35f)),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    imageVector = if (selectedCollege.contains("Computer")) Icons.Outlined.Computer else Icons.Outlined.School,
                                    contentDescription = null,
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (selectedCollege.contains("Computer")) "FCI" else "FPI",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showCollegeDropdown,
                        onDismissRequest = { showCollegeDropdown = false },
                        properties = androidx.compose.ui.window.PopupProperties(focusable = true)
                    ) {
                        CollegeDirectory.ALL_COLLEGES.forEach { col ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            if (col.contains("Computer")) "Feni Computer Institute (FCI)" else "Feni Polytechnic Institute (FPI)",
                                            fontWeight = if (col == selectedCollege) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            if (col.contains("Computer")) "2 Departments: CSE, Telecom" else "6 Technologies: Civil, Electrical, etc.",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                },
                                onClick = {
                                    showCollegeDropdown = false
                                    selectedCollege = col
                                }
                            )
                        }
                    }
                }

                // 2. Department / Technology Dropdown Pill
                Box(modifier = Modifier.weight(1.3f)) {
                    val isTechActive = selectedTechnology != null
                    Surface(
                        onClick = { showTechDropdown = true },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isTechActive) PrimaryPurpleLight.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.95f),
                        border = BorderStroke(
                            1.dp,
                            if (isTechActive) PrimaryPurple else Color(0xFFE2E8F0)
                        ),
                        shadowElevation = if (isTechActive) 3.dp else 1.dp,
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Engineering,
                                    contentDescription = null,
                                    tint = if (isTechActive) PrimaryPurple else TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = selectedTechnology?.shortName ?: "All Dept",
                                    fontSize = 12.sp,
                                    fontWeight = if (isTechActive) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isTechActive) PrimaryPurple else TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = if (isTechActive) PrimaryPurple else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showTechDropdown,
                        onDismissRequest = { showTechDropdown = false },
                        properties = androidx.compose.ui.window.PopupProperties(focusable = true)
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Departments", fontSize = 13.sp, fontWeight = if (selectedTechnology == null) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                showTechDropdown = false
                                selectedTechnology = null
                            }
                        )
                        activeTechnologies.forEach { tech ->
                            DropdownMenuItem(
                                text = { Text(tech.displayName, fontSize = 13.sp, fontWeight = if (selectedTechnology == tech) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    showTechDropdown = false
                                    selectedTechnology = tech
                                }
                            )
                        }
                    }
                }

                // 3. Semester Dropdown Pill
                Box(modifier = Modifier.weight(1.2f)) {
                    val isSemActive = selectedSemester != null
                    Surface(
                        onClick = { showSemDropdown = true },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSemActive) PrimaryPurpleLight.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.95f),
                        border = BorderStroke(
                            1.dp,
                            if (isSemActive) PrimaryPurple else Color(0xFFE2E8F0)
                        ),
                        shadowElevation = if (isSemActive) 3.dp else 1.dp,
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarMonth,
                                    contentDescription = null,
                                    tint = if (isSemActive) PrimaryPurple else TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = selectedSemester?.displayName ?: "All Sem",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSemActive) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSemActive) PrimaryPurple else TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = if (isSemActive) PrimaryPurple else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showSemDropdown,
                        onDismissRequest = { showSemDropdown = false },
                        properties = androidx.compose.ui.window.PopupProperties(focusable = true)
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Semesters", fontSize = 13.sp, fontWeight = if (selectedSemester == null) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                showSemDropdown = false
                                selectedSemester = null
                            }
                        )
                        Semester.entries.forEach { sem ->
                            DropdownMenuItem(
                                text = { Text(sem.displayName, fontSize = 13.sp, fontWeight = if (selectedSemester == sem) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    showSemDropdown = false
                                    selectedSemester = sem
                                }
                            )
                        }
                    }
                }

                // Reset filter button if any filter applied
                if (selectedTechnology != null || selectedSemester != null) {
                    Surface(
                        onClick = {
                            selectedTechnology = null
                            selectedSemester = null
                        },
                        shape = CircleShape,
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear Filters",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // STEP 1: SELECT COLLEGE
        // ==========================================
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = PrimaryPurpleLight,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "1",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = PrimaryPurple
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Select College",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryPurpleLight.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "2 Campuses",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Two College Cards side by side
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Feni Polytechnic Institute
                val isFpiSelected = selectedCollege == CollegeDirectory.FENI_POLYTECHNIC
                Surface(
                    onClick = {
                        selectedCollege = CollegeDirectory.FENI_POLYTECHNIC
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(84.dp)
                        .shadow(
                            elevation = if (isFpiSelected) 6.dp else 2.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = if (isFpiSelected) PrimaryPurple.copy(alpha = 0.35f) else SoftShadowColor,
                            spotColor = if (isFpiSelected) PrimaryPurple.copy(alpha = 0.35f) else SoftShadowColor
                        ),
                    shape = RoundedCornerShape(20.dp),
                    color = if (isFpiSelected) Color.White else Color.White.copy(alpha = 0.85f),
                    border = BorderStroke(
                        width = if (isFpiSelected) 2.dp else 1.dp,
                        color = if (isFpiSelected) PrimaryPurple else Color(0xFFE2E8F0)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (isFpiSelected) Modifier.background(PrimaryPurpleLight.copy(alpha = 0.25f))
                                else Modifier
                            )
                            .padding(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isFpiSelected) PrimaryPurple else Color(0xFFF1F5F9),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.School,
                                            contentDescription = null,
                                            tint = if (isFpiSelected) Color.White else PrimaryPurple,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                if (isFpiSelected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = PrimaryPurple,
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Column {
                                Text(
                                    text = "Feni Polytechnic",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFpiSelected) PrimaryPurple else TextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = "FPI • 6 Technologies",
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Feni Computer Institute
                val isFciSelected = selectedCollege == CollegeDirectory.FENI_COMPUTER_INSTITUTE
                Surface(
                    onClick = {
                        selectedCollege = CollegeDirectory.FENI_COMPUTER_INSTITUTE
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(84.dp)
                        .shadow(
                            elevation = if (isFciSelected) 6.dp else 2.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = if (isFciSelected) PrimaryPurple.copy(alpha = 0.35f) else SoftShadowColor,
                            spotColor = if (isFciSelected) PrimaryPurple.copy(alpha = 0.35f) else SoftShadowColor
                        ),
                    shape = RoundedCornerShape(20.dp),
                    color = if (isFciSelected) Color.White else Color.White.copy(alpha = 0.85f),
                    border = BorderStroke(
                        width = if (isFciSelected) 2.dp else 1.dp,
                        color = if (isFciSelected) PrimaryPurple else Color(0xFFE2E8F0)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (isFciSelected) Modifier.background(PrimaryPurpleLight.copy(alpha = 0.25f))
                                else Modifier
                            )
                            .padding(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isFciSelected) PrimaryPurple else Color(0xFFF1F5F9),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.Computer,
                                            contentDescription = null,
                                            tint = if (isFciSelected) Color.White else PrimaryPurple,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                if (isFciSelected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = PrimaryPurple,
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Column {
                                Text(
                                    text = "Feni Computer Inst.",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFciSelected) PrimaryPurple else TextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = "FCI • 2 Departments",
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // STEP 2: SELECT SEMESTER
        // ==========================================
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = PrimaryPurpleLight,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "2",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = PrimaryPurple
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Select Semester",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                if (selectedSemester != null) {
                    Text(
                        text = "All Semesters",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryPurple,
                        modifier = Modifier.clickable { selectedSemester = null }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "All Semesters" Chip
                item {
                    LiquidGlassChip(
                        text = "All Semesters",
                        selected = selectedSemester == null,
                        onClick = { selectedSemester = null },
                        testTag = "chip_all_semesters"
                    )
                }

                // 1st to 7th Semester Chips
                items(Semester.entries) { sem ->
                    LiquidGlassChip(
                        text = sem.displayName,
                        selected = selectedSemester == sem,
                        onClick = {
                            selectedSemester = if (selectedSemester == sem) null else sem
                        },
                        testTag = "chip_sem_${sem.name}"
                    )
                }
            }
        }

        // ==========================================
        // STEP 3: SELECT TECHNOLOGY
        // ==========================================
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = PrimaryPurpleLight,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "3",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = PrimaryPurple
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedCollege == CollegeDirectory.FENI_COMPUTER_INSTITUTE) {
                            "FCI Departments (2)"
                        } else {
                            "FPI Technologies (6)"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                if (selectedTechnology != null) {
                    Text(
                        text = "All Tech",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryPurple,
                        modifier = Modifier.clickable { selectedTechnology = null }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            TechnologyGrid(
                technologies = activeTechnologies,
                selectedTechnology = selectedTechnology,
                onSelectTechnology = { tech ->
                    selectedTechnology = if (selectedTechnology == tech) null else tech
                }
            )
        }

        // ==========================================
        // SUGGESTED & AVAILABLE BOOKS SECTION
        // ==========================================
        item {
            Spacer(modifier = Modifier.height(26.dp))

            // Active Filter Summary Header
            val shortCollegeName = if (selectedCollege.contains("Feni Computer")) "FCI" else "FPI"
            val semLabel = selectedSemester?.displayName ?: "All Semesters"
            val techLabel = selectedTechnology?.shortName ?: "All Tech"

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Available Books",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "$shortCollegeName • $semLabel • $techLabel (${matchingSets.size} books found)",
                        fontSize = 12.sp,
                        color = PrimaryPurple,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (selectedSemester != null || selectedTechnology != null) {
                    Surface(
                        onClick = {
                            selectedSemester = null
                            selectedTechnology = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear",
                                tint = TextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Reset",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Empty State or List of Books
        if (matchingSets.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.size(60.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.SearchOff,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "No Books Found for this Selection",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "There are currently no book sets listed for ${if (selectedCollege.contains("Feni Computer")) "FCI" else "FPI"} under ${selectedSemester?.displayName ?: ""} ${selectedTechnology?.shortName ?: ""}. Try another semester or check back soon.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FrostedGlassButton(
                                    text = "Clear Filters",
                                    onClick = {
                                        selectedSemester = null
                                        selectedTechnology = null
                                    },
                                    modifier = Modifier.weight(1f),
                                    height = 42.dp,
                                    testTag = "btn_clear_filters"
                                )

                                GlassButton(
                                    text = "Sell Books",
                                    onClick = onNavigateToSell,
                                    modifier = Modifier.weight(1f),
                                    height = 42.dp,
                                    testTag = "btn_sell_books"
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Horizontal Carousel of matching books
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(matchingSets) { set ->
                        BookSetCardHorizontal(
                            set = set,
                            onClick = { onNavigateToSetDetails(set) },
                            onBookmarkClick = { MarketplaceRepository.toggleSaveSet(set.id) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Vertical list of all matching books
            items(matchingSets) { set ->
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    BookSetCardVertical(
                        set = set,
                        onClick = { onNavigateToSetDetails(set) },
                        onBookmarkClick = { MarketplaceRepository.toggleSaveSet(set.id) }
                    )
                }
            }
        }
    }

    // ==========================================================
    // FILTER BOTTOM SHEET (Triggered by SearchBar Corner Button)
    // ==========================================================
    if (showFilterBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterBottomSheet = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color(0xFFCBD5E1),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Box(modifier = Modifier.size(width = 40.dp, height = 4.dp))
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = null,
                            tint = PrimaryPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Filter Books",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    FrostedGlassButton(
                        text = "Reset All",
                        onClick = {
                            selectedSemester = null
                            selectedTechnology = null
                        },
                        textColor = PrimaryPurple,
                        height = 36.dp,
                        testTag = "btn_reset_filters"
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 12.dp))

                val isFci = selectedCollege == CollegeDirectory.FENI_COMPUTER_INSTITUTE

                // 1. Filter by College / Institute Dropdown
                GlassNeumorphicDropdownField(
                    label = "College / Institute",
                    selectedValue = selectedCollege,
                    displayValue = { col ->
                        when (col) {
                            CollegeDirectory.FENI_POLYTECHNIC -> "Feni Polytechnic Institute (FPI)"
                            CollegeDirectory.FENI_COMPUTER_INSTITUTE -> "Feni Computer Institute (FCI)"
                            else -> col ?: "Select College"
                        }
                    },
                    options = listOf(
                        CollegeDirectory.FENI_POLYTECHNIC,
                        CollegeDirectory.FENI_COMPUTER_INSTITUTE
                    ),
                    onSelect = { col ->
                        if (col != null) selectedCollege = col
                    },
                    subtitle = { col ->
                        if (col == CollegeDirectory.FENI_COMPUTER_INSTITUTE) "2 Departments: CSE, Telecom"
                        else "6 Technologies: Civil, Electrical, Mechanical, Power, Computer, AIDT"
                    },
                    leadingIcon = if (isFci) Icons.Outlined.Computer else Icons.Outlined.School,
                    testTag = "home_sheet_dropdown_college"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Filter by Technology / Department (dynamic for selected college)
                GlassNeumorphicDropdownField(
                    label = if (isFci) "Department" else "Technology / Department",
                    selectedValue = selectedTechnology,
                    displayValue = { it?.displayName ?: "All Technologies" },
                    options = listOf<Technology?>(null) + activeTechnologies,
                    onSelect = { tech -> selectedTechnology = tech },
                    placeholder = "All Technologies",
                    supportingText = "Available in this institute: ${activeTechnologies.size} departments",
                    leadingIcon = Icons.Outlined.Engineering,
                    testTag = "home_sheet_dropdown_tech"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Filter by Semester
                GlassNeumorphicDropdownField(
                    label = "Semester",
                    selectedValue = selectedSemester,
                    displayValue = { it?.displayName ?: "All Semesters" },
                    options = listOf<Semester?>(null) + Semester.entries,
                    onSelect = { sem -> selectedSemester = sem },
                    placeholder = "All Semesters",
                    leadingIcon = Icons.Outlined.CalendarMonth,
                    testTag = "home_sheet_dropdown_sem"
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Apply Button
                GlassButton(
                    text = "Apply Filters (${matchingSets.size} Books Found)",
                    onClick = { showFilterBottomSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    testTag = "btn_apply_filters"
                )
            }
        }
    }
}
}

@Composable
private fun HomeHeader(
    userName: String,
    avatarRes: Int?,
    avatarUri: String? = null,
    unreadNotifications: Int,
    onAvatarClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onAvatarClick() }
            ) {
                Image(
                    painter = painterResource(id = com.example.R.drawable.ic_bookswap_logo),
                    contentDescription = "BookSwap Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "BookSwap",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryPurple,
                    letterSpacing = 0.3.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Hello, $userName",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "👋", fontSize = 16.sp)
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Notification bell with unread dot
            Box {
                GlassIconButton(
                    onClick = onNotificationClick,
                    icon = if (unreadNotifications > 0) Icons.Default.Notifications else Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tone = if (unreadNotifications > 0) GlassIconTone.PRIMARY_PURPLE else GlassIconTone.FROSTED_WHITE,
                    size = 46.dp,
                    testTag = "notification_button"
                )
                if (unreadNotifications > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 4.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // User Avatar with Liquid Glass frame
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .liquidGlass(
                        shape = CircleShape,
                        backgroundColor = Color.White.copy(alpha = 0.95f),
                        elevation = 6.dp,
                        shadowColor = SoftShadowColor,
                        innerHighlightColor = Color.White
                    )
                    .clip(CircleShape)
                    .clickable { onAvatarClick() }
                    .testTag("home_avatar"),
                contentAlignment = Alignment.Center
            ) {
                if (!avatarUri.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUri,
                        contentDescription = "Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (avatarRes != null) {
                    Image(
                        painter = painterResource(id = avatarRes),
                        contentDescription = "Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PrimaryPurpleLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.take(1),
                            color = PrimaryPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onSeeAllClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "See all",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = AccentBlue,
            modifier = Modifier.clickable { onSeeAllClick() }
        )
    }
}

@Composable
private fun SemesterChip(
    semester: Semester,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .height(44.dp)
            .shadow(
                elevation = if (isSelected) 6.dp else 2.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = if (isSelected) PrimaryPurple.copy(alpha = 0.3f) else SoftShadowColor,
                spotColor = if (isSelected) PrimaryPurple.copy(alpha = 0.3f) else SoftShadowColor
            ),
        shape = RoundedCornerShape(22.dp),
        color = if (isSelected) Color.Transparent else Color.White
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (isSelected) Modifier.background(PurpleGradient)
                    else Modifier.background(Color.White)
                )
                .padding(horizontal = 18.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = semester.shortName,
                color = if (isSelected) Color.White else TextSecondary,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

data class TechItemUi(
    val tech: Technology,
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color
)

@Composable
private fun TechnologyGrid(
    technologies: List<Technology>,
    selectedTechnology: Technology?,
    onSelectTechnology: (Technology) -> Unit
) {
    val allItems = listOf(
        TechItemUi(Technology.CIVIL, Icons.Outlined.Engineering, Color(0xFFE0EDFF), Color(0xFF2563EB)),
        TechItemUi(Technology.ELECTRICAL, Icons.Outlined.ElectricBolt, Color(0xFFFEF3C7), Color(0xFFD97706)),
        TechItemUi(Technology.MECHANICAL, Icons.Outlined.Settings, Color(0xFFF3E8FF), Color(0xFF9333EA)),
        TechItemUi(Technology.POWER, Icons.Outlined.Power, Color(0xFFDCFCE7), Color(0xFF16A34A)),
        TechItemUi(Technology.CSE, Icons.Outlined.Code, Color(0xFFE0F2FE), Color(0xFF0284C7)),
        TechItemUi(Technology.AIDT, Icons.Outlined.Build, Color(0xFFFCE7F3), Color(0xFFDB2777)),
        TechItemUi(Technology.TELECOM, Icons.Outlined.Call, Color(0xFFCCFBF1), Color(0xFF0D9488))
    )

    val items = allItems.filter { item -> technologies.contains(item.tech) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Chunk items: If 2 items (like FCI), 2 per row; otherwise 3 per row
        val chunkSize = if (items.size <= 2) 2 else if (items.size <= 4) 2 else 3
        val rows = items.chunked(chunkSize)

        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { item ->
                    val isSelected = selectedTechnology == item.tech
                    TechnologyItem(
                        item = item,
                        isSelected = isSelected,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectTechnology(item.tech) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TechnologyItem(
    item: TechItemUi,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(58.dp)
            .shadow(
                elevation = if (isSelected) 6.dp else 2.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = if (isSelected) PrimaryPurple.copy(alpha = 0.35f) else SoftShadowColor,
                spotColor = if (isSelected) PrimaryPurple.copy(alpha = 0.35f) else SoftShadowColor
            ),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.9f),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) PrimaryPurple else Color(0xFFE2E8F0)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isSelected) Modifier.background(PrimaryPurpleLight.copy(alpha = 0.3f))
                    else Modifier
                )
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) PrimaryPurple else item.iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Outlined.Check else item.icon,
                    contentDescription = item.tech.shortName,
                    tint = if (isSelected) Color.White else item.iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = item.tech.shortName,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) PrimaryPurple else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val deptSubtitle = when (item.tech) {
                    Technology.CSE -> "Computer Science"
                    Technology.TELECOM -> "Telecommunication"
                    Technology.CIVIL -> "Civil Engineering"
                    Technology.ELECTRICAL -> "Electrical Eng."
                    Technology.MECHANICAL -> "Mechanical Eng."
                    Technology.POWER -> "Power Engineering"
                    Technology.AIDT -> "Architecture & Interior"
                }
                Text(
                    text = deptSubtitle,
                    fontSize = 10.sp,
                    color = TextMuted,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun BookSetCardHorizontal(
    set: BookSet,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estimatedMrp = (set.totalPrice * 2.2).toInt()

    GlassCard(
        modifier = modifier
            .width(190.dp)
            .testTag("bookset_card_${set.id}"),
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        elevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(136.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFE2E8F0))
        ) {
            if (set.imageUrls.isNotEmpty()) {
                AsyncImage(
                    model = set.imageUrls.first(),
                    contentDescription = set.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (set.coverDrawableRes != null) {
                Image(
                    painter = painterResource(id = set.coverDrawableRes),
                    contentDescription = set.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top Left Condition Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                ConditionBadge(condition = set.overallCondition)
            }

            // Bookmark button at top right
            Surface(
                onClick = onBookmarkClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.92f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (set.isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save",
                        tint = if (set.isSaved) PrimaryPurple else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Bottom Left Book Count Pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = "📚 ${set.includedBooks.size} Books Set",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = set.title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "${set.semester.shortName} Sem • ${set.technology.shortName}",
            fontSize = 12.sp,
            color = TextMuted,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "Tk ${set.totalPrice}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF059669)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Tk $estimatedMrp",
                    fontSize = 11.sp,
                    color = TextMuted,
                    textDecoration = TextDecoration.LineThrough
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFECFDF5)
            ) {
                Text(
                    text = "50% OFF",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF059669),
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun BookSetCardVertical(
    set: BookSet,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estimatedMrp = (set.totalPrice * 2.1).toInt()

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bookset_vertical_${set.id}"),
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFE2E8F0))
            ) {
                if (set.imageUrls.isNotEmpty()) {
                    AsyncImage(
                        model = set.imageUrls.first(),
                        contentDescription = set.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (set.coverDrawableRes != null) {
                    Image(
                        painter = painterResource(id = set.coverDrawableRes),
                        contentDescription = set.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                ) {
                    Text(
                        text = "${set.includedBooks.size} Books",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = set.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    ConditionBadge(condition = set.overallCondition)
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${set.semester.displayName} • ${set.technology.shortName}",
                    fontSize = 12.sp,
                    color = TextMuted
                )

                Text(
                    text = "🏫 ${set.college}",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val allUsers by MarketplaceRepository.allUsers.collectAsState()
                val seller = allUsers.find { it.id == set.sellerId }
                val sellerAvatarUri = seller?.profilePhotoUri

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(16.dp),
                        shape = CircleShape,
                        color = Color(0xFFE2E8F0)
                    ) {
                        if (!sellerAvatarUri.isNullOrBlank()) {
                            AsyncImage(
                                model = sellerAvatarUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (seller?.profilePhotoRes != null) {
                            Image(
                                painter = painterResource(id = seller.profilePhotoRes!!),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.padding(2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = seller?.name ?: set.sellerName,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Tk ${set.totalPrice}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF059669)
                    )
                    Text(
                        text = "Tk $estimatedMrp",
                        fontSize = 11.sp,
                        color = TextMuted,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            GlassIconButton(
                onClick = onBookmarkClick,
                icon = if (set.isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = "Save",
                tone = if (set.isSaved) GlassIconTone.PRIMARY_PURPLE else GlassIconTone.FROSTED_WHITE,
                size = 38.dp,
                testTag = "bookmark_${set.id}"
            )
        }
    }
}
