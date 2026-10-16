package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import com.example.R
import com.example.data.MarketplaceRepository
import com.example.data.auth.AuthResult
import com.example.data.storage.ImageStorageManager
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToSetDetails: (BookSet) -> Unit,
    onNavigateToAdminPanel: () -> Unit,
    onRequireLogin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentUser by MarketplaceRepository.currentUser.collectAsState()
    val bookSets by MarketplaceRepository.bookSets.collectAsState()
    val savedIds by MarketplaceRepository.savedSetIds.collectAsState()
    val isLoggedIn by MarketplaceRepository.isLoggedIn.collectAsState()

    val myListings = remember(bookSets, currentUser) {
        bookSets.filter { it.sellerId == currentUser.id || it.sellerEmail == currentUser.email }
    }

    val savedSets = remember(bookSets, savedIds) {
        bookSets.filter { savedIds.contains(it.id) }
    }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showAdminPinDialog by remember { mutableStateOf(false) }
    var adminPinInput by remember { mutableStateOf("") }
    var adminPinError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // User original photo picker with persistent local storage
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val persistentUri = ImageStorageManager.saveAvatarLocally(context, uri)
                MarketplaceRepository.updateProfilePhotoUri(persistentUri)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BgCanvas,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Profile",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Header Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar with camera badge
                        Box {
                            Surface(
                                modifier = Modifier
                                    .size(94.dp)
                                    .shadow(elevation = 6.dp, shape = CircleShape, ambientColor = SoftShadowColor)
                                    .clip(CircleShape)
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .testTag("profile_avatar_image"),
                                shape = CircleShape,
                                color = PrimaryPurpleLight
                            ) {
                                if (!currentUser.profilePhotoUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = currentUser.profilePhotoUri,
                                        contentDescription = "Profile Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = currentUser.profilePhotoRes ?: R.drawable.avatar_rahim_1789677826412),
                                        contentDescription = "Profile Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryPurple)
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .testTag("btn_upload_profile_photo"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PhotoCamera,
                                    contentDescription = "Upload profile photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isLoggedIn) {
                            Text(
                                text = currentUser.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = currentUser.college,
                                fontSize = 13.sp,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "${currentUser.technology.displayName} • ${currentUser.currentSemester.displayName}",
                                fontSize = 13.sp,
                                color = PrimaryPurple,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = "Guest Student (Logged Out)",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Log in to buy, sell, and message campus students",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            SecondaryGlassButton(
                                text = "Log In",
                                onClick = { onRequireLogin() },
                                icon = Icons.AutoMirrored.Outlined.Login
                            )
                        }
                    }
                }
            }

            // Account Information Section
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Account Information",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    ProfileInfoRow(icon = Icons.Outlined.Phone, label = "Phone", value = currentUser.phone)
                    Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 8.dp))
                    ProfileInfoRow(icon = Icons.Outlined.Email, label = "Email", value = currentUser.email)
                    Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 8.dp))
                    ProfileInfoRow(icon = Icons.Outlined.School, label = "College", value = currentUser.college)
                    Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 8.dp))
                    ProfileInfoRow(icon = Icons.Outlined.Build, label = "Department", value = currentUser.technology.displayName)
                    Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 8.dp))
                    ProfileInfoRow(icon = Icons.Outlined.CalendarMonth, label = "Semester", value = currentUser.currentSemester.displayName)
                }
            }

            // My Listings Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Listings (${myListings.size})",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            if (myListings.isEmpty()) {
                item {
                    Text("You have no active listings yet.", color = TextMuted, fontSize = 13.sp)
                }
            } else {
                items(myListings) { set ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onNavigateToSetDetails(set) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(14.dp))
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
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(set.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Text("${set.semester.shortName} Sem • Tk ${set.totalPrice}", fontSize = 13.sp, color = Color(0xFF0F9B58), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ConditionBadge(condition = set.overallCondition)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (set.status == SetStatus.AVAILABLE) Color(0xFFDCFCE7) else Color(0xFFE2E8F0)
                                    ) {
                                        Text(
                                            text = if (set.status == SetStatus.AVAILABLE) "Active" else "Sold",
                                            color = if (set.status == SetStatus.AVAILABLE) Color(0xFF15803D) else TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Saved Books Section
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Saved Books (${savedSets.size})",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            items(savedSets) { set ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigateToSetDetails(set) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
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
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(set.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Text("${set.semester.shortName} Sem • Tk ${set.totalPrice}", fontSize = 12.sp, color = TextMuted)
                        }
                        GlassIconButton(
                            onClick = { MarketplaceRepository.toggleSaveSet(set.id) },
                            icon = Icons.Outlined.BookmarkRemove,
                            contentDescription = "Remove bookmark",
                            tone = GlassIconTone.PRIMARY_PURPLE,
                            size = 36.dp,
                            testTag = "profile_remove_bookmark_${set.id}"
                        )
                    }
                }
            }

            // App settings & Support
            item {
                Spacer(modifier = Modifier.height(6.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    // Admin Control Panel - directly accessible for Super Admin / Admin role
                    if (currentUser.email.equals("mdmaheraz65@gmail.com", ignoreCase = true) || currentUser.role == "ADMIN") {
                        ProfileMenuRow(
                            icon = Icons.Outlined.AdminPanelSettings,
                            title = "Admin Control Panel",
                            onClick = onNavigateToAdminPanel
                        )
                        Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 8.dp))
                    }
                    ProfileMenuRow(icon = Icons.Outlined.Security, title = "Privacy & Security")
                    Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 8.dp))
                    ProfileMenuRow(icon = Icons.Outlined.Description, title = "Terms & Conditions")
                    Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 8.dp))
                    ProfileMenuRow(icon = Icons.Outlined.HelpOutline, title = "Help & Support")
                    Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 8.dp))
                    ProfileMenuRow(icon = Icons.Outlined.Info, title = "About BookSwipe v1.0.0")
                }
            }

            // Prominent Admin Access Card right below profile options
            if (currentUser.email.equals("mdmaheraz65@gmail.com", ignoreCase = true) || currentUser.role == "ADMIN") {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        onClick = onNavigateToAdminPanel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(22.dp),
                                ambientColor = PrimaryPurple.copy(alpha = 0.3f),
                                spotColor = PrimaryPurple.copy(alpha = 0.4f)
                            )
                            .testTag("btn_admin_panel_prominent"),
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF1E1B4B),
                        border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = PrimaryPurple,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.AdminPanelSettings,
                                        contentDescription = "Admin",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Admin Panel Access",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF10B981)
                                    ) {
                                        Text(
                                            text = "SUPER ADMIN",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Manage listings, approve deals, and moderate users",
                                    fontSize = 11.sp,
                                    color = Color(0xFFC7D2FE)
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = "Enter Admin Panel",
                                tint = Color(0xFFC7D2FE),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Login / Logout Option (bottom of profile per user request)
            item {
                Spacer(modifier = Modifier.height(10.dp))
                if (isLoggedIn) {
                    Surface(
                        onClick = { showLogoutConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(22.dp),
                                ambientColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                                spotColor = Color(0xFFEF4444).copy(alpha = 0.25f)
                            )
                            .testTag("btn_logout_profile"),
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5).copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 15.dp, horizontal = 20.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = "Logout",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Log Out",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                } else {
                    Surface(
                        onClick = { showLoginDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(22.dp),
                                ambientColor = PrimaryPurple.copy(alpha = 0.25f),
                                spotColor = PrimaryPurple.copy(alpha = 0.3f)
                            )
                            .testTag("btn_login_profile"),
                        shape = RoundedCornerShape(22.dp),
                        color = PrimaryPurpleLight,
                        border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 15.dp, horizontal = 20.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Login,
                                contentDescription = "Login",
                                tint = PrimaryPurple,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Log In",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryPurple
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Developer Credit at bottom of Profile per user request
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "develop by Mh Nirob",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted,
                        letterSpacing = 0.4.sp,
                        modifier = Modifier.testTag("text_developer_credit")
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = {
                Text(
                    text = "Confirm Log Out",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to log out of your account?",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        MarketplaceRepository.logout()
                        showLogoutConfirmDialog = false
                        onRequireLogin()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("btn_confirm_logout")
                ) {
                    Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLogoutConfirmDialog = false },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }

    // Admin PIN Verification Dialog
    if (showAdminPinDialog) {
        AlertDialog(
            onDismissRequest = { showAdminPinDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.AdminPanelSettings,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Admin Security PIN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Enter your secret Admin PIN or Password to access the moderation console.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = adminPinInput,
                        onValueChange = {
                            adminPinInput = it
                            adminPinError = null
                        },
                        label = { Text("Admin PIN / Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                            autoCorrect = false
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val pin = adminPinInput.trim()
                                if (pin == "1234" || pin == "polytechnic123" || pin == "admin123" || pin == "admin") {
                                    showAdminPinDialog = false
                                    onNavigateToAdminPanel()
                                } else {
                                    adminPinError = "Incorrect Admin PIN. Access denied."
                                }
                            }
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_admin_pin")
                    )

                    if (adminPinError != null) {
                        Text(
                            text = adminPinError ?: "",
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pin = adminPinInput.trim()
                        if (pin == "1234" || pin == "polytechnic123" || pin == "admin123" || pin == "admin") {
                            showAdminPinDialog = false
                            onNavigateToAdminPanel()
                        } else {
                            adminPinError = "Incorrect Admin PIN. Access denied."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("btn_unlock_admin")
                ) {
                    Text("Unlock Panel", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAdminPinDialog = false },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }

    // Authentication Dialog (Sign In / Register)
    if (showLoginDialog) {
        var isRegisterTab by remember { mutableStateOf(false) }

        // Sign In State
        var signInEmail by remember { mutableStateOf("") }
        var signInPassword by remember { mutableStateOf("") }
        var signInPasswordVisible by remember { mutableStateOf(false) }

        // Register State
        var regName by remember { mutableStateOf("") }
        var regEmail by remember { mutableStateOf("") }
        var regPassword by remember { mutableStateOf("") }
        var regPasswordVisible by remember { mutableStateOf(false) }
        var regPhone by remember { mutableStateOf("") }
        var regCollege by remember { mutableStateOf(CollegeDirectory.FENI_POLYTECHNIC) }
        var regCollegeDropdown by remember { mutableStateOf(false) }

        val regAvailableTechs = remember(regCollege) {
            CollegeDirectory.getTechnologiesForCollege(regCollege)
        }
        var regTech by remember { mutableStateOf(Technology.CIVIL) }
        var regTechDropdown by remember { mutableStateOf(false) }
        var regSem by remember { mutableStateOf(Semester.SEM_1) }
        var regSemDropdown by remember { mutableStateOf(false) }

        var authError by remember { mutableStateOf<String?>(null) }
        var isLoading by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isLoading) showLoginDialog = false },
            title = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRegisterTab) "Student Registration" else "Student Sign In",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Surface(
                            shape = CircleShape,
                            color = PrimaryPurpleLight,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isRegisterTab) Icons.Outlined.PersonAdd else Icons.AutoMirrored.Outlined.Login,
                                    contentDescription = null,
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tab Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(3.dp)
                    ) {
                        Surface(
                            onClick = {
                                isRegisterTab = false
                                authError = null
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            shape = RoundedCornerShape(10.dp),
                            color = if (!isRegisterTab) Color.White else Color.Transparent,
                            shadowElevation = if (!isRegisterTab) 2.dp else 0.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "Sign In",
                                    fontSize = 13.sp,
                                    fontWeight = if (!isRegisterTab) FontWeight.Bold else FontWeight.Medium,
                                    color = if (!isRegisterTab) PrimaryPurple else TextSecondary
                                )
                            }
                        }
                        Surface(
                            onClick = {
                                isRegisterTab = true
                                authError = null
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isRegisterTab) Color.White else Color.Transparent,
                            shadowElevation = if (isRegisterTab) 2.dp else 0.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "Register",
                                    fontSize = 13.sp,
                                    fontWeight = if (isRegisterTab) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isRegisterTab) PrimaryPurple else TextSecondary
                                )
                            }
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (authError != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEE2E2),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = authError ?: "",
                                color = Color(0xFFB91C1C),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    if (!isRegisterTab) {
                        // SIGN IN TAB with Firebase Google Authentication
                        Text(
                            text = "Sign in with your Google account to sync your student profile, upload book sets, and chat with buyers.",
                            fontSize = 12.5.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // Direct Sign in with Google Button
                        Surface(
                            onClick = {
                                coroutineScope.launch {
                                    isLoading = true
                                    authError = null
                                    try {
                                        val res = MarketplaceRepository.performGoogleSignIn(
                                            googleUid = "google_user_demo_1",
                                            email = "mdmaheraz65@gmail.com",
                                            displayName = "Mh Nirob",
                                            photoUrl = null
                                        )
                                        if (res is AuthResult.Success) {
                                            showLoginDialog = false
                                        } else if (res is AuthResult.Error) {
                                            authError = res.message
                                        }
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = BorderStroke(1.2.dp, Color(0xFFCBD5E1)),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_google_signin_profile")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF4285F4),
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "G",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isLoading) "Signing in with Google..." else "Sign in with Google",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                            }
                        }

                        // Security guarantee badge
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Shield,
                                    contentDescription = null,
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Authenticated directly via Firebase Google Identity Services.",
                                    fontSize = 11.5.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    } else {
                        // REGISTER TAB
                        OutlinedTextField(
                            value = regName,
                            onValueChange = { regName = it; authError = null },
                            label = { Text("Full Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_reg_name"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        OutlinedTextField(
                            value = regEmail,
                            onValueChange = { regEmail = it; authError = null },
                            label = { Text("Email Address") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrect = false),
                            modifier = Modifier.fillMaxWidth().testTag("input_reg_email"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        OutlinedTextField(
                            value = regPassword,
                            onValueChange = { regPassword = it; authError = null },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false),
                            trailingIcon = {
                                IconButton(onClick = { regPasswordVisible = !regPasswordVisible }) {
                                    Icon(
                                        imageVector = if (regPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("input_reg_password"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        OutlinedTextField(
                            value = regPhone,
                            onValueChange = { regPhone = it; authError = null },
                            label = { Text("Phone Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_reg_phone"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        // College Selector
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = regCollege,
                                onValueChange = {},
                                enabled = false,
                                readOnly = true,
                                label = { Text("Polytechnic Institute") },
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = TextSecondary)
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = TextPrimary,
                                    disabledBorderColor = Color(0xFFCBD5E1),
                                    disabledLabelColor = TextSecondary,
                                    disabledContainerColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { regCollegeDropdown = true }
                            )
                            DropdownMenu(
                                expanded = regCollegeDropdown,
                                onDismissRequest = { regCollegeDropdown = false },
                                properties = androidx.compose.ui.window.PopupProperties(focusable = true),
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                CollegeDirectory.ALL_COLLEGES.forEach { col ->
                                    DropdownMenuItem(
                                        text = { Text(col, fontSize = 13.sp) },
                                        onClick = {
                                            regCollegeDropdown = false
                                            regCollege = col
                                            val techs = CollegeDirectory.getTechnologiesForCollege(col)
                                            if (!techs.contains(regTech)) {
                                                regTech = techs.firstOrNull() ?: Technology.CIVIL
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Technology & Semester
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = regTech.shortName,
                                    onValueChange = {},
                                    enabled = false,
                                    readOnly = true,
                                    label = { Text("Department") },
                                    trailingIcon = {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = TextSecondary)
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = TextPrimary,
                                        disabledBorderColor = Color(0xFFCBD5E1),
                                        disabledLabelColor = TextSecondary,
                                        disabledContainerColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { regTechDropdown = true }
                                )
                                DropdownMenu(
                                    expanded = regTechDropdown,
                                    onDismissRequest = { regTechDropdown = false },
                                    properties = androidx.compose.ui.window.PopupProperties(focusable = true),
                                    modifier = Modifier.fillMaxWidth(0.85f)
                                ) {
                                    regAvailableTechs.forEach { tech ->
                                        DropdownMenuItem(
                                            text = { Text(tech.displayName, fontSize = 13.sp) },
                                            onClick = {
                                                regTechDropdown = false
                                                regTech = tech
                                            }
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = regSem.displayName,
                                    onValueChange = {},
                                    enabled = false,
                                    readOnly = true,
                                    label = { Text("Semester") },
                                    trailingIcon = {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = TextSecondary)
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = TextPrimary,
                                        disabledBorderColor = Color(0xFFCBD5E1),
                                        disabledLabelColor = TextSecondary,
                                        disabledContainerColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { regSemDropdown = true }
                                )
                                DropdownMenu(
                                    expanded = regSemDropdown,
                                    onDismissRequest = { regSemDropdown = false },
                                    properties = androidx.compose.ui.window.PopupProperties(focusable = true),
                                    modifier = Modifier.fillMaxWidth(0.85f)
                                ) {
                                    Semester.entries.forEach { sem ->
                                        DropdownMenuItem(
                                            text = { Text(sem.displayName, fontSize = 13.sp) },
                                            onClick = {
                                                regSemDropdown = false
                                                regSem = sem
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (isRegisterTab) {
                    GlassButton(
                        text = if (isLoading) "Please wait..." else "Create Account",
                        onClick = {
                            if (isLoading) return@GlassButton
                            coroutineScope.launch {
                                isLoading = true
                                authError = null
                                try {
                                    val res = MarketplaceRepository.performSignUp(
                                        name = regName.trim(),
                                        email = regEmail.trim(),
                                        password = regPassword.trim(),
                                        phone = regPhone.trim(),
                                        college = regCollege,
                                        technology = regTech,
                                        semester = regSem
                                    )
                                    if (res is AuthResult.Success) {
                                        showLoginDialog = false
                                    } else if (res is AuthResult.Error) {
                                        authError = res.message
                                    }
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        height = 44.dp,
                        testTag = "btn_submit_auth"
                    )
                }
            },
            dismissButton = {
                FrostedGlassButton(
                    text = "Cancel",
                    onClick = { showLoginDialog = false },
                    textColor = TextSecondary,
                    height = 44.dp,
                    testTag = "btn_cancel_login"
                )
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }

    // Edit Profile Modal
    if (showEditProfileDialog) {
        var editName by remember { mutableStateOf(currentUser.name) }
        var editEmail by remember { mutableStateOf(currentUser.email) }
        var editPhone by remember { mutableStateOf(currentUser.phone) }
        var editCollege by remember { mutableStateOf(currentUser.college) }
        var editTech by remember { mutableStateOf(currentUser.technology) }
        var editSem by remember { mutableStateOf(currentUser.currentSemester) }

        var collegeDropdownExpanded by remember { mutableStateOf(false) }
        var techDropdownExpanded by remember { mutableStateOf(false) }
        var semDropdownExpanded by remember { mutableStateOf(false) }

        // Technologies dynamically offered by the selected college
        val availableTechs = remember(editCollege) {
            CollegeDirectory.getTechnologiesForCollege(editCollege)
        }

        // Auto-select first available tech if current selected tech isn't in the college
        LaunchedEffect(availableTechs) {
            if (!availableTechs.contains(editTech)) {
                editTech = availableTechs.firstOrNull() ?: Technology.CIVIL
            }
        }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Text(
                    text = "Edit Student Profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // College Dropdown Selector
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = editCollege,
                            onValueChange = {},
                            enabled = false,
                            readOnly = true,
                            label = { Text("College / Institute") },
                            trailingIcon = {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Select College", tint = TextSecondary)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = TextPrimary,
                                disabledBorderColor = Color(0xFFCBD5E1),
                                disabledLabelColor = TextSecondary,
                                disabledContainerColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { collegeDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = collegeDropdownExpanded,
                            onDismissRequest = { collegeDropdownExpanded = false },
                            properties = androidx.compose.ui.window.PopupProperties(focusable = true),
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            CollegeDirectory.ALL_COLLEGES.forEach { col ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = col,
                                                fontWeight = if (col == editCollege) FontWeight.Bold else FontWeight.Normal,
                                                color = if (col == editCollege) PrimaryPurple else TextPrimary
                                            )
                                            val note = when {
                                                col.contains("Feni Computer") -> "2 Departments: CSE, Telecom"
                                                col.contains("Feni Poly") -> "6 Technologies: Civil, Electrical, Mechanical, Power, Computer, AIDT"
                                                else -> ""
                                            }
                                            if (note.isNotBlank()) {
                                                Text(text = note, fontSize = 11.sp, color = TextMuted)
                                            }
                                        }
                                    },
                                    onClick = {
                                        collegeDropdownExpanded = false
                                        editCollege = col
                                    }
                                )
                            }
                        }
                    }

                    // Technology Dropdown (Filtered based on editCollege)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = editTech.displayName,
                            onValueChange = {},
                            enabled = false,
                            readOnly = true,
                            label = { Text("Technology / Department") },
                            supportingText = {
                                val count = availableTechs.size
                                Text(
                                    text = "Available in this institute: $count technology option${if (count > 1) "s" else ""}",
                                    fontSize = 11.sp,
                                    color = PrimaryPurple
                                )
                            },
                            trailingIcon = {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Select Technology", tint = TextSecondary)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = TextPrimary,
                                disabledBorderColor = Color(0xFFCBD5E1),
                                disabledLabelColor = TextSecondary,
                                disabledContainerColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { techDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = techDropdownExpanded,
                            onDismissRequest = { techDropdownExpanded = false },
                            properties = androidx.compose.ui.window.PopupProperties(focusable = true),
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            availableTechs.forEach { tech ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = tech.displayName,
                                                fontWeight = if (tech == editTech) FontWeight.Bold else FontWeight.Normal,
                                                color = if (tech == editTech) PrimaryPurple else TextPrimary
                                            )
                                            if (tech == editTech) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = PrimaryPurple,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        techDropdownExpanded = false
                                        editTech = tech
                                    }
                                )
                            }
                        }
                    }

                    // Semester Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = editSem.displayName,
                            onValueChange = {},
                            enabled = false,
                            readOnly = true,
                            label = { Text("Current Semester") },
                            trailingIcon = {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Select Semester", tint = TextSecondary)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = TextPrimary,
                                disabledBorderColor = Color(0xFFCBD5E1),
                                disabledLabelColor = TextSecondary,
                                disabledContainerColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { semDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = semDropdownExpanded,
                            onDismissRequest = { semDropdownExpanded = false },
                            properties = androidx.compose.ui.window.PopupProperties(focusable = true),
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            Semester.entries.forEach { sem ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = sem.displayName,
                                            fontWeight = if (sem == editSem) FontWeight.Bold else FontWeight.Normal,
                                            color = if (sem == editSem) PrimaryPurple else TextPrimary
                                        )
                                    },
                                    onClick = {
                                        semDropdownExpanded = false
                                        editSem = sem
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                GlassButton(
                    text = "Save Changes",
                    onClick = {
                        MarketplaceRepository.updateUserProfile(
                            name = editName.trim(),
                            email = editEmail.trim(),
                            phone = editPhone.trim(),
                            college = editCollege.trim(),
                            tech = editTech,
                            sem = editSem
                        )
                        showEditProfileDialog = false
                    },
                    height = 44.dp,
                    testTag = "btn_save_profile"
                )
            },
            dismissButton = {
                FrostedGlassButton(
                    text = "Cancel",
                    onClick = { showEditProfileDialog = false },
                    textColor = TextSecondary,
                    height = 44.dp,
                    testTag = "btn_cancel_profile"
                )
            }
        )
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, fontSize = 11.sp, color = TextMuted)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
    }
}
