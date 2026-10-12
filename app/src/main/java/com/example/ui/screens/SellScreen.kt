package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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

    var setName by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
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
    var selectedPhotoUris by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSavingPhoto by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isSavingPhoto = true
                try {
                    val persistentUri = ImageStorageManager.saveImageLocally(context, uri)
                    selectedPhotoUris = selectedPhotoUris + persistentUri
                } finally {
                    isSavingPhoto = false
                }
            }
        }
    }

    // Included Books list (Real clean list)
    var includedBooks by remember {
        mutableStateOf(emptyList<IncludedBook>())
    }

    var showAddBookDialog by remember { mutableStateOf(false) }
    var newBookTitle by remember { mutableStateOf("") }
    var newBookSubject by remember { mutableStateOf("") }
    var newBookAuthor by remember { mutableStateOf("") }

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
                            onClick = { isPublished = false },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
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

                // Set Basic Info
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Set Information", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = setName,
                            onValueChange = { setName = it },
                            label = { Text("Set Name / Title") },
                            placeholder = { Text("e.g. Civil Technology 4th Sem Set") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_set_name"),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text("Primary Subject / Department") },
                            placeholder = { Text("e.g. Civil Engineering") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )
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
                            label = "Semester",
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
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(book.bookName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                                Text("${book.subject}${if (book.author.isNotBlank()) " • " + book.author else ""}", fontSize = 12.sp, color = TextMuted)
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
                            listOf("350", "450", "550", "650", "800").forEach { quickPrice ->
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

                // Photo preview
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Set Photos", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                            Text(
                                text = "${selectedPhotoUris.size}/4 Photos",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Selected photos
                            itemsIndexed(selectedPhotoUris) { index, photoUri ->
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFFE2E8F0))
                                ) {
                                    AsyncImage(
                                        model = photoUri,
                                        contentDescription = "Photo ${index + 1}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // Remove photo button
                                    Surface(
                                        onClick = {
                                            selectedPhotoUris = selectedPhotoUris.filterIndexed { i, _ -> i != index }
                                        },
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(22.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove photo",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Add Photo button (if less than 4)
                            if (selectedPhotoUris.size < 4) {
                                item {
                                    GlassIconButton(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        icon = Icons.Outlined.AddPhotoAlternate,
                                        contentDescription = "Add photo",
                                        tone = GlassIconTone.PRIMARY_PURPLE,
                                        size = 80.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        testTag = "btn_add_photo"
                                    )
                                }
                            }
                        }

                        if (isSavingPhoto) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Saving photo to app storage...", fontSize = 12.sp, color = PrimaryPurple)
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
                            if (setName.isBlank()) {
                                errorMessage = "Please enter a title for the book set."
                            } else if (includedBooks.isEmpty()) {
                                errorMessage = "Please include at least one book in this set."
                            } else if (price == null || price <= 0) {
                                errorMessage = "Please enter a valid price (Tk)."
                            } else {
                                errorMessage = null
                                val newSet = MarketplaceRepository.publishSet(
                                    title = setName.trim(),
                                    subject = subject.ifBlank { "Core Engineering" },
                                    technology = selectedTech,
                                    semester = selectedSem,
                                    college = college,
                                    totalPrice = price,
                                    overallCondition = overallCondition,
                                    description = description.ifBlank { "Complete semester book set in ${overallCondition.label.lowercase()} condition." },
                                    includedBooks = includedBooks,
                                    imageUrls = selectedPhotoUris
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
        AlertDialog(
            onDismissRequest = { showAddBookDialog = false },
            title = { Text("Add Included Book") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newBookTitle,
                        onValueChange = { newBookTitle = it },
                        label = { Text("Book Title") },
                        placeholder = { Text("e.g. Mathematics-IV") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newBookSubject,
                        onValueChange = { newBookSubject = it },
                        label = { Text("Subject") },
                        placeholder = { Text("e.g. Applied Math") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newBookAuthor,
                        onValueChange = { newBookAuthor = it },
                        label = { Text("Author (Optional)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                GlassButton(
                    text = "Add",
                    onClick = {
                        if (newBookTitle.isNotBlank()) {
                            val added = IncludedBook(
                                id = "b_" + (includedBooks.size + 1),
                                setId = "",
                                bookName = newBookTitle.trim(),
                                subject = newBookSubject.ifBlank { "Technical Subject" },
                                author = newBookAuthor.trim()
                            )
                            includedBooks = includedBooks + added
                            newBookTitle = ""
                            newBookSubject = ""
                            newBookAuthor = ""
                            showAddBookDialog = false
                        }
                    },
                    height = 42.dp,
                    testTag = "btn_dialog_add_book"
                )
            },
            dismissButton = {
                FrostedGlassButton(
                    text = "Cancel",
                    onClick = { showAddBookDialog = false },
                    height = 42.dp,
                    textColor = TextSecondary,
                    testTag = "btn_dialog_cancel_book"
                )
            }
        )
    }
}
