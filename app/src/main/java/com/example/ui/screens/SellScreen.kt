package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import com.example.R
import com.example.data.MarketplaceRepository
import com.example.data.storage.ImageStorageManager
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellScreen(
    onPublishSuccess: (BookSet) -> Unit,
    onRequireLogin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentUser by MarketplaceRepository.currentUser.collectAsState()
    val isLoggedIn by MarketplaceRepository.isLoggedIn.collectAsState()

    if (!isLoggedIn) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PrimaryPurpleLight,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(32.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Sign in to list books", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Please sign in to your student account to list your semester book set for other students.", fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(18.dp))
                    GlassButton(
                        text = "Sign In / Register",
                        onClick = onRequireLogin,
                        icon = Icons.AutoMirrored.Outlined.Login,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "sell_btn_login_prompt"
                    )
                }
            }
        }
        return
    }

    var college by remember { mutableStateOf(currentUser.college) }
    var collegeDropdownExpanded by remember { mutableStateOf(false) }

    // Technologies filtered for this specific college
    val availableTechsForCollege = remember(college) {
        CollegeDirectory.getTechnologiesForCollege(college)
    }

    var selectedTech by remember { mutableStateOf(currentUser.technology) }
    var selectedSem by remember { mutableStateOf(currentUser.currentSemester) }

    // Ensure selectedTech is always valid for the selected institute
    LaunchedEffect(availableTechsForCollege) {
        if (!availableTechsForCollege.contains(selectedTech)) {
            selectedTech = availableTechsForCollege.firstOrNull() ?: Technology.CIVIL
        }
    }

    var totalPriceText by remember { mutableStateOf("") }
    var overallCondition by remember { mutableStateOf(SetCondition.GOOD) }
    var description by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var coverPhotoUri by remember { mutableStateOf<String?>(null) }
    var isSavingPhoto by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isSavingPhoto = true
                try {
                    val persistentUri = ImageStorageManager.saveImageLocally(context, uri)
                    coverPhotoUri = persistentUri
                } finally {
                    isSavingPhoto = false
                }
            }
        }
    }

    // Included Books list
    var includedBooks by remember {
        mutableStateOf(emptyList<IncludedBook>())
    }

    var showAddBookDialog by remember { mutableStateOf(false) }
    var newBookSubjectName by remember { mutableStateOf("") }
    var newBookAuthor by remember { mutableStateOf("") }
    var newBookImage1Uri by remember { mutableStateOf<String?>(null) }
    var isSavingBookImage by remember { mutableStateOf(false) }

    val bookImage1PickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isSavingBookImage = true
                try {
                    val persistentUri = ImageStorageManager.saveImageLocally(context, uri)
                    newBookImage1Uri = persistentUri
                } finally {
                    isSavingBookImage = false
                }
            }
        }
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPublished by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BgCanvas,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sell Book Set",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    ) { innerPadding ->
        if (isPublished) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Set Published Successfully! 🎉",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your complete semester book set is now available for other students to discover and negotiate.",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        GlassButton(
                            text = "Done",
                            onClick = {
                                isPublished = false
                                coverPhotoUri = null
                                totalPriceText = ""
                                description = ""
                                includedBooks = emptyList()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Intro hint card
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = PrimaryPurpleLight.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💡 Marketplace Rule: Listings are complete semester sets. Add all books included in your bundle.",
                                fontSize = 13.sp,
                                color = TextPrimary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }


                // Institute, Technology & Semester Selectors
                item {
                    val isFci = college.contains("Feni Computer", ignoreCase = true)

                    NeumorphicGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        backgroundColor = CardGlass,
                        elevation = 5.dp
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PrimaryPurpleLight,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.School,
                                        contentDescription = null,
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Campus & Academic Details",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Select your institute, technology, and semester",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 1. Institute / College Dropdown
                        GlassNeumorphicDropdownField(
                            label = "Institute / Campus",
                            selectedValue = college,
                            displayValue = { it ?: "Select College" },
                            options = CollegeDirectory.ALL_COLLEGES,
                            onSelect = { col ->
                                if (col != null) college = col
                            },
                            subtitle = { col ->
                                when {
                                    col?.contains("Feni Computer") == true -> "2 Departments: CSE, Telecom"
                                    col?.contains("Feni Poly") == true -> "6 Technologies: Civil, Electrical, Mechanical, Power, Computer, AIDT"
                                    else -> ""
                                }
                            },
                            leadingIcon = if (isFci) Icons.Outlined.Computer else Icons.Outlined.School,
                            testTag = "sell_dropdown_college"
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 2. Technology / Department Dropdown (Filtered dynamically by chosen College)
                        GlassNeumorphicDropdownField(
                            label = if (isFci) "Department" else "Technology / Department",
                            selectedValue = selectedTech,
                            displayValue = { it?.displayName ?: "Select Technology" },
                            options = availableTechsForCollege,
                            onSelect = { tech ->
                                if (tech != null) selectedTech = tech
                            },
                            placeholder = "Select Technology",
                            supportingText = "Available in this institute: ${availableTechsForCollege.size} departments",
                            leadingIcon = Icons.Outlined.Engineering,
                            testTag = "sell_dropdown_tech"
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 3. Semester Dropdown
                        GlassNeumorphicDropdownField(
                            label = "Which Semester Book?",
                            selectedValue = selectedSem,
                            displayValue = { it?.displayName ?: "Select Semester" },
                            options = Semester.entries,
                            onSelect = { sem ->
                                if (sem != null) selectedSem = sem
                            },
                            placeholder = "Select Semester",
                            leadingIcon = Icons.Outlined.CalendarMonth,
                            testTag = "sell_dropdown_sem"
                        )
                    }
                }

                // Included Books Section
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Included Books (${includedBooks.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                Text("List each book in this semester set", fontSize = 12.sp, color = TextMuted)
                            }

                            FrostedGlassButton(
                                text = "Add Book",
                                icon = Icons.Default.Add,
                                onClick = { showAddBookDialog = true },
                                height = 36.dp,
                                textColor = PrimaryPurple,
                                testTag = "btn_add_book"
                            )
                        }
                    }
                }

                if (includedBooks.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Add books to this set",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Tap the 'Add Book' button above to specify book titles and subjects.",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }

                itemsIndexed(includedBooks) { index, book ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}.",
                                fontWeight = FontWeight.Bold,
                                color = PrimaryPurple,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            // Book Photo Thumbnail
                            if (book.imageUris.isNotEmpty()) {
                                book.imageUris.firstOrNull()?.let { imgUri ->
                                    AsyncImage(
                                        model = imgUri,
                                        contentDescription = "Front cover",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = book.bookName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                val subtitle = buildString {
                                    if (book.author.isNotBlank()) append(book.author + " • ")
                                    append("Front cover")
                                }
                                Text(subtitle, fontSize = 12.sp, color = TextMuted)
                            }
                            GlassIconButton(
                                onClick = {
                                    includedBooks = includedBooks.filterIndexed { i, _ -> i != index }
                                },
                                icon = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tone = GlassIconTone.DANGER,
                                size = 36.dp,
                                testTag = "delete_book_$index"
                            )
                        }
                    }
                }

                // Price and Condition
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Total Set Price (Tk)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        Text("Single price for the entire book set", fontSize = 12.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = totalPriceText,
                            onValueChange = { totalPriceText = it },
                            placeholder = { Text("e.g. 500") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_total_price"),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Smart Quick Price Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Quick:", fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                            listOf("450", "500", "550", "600").forEach { quickPrice ->
                                LiquidGlassChip(
                                    text = "Tk $quickPrice",
                                    selected = totalPriceText == quickPrice,
                                    onClick = { totalPriceText = quickPrice }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💡 PolyTip: Complete ${selectedSem.displayName} sets sell 3x faster when priced between Tk 400 - Tk 600",
                                    fontSize = 11.sp,
                                    color = Color(0xFF1E40AF)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Overall Set Condition", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SetCondition.entries.forEach { cond ->
                                val isSel = overallCondition == cond
                                LiquidGlassChip(
                                    text = cond.label,
                                    selected = isSel,
                                    onClick = { overallCondition = cond }
                                )
                            }
                        }
                    }
                }

                // Description
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Description", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text("Mention condition details, notes, highlighting, and availability...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .testTag("input_description"),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                // Cover Photo preview
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Cover Photo", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                Text("Main display cover for this book set", fontSize = 12.sp, color = TextMuted)
                            }
                            if (coverPhotoUri != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Cover Added", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF047857))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        if (coverPhotoUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(190.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFE2E8F0))
                            ) {
                                AsyncImage(
                                    model = coverPhotoUri,
                                    contentDescription = "Cover Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "★ Main Cover",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.White.copy(alpha = 0.92f),
                                        shadowElevation = 2.dp,
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.AddPhotoAlternate,
                                                contentDescription = "Change Cover Photo",
                                                tint = PrimaryPurple,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Change",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = PrimaryPurple
                                            )
                                        }
                                    }

                                    Surface(
                                        onClick = { coverPhotoUri = null },
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.65f),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove Cover Photo",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Surface(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = PrimaryPurpleLight.copy(alpha = 0.35f),
                                border = BorderStroke(1.5.dp, PrimaryPurple.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .testTag("btn_add_cover_photo")
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AddPhotoAlternate,
                                        contentDescription = "Upload Cover Photo",
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Upload Cover Photo",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = PrimaryPurple
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Single image • Serves as main display cover",
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }

                        if (isSavingPhoto) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = PrimaryPurple
                                )
                                Text("Saving cover photo...", fontSize = 12.sp, color = PrimaryPurple)
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    item {
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFEF4444),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Publish Button
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    GlassButton(
                        text = "Publish Book Set",
                        onClick = {
                            val price = totalPriceText.toIntOrNull()
                            if (includedBooks.isEmpty()) {
                                errorMessage = "Please include at least one book in this set."
                            } else if (price == null || price <= 0) {
                                errorMessage = "Please enter a valid price (Tk)."
                            } else {
                                errorMessage = null
                                val autoTitle = "${selectedTech.displayName} ${selectedSem.shortName} Semester Set (${includedBooks.size} Books)"
                                val autoSubject = selectedTech.displayName
                                val newSet = MarketplaceRepository.publishSet(
                                    title = autoTitle,
                                    subject = autoSubject,
                                    technology = selectedTech,
                                    semester = selectedSem,
                                    college = college,
                                    totalPrice = price,
                                    overallCondition = overallCondition,
                                    description = description.ifBlank { "Complete ${selectedSem.displayName} set for ${selectedTech.displayName} in ${overallCondition.label.lowercase()} condition." },
                                    includedBooks = includedBooks,
                                    imageUrls = coverPhotoUri?.let { listOf(it) } ?: emptyList()
                                )
                                isPublished = true
                                onPublishSuccess(newSet)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_publish_set")
                    )
                }
            }
        }
    }

    // Add Book Dialog
    if (showAddBookDialog) {
        var dialogError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                showAddBookDialog = false
                newBookSubjectName = ""
                newBookAuthor = ""
                newBookImage1Uri = null
                dialogError = null
            },
            title = {
                Text(
                    text = "Add Included Book",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newBookSubjectName,
                        onValueChange = { 
                            newBookSubjectName = it
                            dialogError = null
                        },
                        label = { Text("Subject Name *") },
                        placeholder = { Text("e.g. Mathematics-IV / Programming in C") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_input_subject_name")
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Book Photo (Mandatory)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Front cover",
                        fontSize = 11.sp,
                        color = TextMuted
                    )

                    // Single Mandatory Image Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (newBookImage1Uri != null) Color(0xFFF8FAFC) else PrimaryPurpleLight.copy(alpha = 0.25f))
                            .border(
                                1.5.dp,
                                if (dialogError != null && newBookImage1Uri == null) Color(0xFFEF4444) else if (newBookImage1Uri != null) Color(0xFFCBD5E1) else PrimaryPurple.copy(alpha = 0.4f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                dialogError = null
                                bookImage1PickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (newBookImage1Uri != null) {
                            AsyncImage(
                                model = newBookImage1Uri,
                                contentDescription = "Front cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { newBookImage1Uri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(26.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Upload Front Cover",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "Single photo required",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    if (isSavingBookImage) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = PrimaryPurple
                            )
                            Text("Compressing photo...", fontSize = 11.sp, color = PrimaryPurple)
                        }
                    }

                    if (dialogError != null) {
                        Text(
                            text = dialogError!!,
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                val isFormValid = newBookSubjectName.isNotBlank() && !newBookImage1Uri.isNullOrBlank()
                GlassButton(
                    text = "Add Book",
                    onClick = {
                        if (newBookSubjectName.isBlank()) {
                            dialogError = "Subject name is required."
                            return@GlassButton
                        }
                        if (newBookImage1Uri.isNullOrBlank()) {
                            dialogError = "Front cover photo is mandatory for each subject."
                            return@GlassButton
                        }
                        val added = IncludedBook(
                            id = "b_" + (includedBooks.size + 1) + "_" + System.currentTimeMillis(),
                            setId = "",
                            bookName = newBookSubjectName.trim(),
                            subject = newBookSubjectName.trim(),
                            author = newBookAuthor.trim(),
                            imageUris = listOf(newBookImage1Uri!!)
                        )
                        includedBooks = includedBooks + added
                        newBookSubjectName = ""
                        newBookAuthor = ""
                        newBookImage1Uri = null
                        dialogError = null
                        showAddBookDialog = false
                    },
                    enabled = isFormValid,
                    height = 42.dp,
                    testTag = "btn_dialog_add_book"
                )
            },
            dismissButton = {
                FrostedGlassButton(
                    text = "Cancel",
                    onClick = {
                        showAddBookDialog = false
                        newBookSubjectName = ""
                        newBookAuthor = ""
                        newBookImage1Uri = null
                        dialogError = null
                    },
                    height = 42.dp,
                    textColor = TextSecondary,
                    testTag = "btn_dialog_cancel_book"
                )
            }
        )
    }
}
