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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MarketplaceRepository
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    initialQuery: String = "",
    initialTechnology: Technology? = null,
    initialSemester: Semester? = null,
    onNavigateBack: () -> Unit,
    onNavigateToSetDetails: (BookSet) -> Unit,
    modifier: Modifier = Modifier
) {
    val bookSets by MarketplaceRepository.bookSets.collectAsState()

    var searchQuery by remember { mutableStateOf(initialQuery) }
    
    // Default college based on initialTechnology or FPI
    var selectedCollege by remember {
        mutableStateOf(
            if (initialTechnology == Technology.TELECOM) {
                CollegeDirectory.FENI_COMPUTER_INSTITUTE
            } else {
                CollegeDirectory.FENI_POLYTECHNIC
            }
        )
    }
    var selectedTechnology by remember { mutableStateOf(initialTechnology) }
    var selectedSemester by remember { mutableStateOf(initialSemester) }
    var showCollegeDropdown by remember { mutableStateOf(false) }

    // Technologies filtered by selected college
    val availableTechs = remember(selectedCollege) {
        CollegeDirectory.getTechnologiesForCollege(selectedCollege)
    }

    // Auto clear tech if not valid for selected college
    LaunchedEffect(selectedCollege) {
        if (selectedTechnology != null && !availableTechs.contains(selectedTechnology)) {
            selectedTechnology = null
        }
    }

    // Filter sets dynamically based on College -> Technology -> Semester
    val filteredSets = remember(bookSets, searchQuery, selectedCollege, selectedTechnology, selectedSemester) {
        bookSets.filter { set ->
            val matchStatus = set.status == SetStatus.AVAILABLE
            val matchCollege = set.college.trim().equals(selectedCollege.trim(), ignoreCase = true)
            val matchTech = selectedTechnology == null || set.technology == selectedTechnology
            val matchSem = selectedSemester == null || set.semester == selectedSemester

            val q = searchQuery.trim().lowercase()
            val matchQuery = if (q.isEmpty()) true else {
                set.title.lowercase().contains(q) ||
                        set.college.lowercase().contains(q) ||
                        set.subject.lowercase().contains(q) ||
                        set.technology.displayName.lowercase().contains(q) ||
                        set.semester.displayName.lowercase().contains(q) ||
                        set.includedBooks.any {
                            it.bookName.lowercase().contains(q) ||
                                    it.subject.lowercase().contains(q) ||
                                    it.author.lowercase().contains(q)
                        }
            }

            matchStatus && matchCollege && matchTech && matchSem && matchQuery
        }
    }

    val isFci = selectedCollege == CollegeDirectory.FENI_COMPUTER_INSTITUTE

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
                // Search row with back button and full-width search input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassIconButton(
                        onClick = onNavigateBack,
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tone = GlassIconTone.FROSTED_WHITE,
                        size = 44.dp,
                        testTag = "btn_search_back"
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Input field with full Glass Effect and Neumorphism
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .liquidGlass(
                                shape = RoundedCornerShape(25.dp),
                                backgroundColor = Color.White.copy(alpha = 0.92f),
                                borderBrush = GlassRimBrush,
                                borderWidth = 1.2.dp,
                                elevation = 4.dp,
                                shadowColor = SoftShadowColor,
                                innerShadowColor = Color(0x10000000),
                                innerHighlightColor = Color.White,
                                showTopSheen = true
                            )
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text("Search books or subjects...", color = TextMuted, fontSize = 14.sp)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("search_input_field"),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = PrimaryPurple
                                )
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Clear",
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
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
                .padding(innerPadding)
                .imePadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ==========================================================
            // GLASS + NEUMORPHISM DROPDOWN SELECTION PANEL
            // Replaces horizontal sliding with vertical dropdowns matching user reference
            // ==========================================================
            item {
                val hasActiveFilters = selectedTechnology != null || selectedSemester != null || searchQuery.isNotBlank()

                NeumorphicGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    backgroundColor = CardGlass,
                    elevation = 5.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PrimaryPurpleLight,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.FilterList,
                                        contentDescription = null,
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Department & Semester Filters",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Instant dropdown selection without horizontal scrolling",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        if (hasActiveFilters) {
                            TextButton(
                                onClick = {
                                    selectedTechnology = null
                                    selectedSemester = null
                                    searchQuery = ""
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = null,
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Reset",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PrimaryPurple
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. College / Institute Dropdown
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
                        testTag = "dropdown_college"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Technology / Department Dropdown (Filtered dynamically by chosen College)
                    GlassNeumorphicDropdownField(
                        label = if (isFci) "Department" else "Technology / Department",
                        selectedValue = selectedTechnology,
                        displayValue = { it?.displayName ?: "All Technologies" },
                        options = listOf<Technology?>(null) + availableTechs,
                        onSelect = { tech -> selectedTechnology = tech },
                        placeholder = "All Technologies",
                        supportingText = "Available in this institute: ${availableTechs.size} departments",
                        leadingIcon = Icons.Outlined.Engineering,
                        testTag = "dropdown_tech"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Semester Dropdown (1st to 7th Semester)
                    GlassNeumorphicDropdownField(
                        label = "Semester",
                        selectedValue = selectedSemester,
                        displayValue = { it?.displayName ?: "All Semesters" },
                        options = listOf<Semester?>(null) + Semester.entries,
                        onSelect = { sem -> selectedSemester = sem },
                        placeholder = "All Semesters",
                        leadingIcon = Icons.Outlined.CalendarMonth,
                        testTag = "dropdown_sem"
                    )

                    // Active filter pills preview
                    if (hasActiveFilters) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Active:", fontSize = 11.sp, color = TextMuted)

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PrimaryPurpleLight,
                                modifier = Modifier.height(24.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isFci) "FCI" else "FPI",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryPurple
                                    )
                                }
                            }

                            if (selectedTechnology != null) {
                                Surface(
                                    onClick = { selectedTechnology = null },
                                    shape = RoundedCornerShape(12.dp),
                                    color = PrimaryPurpleLight,
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${selectedTechnology?.shortName} ✕",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PrimaryPurple
                                        )
                                    }
                                }
                            }

                            if (selectedSemester != null) {
                                Surface(
                                    onClick = { selectedSemester = null },
                                    shape = RoundedCornerShape(12.dp),
                                    color = PrimaryPurpleLight,
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${selectedSemester?.displayName} ✕",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PrimaryPurple
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                val collegeShort = if (isFci) "FCI" else "FPI"
                val techShort = selectedTechnology?.shortName ?: "All Technologies"
                val semShort = selectedSemester?.displayName ?: "All Semesters"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Suggested Books",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$collegeShort • $techShort • $semShort (${filteredSets.size} books found)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryPurple
                        )
                    }

                    if (selectedTechnology != null || selectedSemester != null || searchQuery.isNotEmpty()) {
                        Text(
                            text = "Reset",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryPurple,
                            modifier = Modifier.clickable {
                                searchQuery = ""
                                selectedTechnology = null
                                selectedSemester = null
                            }
                        )
                    }
                }
            }

            if (filteredSets.isEmpty()) {
                item {
                    EmptySearchState(
                        collegeName = if (isFci) "Feni Computer Institute (FCI)" else "Feni Polytechnic Institute (FPI)",
                        techName = selectedTechnology?.displayName,
                        semName = selectedSemester?.displayName,
                        onClearFilters = {
                            searchQuery = ""
                            selectedTechnology = null
                            selectedSemester = null
                        }
                    )
                }
            } else {
                items(filteredSets, key = { it.id }, contentType = { "search_result" }) { set ->
                    SearchResultCard(
                        set = set,
                        onClick = { onNavigateToSetDetails(set) },
                        onBookmarkClick = { MarketplaceRepository.toggleSaveSet(set.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    set: BookSet,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estimatedMrp = (set.totalPrice * 2.1).toInt()

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("search_card_${set.id}"),
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
                    .size(86.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFE2E8F0))
            ) {
                if (set.coverDrawableRes != null) {
                    Image(
                        painter = painterResource(id = set.coverDrawableRes),
                        contentDescription = set.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.72f),
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
                contentDescription = "Bookmark",
                tone = if (set.isSaved) GlassIconTone.PRIMARY_PURPLE else GlassIconTone.FROSTED_WHITE,
                size = 38.dp,
                testTag = "search_bookmark_${set.id}"
            )
        }
    }
}

@Composable
private fun EmptySearchState(
    collegeName: String,
    techName: String?,
    semName: String?,
    onClearFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = PrimaryPurpleLight
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Books Found",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        val details = buildString {
            append(collegeName)
            if (techName != null) append(" • $techName")
            if (semName != null) append(" • $semName")
        }

        Text(
            text = "No books are currently listed for $details. Try choosing another semester or technology, or reset the filters.",
            fontSize = 13.sp,
            color = TextMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        FrostedGlassButton(
            text = "Clear Filters",
            onClick = onClearFilters,
            textColor = PrimaryPurple,
            height = 44.dp,
            testTag = "btn_clear_filters"
        )
    }
}
