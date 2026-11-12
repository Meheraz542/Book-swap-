package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.model.primaryCoverUrl
import com.example.model.sanitizeMediaUrl
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.data.MarketplaceRepository
import com.example.model.BookSet
import com.example.model.SetStatus
import com.example.model.UserProfile
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun SetDetailsScreen(
    bookSet: BookSet,
    onNavigateBack: () -> Unit,
    onMessageSeller: (BookSet) -> Unit,
    onMarkAsSold: (BookSet) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by MarketplaceRepository.currentUser.collectAsState()
    val allUsers by MarketplaceRepository.allUsers.collectAsState()
    val allBookSets by MarketplaceRepository.bookSets.collectAsState()
    val repoBookSet = allBookSets.find { it.id == bookSet.id } ?: bookSet

    // Direct Real-Time Snapshot Listener for this specific book set
    var directLiveSet by remember { mutableStateOf<BookSet?>(null) }
    DisposableEffect(bookSet.id) {
        val reg = MarketplaceRepository.firestoreManager?.listenSingleBookSet(bookSet.id) { updatedRemote ->
            if (updatedRemote != null) {
                directLiveSet = updatedRemote
            }
        }
        onDispose {
            reg?.remove()
        }
    }
    val currentSet = directLiveSet ?: repoBookSet

    val sellerUser = (if (currentUser.id == currentSet.sellerId) currentUser else null)
        ?: allUsers.find { it.id == currentSet.sellerId }
        ?: MarketplaceRepository.getCachedUserProfile(currentSet.sellerId)

    var fetchedSellerUser by remember { mutableStateOf<UserProfile?>(null) }
    LaunchedEffect(currentSet.sellerId) {
        if (sellerUser == null && currentSet.sellerId.isNotBlank()) {
            fetchedSellerUser = MarketplaceRepository.getUserProfileAsync(currentSet.sellerId)
        }
    }
    val effectiveSeller = sellerUser ?: fetchedSellerUser
    val dynamicSellerName = effectiveSeller?.name?.takeIf { it.isNotBlank() } ?: currentSet.sellerName
    val dynamicSellerCollege = effectiveSeller?.college?.takeIf { it.isNotBlank() } ?: currentSet.sellerCollege
    val dynamicSellerTech = effectiveSeller?.technology?.displayName?.takeIf { it.isNotBlank() } ?: currentSet.sellerTech
    val dynamicSellerSem = effectiveSeller?.currentSemester?.displayName?.takeIf { it.isNotBlank() } ?: currentSet.sellerSem
    val dynamicSellerPhone = effectiveSeller?.phone?.takeIf { it.isNotBlank() } ?: currentSet.sellerPhone
    val dynamicSellerAvatarUri = (effectiveSeller?.profilePhotoUri ?: currentSet.sellerAvatarUri)?.sanitizeMediaUrl()
    val isOwner = currentUser.id == currentSet.sellerId || (currentSet.sellerEmail.isNotBlank() && currentUser.email.equals(currentSet.sellerEmail, ignoreCase = true))
    val isAdmin = currentUser.role == "ADMIN" || currentUser.email.equals("mdmaheraz65@gmail.com", ignoreCase = true)
    val canDelete = isOwner || isAdmin

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showMarkAsSoldConfirmDialog by remember { mutableStateOf(false) }

    var isUploadingPhotos by remember { mutableStateOf(false) }
    var optimisticPhotos by remember { mutableStateOf<List<String>>(emptyList()) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            isUploadingPhotos = true
            // Immediately render newly picked photos in real time
            val stringUris = uris.map { it.toString() }
            optimisticPhotos = (optimisticPhotos + stringUris).distinct()
            MarketplaceRepository.uploadPhotosForBook(context, currentSet.id, uris) {
                isUploadingPhotos = false
            }
        }
    }

    // Clear optimistic photos once remote Firestore URLs have propagated
    LaunchedEffect(currentSet.imageUrls) {
        if (currentSet.imageUrls.any { it.startsWith("http") }) {
            optimisticPhotos = emptyList()
        }
    }

    // Aggregates all uploaded photos: primary cover, set image URLs, individual included book cover photos, and live uploads
    val allUploadedPhotos = remember(currentSet, optimisticPhotos) {
        val list = mutableListOf<String>()

        fun isValidPhoto(uri: String): Boolean {
            if (uri.startsWith("http://") || uri.startsWith("https://") || uri.startsWith("data:image/")) {
                return true
            }
            if (uri.startsWith("file:/")) {
                return try {
                    val path = uri.removePrefix("file://").removePrefix("file:")
                    val file = java.io.File(path)
                    file.exists() && file.length() > 0
                } catch (_: Exception) {
                    false
                }
            }
            return false
        }

        // 1. Primary main cover photo at index 0 if available
        currentSet.primaryCoverUrl?.takeIf { it.isNotBlank() }?.let { primary ->
            val clean = primary.sanitizeMediaUrl() ?: primary
            if (clean.isNotBlank() && isValidPhoto(clean)) list.add(clean)
        }

        // 2. Explicit set cover / image URLs
        currentSet.imageUrls.filter { it.isNotBlank() }.forEach { rawUri ->
            val clean = rawUri.sanitizeMediaUrl() ?: rawUri
            if (clean.isNotBlank() && clean !in list && isValidPhoto(clean)) list.add(clean)
        }

        // 3. Individual included book front cover photos
        currentSet.includedBooks.forEach { book ->
            book.imageUris.filter { it.isNotBlank() }.forEach { rawUri ->
                val clean = rawUri.sanitizeMediaUrl() ?: rawUri
                if (clean.isNotBlank() && clean !in list && isValidPhoto(clean)) list.add(clean)
            }
        }

        // 4. Optimistic real-time uploads
        optimisticPhotos.filter { it.isNotBlank() }.forEach { rawUri ->
            val clean = rawUri.sanitizeMediaUrl() ?: rawUri
            if (clean.isNotBlank() && clean !in list && isValidPhoto(clean)) list.add(clean)
        }
        list
    }
    var selectedPhotoIndex by remember(allUploadedPhotos) { mutableIntStateOf(0) }
    var viewingPhotoIndex by remember(allUploadedPhotos) { mutableIntStateOf(0) }
    var viewingPhotoFullscreen by remember { mutableStateOf<String?>(null) }

    // Increment actual book view interaction in telemetry
    LaunchedEffect(currentSet.id) {
        MarketplaceRepository.recordBookCheck()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BgCanvas,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                GlassIconButton(
                    onClick = onNavigateBack,
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tone = GlassIconTone.FROSTED_WHITE,
                    size = 44.dp,
                    testTag = "btn_details_back"
                )

                // Role-Based Access Control: Delete Listing for Creator & Admin; Save Book for third-party viewers
                if (canDelete) {
                    GlassIconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        icon = Icons.Outlined.Delete,
                        contentDescription = "Delete Listing",
                        tone = GlassIconTone.DANGER,
                        size = 44.dp,
                        testTag = "btn_details_delete"
                    )
                } else {
                    val savedIds by MarketplaceRepository.savedSetIds.collectAsState()
                    val isSaved = savedIds.contains(currentSet.id)
                    GlassIconButton(
                        onClick = { MarketplaceRepository.toggleSaveSet(currentSet.id) },
                        icon = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isSaved) "Remove from Saved" else "Save Book",
                        tone = if (isSaved) GlassIconTone.PRIMARY_PURPLE else GlassIconTone.FROSTED_WHITE,
                        size = 44.dp,
                        testTag = "btn_details_save"
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                color = Color.White.copy(alpha = 0.95f),
                shadowElevation = 16.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    if (currentSet.status == SetStatus.SOLD) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = "This book set has been SOLD",
                                color = TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(14.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else if (isOwner) {
                        GlassButton(
                            text = "Mark as Sold (Physical Exchange)",
                            onClick = { showMarkAsSoldConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "btn_mark_as_sold"
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column {
                                Text("Set Price", fontSize = 11.sp, color = TextMuted)
                                Text(
                                    text = "Tk ${currentSet.totalPrice}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF059669)
                                )
                            }

                            GlassIconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${currentSet.sellerPhone}"))
                                    context.startActivity(intent)
                                },
                                icon = Icons.Outlined.Phone,
                                contentDescription = "Call Seller",
                                tone = GlassIconTone.CYAN,
                                size = 48.dp,
                                shape = RoundedCornerShape(16.dp),
                                testTag = "btn_call_seller"
                            )

                            GlassButton(
                                text = "Message Seller",
                                onClick = { onMessageSeller(currentSet) },
                                icon = Icons.Outlined.ChatBubbleOutline,
                                modifier = Modifier.weight(1f),
                                testTag = "btn_message_seller"
                            )
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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 3D Hero Book Presentation with Next/Prev Photo Navigation
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Soft background glow
                    Box(
                        modifier = Modifier
                            .size(210.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(PrimaryPurpleLight.copy(alpha = 0.65f), Color.Transparent)
                                )
                            )
                    )

                    // Hero Book Cover Presentation
                    Surface(
                        modifier = Modifier
                            .width(176.dp)
                            .height(240.dp)
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(14.dp),
                                ambientColor = DeepShadowColor,
                                spotColor = DeepShadowColor
                            )
                            .clickable {
                                if (allUploadedPhotos.isNotEmpty()) {
                                    viewingPhotoIndex = selectedPhotoIndex
                                    viewingPhotoFullscreen = allUploadedPhotos.getOrNull(selectedPhotoIndex) ?: currentSet.primaryCoverUrl
                                } else if (!currentSet.primaryCoverUrl.isNullOrBlank()) {
                                    viewingPhotoIndex = 0
                                    viewingPhotoFullscreen = currentSet.primaryCoverUrl
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White
                    ) {
                        val activePhoto = if (allUploadedPhotos.isNotEmpty()) {
                            allUploadedPhotos.getOrElse(selectedPhotoIndex) { allUploadedPhotos.first() }
                        } else {
                            currentSet.primaryCoverUrl
                        }
                        val sanitizedPhoto = activePhoto.sanitizeMediaUrl()

                        Box(modifier = Modifier.fillMaxSize()) {
                            if (!sanitizedPhoto.isNullOrBlank() && (sanitizedPhoto.startsWith("https://", ignoreCase = true) || sanitizedPhoto.startsWith("data:image/", ignoreCase = true))) {
                                SubcomposeAsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(sanitizedPhoto)
                                        .crossfade(true)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .networkCachePolicy(CachePolicy.ENABLED)
                                        .build(),
                                    contentDescription = currentSet.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    loading = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFFF1F5F9)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(32.dp),
                                                strokeWidth = 2.5.dp,
                                                color = PrimaryPurple
                                            )
                                        }
                                    },
                                    error = {
                                        // Coil's error state attribute exclusively: NO demo images shown
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFFF8FAFC)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(16.dp)
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFFEF4444).copy(alpha = 0.12f),
                                                    modifier = Modifier.size(56.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.AutoStories,
                                                            contentDescription = "Photo load failed",
                                                            tint = Color(0xFFEF4444),
                                                            modifier = Modifier.size(28.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(
                                                    text = "Failed to load book photo",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TextPrimary
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Image unavailable or network error",
                                                    fontSize = 11.sp,
                                                    color = TextMuted
                                                )
                                            }
                                        }
                                    }
                                )

                                // Tag overlay at top-left
                                Surface(
                                    shape = RoundedCornerShape(bottomEnd = 10.dp),
                                    color = Color.Black.copy(alpha = 0.65f),
                                    modifier = Modifier.align(Alignment.TopStart)
                                ) {
                                    val matchingBook = currentSet.includedBooks.find { b ->
                                        b.imageUris.any { u -> u.sanitizeMediaUrl() == sanitizedPhoto }
                                    }
                                    val heroTag = if (selectedPhotoIndex == 0) "★ Main Cover" else (matchingBook?.bookName?.let { "📖 $it" } ?: "Book Cover ${selectedPhotoIndex + 1}")
                                    Text(
                                        text = heroTag,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }

                                // Zoom indicator at bottom-left
                                Surface(
                                    shape = RoundedCornerShape(topEnd = 8.dp),
                                    color = Color.Black.copy(alpha = 0.6f),
                                    modifier = Modifier.align(Alignment.BottomStart)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = "Zoom",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Zoom", color = Color.White, fontSize = 9.sp)
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFFF8FAFC)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = PrimaryPurpleLight.copy(alpha = 0.5f),
                                            modifier = Modifier.size(64.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Outlined.AutoStories,
                                                    contentDescription = "No photo",
                                                    tint = PrimaryPurple,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "No Photo Uploaded",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextMuted
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = currentSet.title,
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Navigation Chevrons for multi-photo browsing right on the hero card
                    if (allUploadedPhotos.size > 1) {
                        if (selectedPhotoIndex > 0) {
                            IconButton(
                                onClick = {
                                    selectedPhotoIndex--
                                    viewingPhotoIndex = selectedPhotoIndex
                                },
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 24.dp)
                                    .size(38.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (selectedPhotoIndex < allUploadedPhotos.size - 1) {
                            IconButton(
                                onClick = {
                                    selectedPhotoIndex++
                                    viewingPhotoIndex = selectedPhotoIndex
                                },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 24.dp)
                                    .size(38.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Photo count badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 12.dp, bottom = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF334155).copy(alpha = 0.88f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${selectedPhotoIndex + 1}/${allUploadedPhotos.size}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // All Uploaded Book Cover Photos Gallery with Real-Time Add/Upload affordance
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (allUploadedPhotos.isNotEmpty()) "Uploaded Book Photos (${allUploadedPhotos.size})" else "Book Photos",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        if (isOwner) {
                            TextButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled = !isUploadingPhotos,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                if (isUploadingPhotos) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = PrimaryPurple
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Uploading...", fontSize = 12.sp, color = PrimaryPurple)
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.AddPhotoAlternate,
                                        contentDescription = "Upload Photos",
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Upload Photos", fontSize = 12.sp, color = PrimaryPurple, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (allUploadedPhotos.isNotEmpty()) {
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(allUploadedPhotos) { idx, photoUri ->
                                val isSelected = idx == selectedPhotoIndex
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable {
                                        selectedPhotoIndex = idx
                                        viewingPhotoIndex = idx
                                        viewingPhotoFullscreen = photoUri.sanitizeMediaUrl()
                                    }
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .width(72.dp)
                                            .height(98.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White,
                                        border = BorderStroke(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) PrimaryPurple else Color(0xFFCBD5E1)
                                        ),
                                        shadowElevation = if (isSelected) 6.dp else 2.dp
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            SubcomposeAsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(photoUri.sanitizeMediaUrl())
                                                    .crossfade(true)
                                                    .diskCachePolicy(CachePolicy.ENABLED)
                                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                                    .networkCachePolicy(CachePolicy.ENABLED)
                                                    .build(),
                                                contentDescription = "Cover Photo ${idx + 1}",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                                loading = {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(Color(0xFFF1F5F9)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(16.dp),
                                                            strokeWidth = 1.5.dp,
                                                            color = PrimaryPurple
                                                        )
                                                    }
                                                },
                                                error = {
                                                    // Coil error state: clean neutral fallback, no demo images
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(Color(0xFFF8FAFC)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.AutoStories,
                                                            contentDescription = "Unavailable",
                                                            tint = Color(0xFF94A3B8),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            )
                                            if (idx == 0) {
                                                Surface(
                                                    shape = RoundedCornerShape(bottomEnd = 6.dp),
                                                    color = PrimaryPurple,
                                                    modifier = Modifier.align(Alignment.TopStart)
                                                ) {
                                                    Text(
                                                        text = "Cover",
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    val matchingBook = currentSet.includedBooks.find { b ->
                                        b.imageUris.any { u -> u.sanitizeMediaUrl() == photoUri.sanitizeMediaUrl() }
                                    }
                                    val photoLabel = if (idx == 0) "Main Cover" else (matchingBook?.bookName?.take(14) ?: "Photo ${idx + 1}")
                                    Text(
                                        text = photoLabel,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) PrimaryPurple else TextMuted,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isOwner) "Tap 'Upload Photos' above to add photos of all included books." else "No additional photos uploaded by seller.",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }

            // Main Info Card - Author, Edition, Book Code fields removed per requirement
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Text(
                        text = currentSet.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Subject: ${currentSet.subject}",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Badges Grid - Showing Semester and Department only
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailBadge("${currentSet.semester.displayName}")
                        DetailBadge("${currentSet.technology.displayName}")
                    }
                }
            }

            // Condition & Price Card
            item {
                val estimatedRetail = (currentSet.totalPrice * 2.2).toInt()
                val savings = estimatedRetail - currentSet.totalPrice

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Condition", fontSize = 12.sp, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            ConditionBadge(condition = currentSet.overallCondition)
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(36.dp)
                                .background(Color(0xFFE2E8F0))
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Set Price", fontSize = 12.sp, color = TextMuted)
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "Tk ${currentSet.totalPrice}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF059669)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tk $estimatedRetail",
                                    fontSize = 13.sp,
                                    color = TextMuted,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎉 Student Bundle Savings",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF065F46)
                            )
                            Text(
                                text = "Save Tk $savings (55% OFF)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                        }
                    }
                }
            }

            // Campus Safe Exchange Badge
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            color = PrimaryPurpleLight
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.VerifiedUser,
                                    contentDescription = "Safe Exchange",
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "100% Safe Campus Meetup",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Meet inside ${currentSet.sellerCollege}. Verify all books before paying.",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            // Description Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Text(
                        text = "Description",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = currentSet.description,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )
                }
            }

            // Included Books in this complete semester set (PRD Requirement)
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Included Books in Set",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${currentSet.includedBooks.size} Books",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryPurple
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "This listing is for the complete semester book set. Negotiate for the entire bundle.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            itemsIndexed(currentSet.includedBooks) { index, book ->
                val primaryBookPhoto = book.imageUris.firstOrNull { it.isNotBlank() }
                    ?: allUploadedPhotos.getOrNull(index + (if (currentSet.imageUrls.isNotEmpty()) 1 else 0))
                    ?: allUploadedPhotos.getOrNull(index)
                val sanitizedBookPhoto = primaryBookPhoto?.sanitizeMediaUrl()

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Display prominent book cover photo with book aspect ratio
                        Surface(
                            modifier = Modifier
                                .width(56.dp)
                                .height(74.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    if (!sanitizedBookPhoto.isNullOrBlank()) {
                                        val foundIdx = allUploadedPhotos.indexOfFirst { it.sanitizeMediaUrl() == sanitizedBookPhoto }
                                        if (foundIdx >= 0) {
                                            selectedPhotoIndex = foundIdx
                                            viewingPhotoIndex = foundIdx
                                        }
                                        viewingPhotoFullscreen = sanitizedBookPhoto
                                    }
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            shadowElevation = 2.dp
                        ) {
                            if (!sanitizedBookPhoto.isNullOrBlank() && (sanitizedBookPhoto.startsWith("https://", ignoreCase = true) || sanitizedBookPhoto.startsWith("data:image/", ignoreCase = true))) {
                                SubcomposeAsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(sanitizedBookPhoto)
                                        .crossfade(true)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .networkCachePolicy(CachePolicy.ENABLED)
                                        .build(),
                                    contentDescription = book.bookName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    loading = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFFF1F5F9)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 1.5.dp,
                                                color = PrimaryPurple
                                            )
                                        }
                                    },
                                    error = {
                                        // Coil error state attribute: NO demo images
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFFF8FAFC)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.AutoStories,
                                                contentDescription = "Unavailable",
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFFF8FAFC)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AutoStories,
                                        contentDescription = "No photo",
                                        tint = PrimaryPurple.copy(alpha = 0.65f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PrimaryPurpleLight.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = "Book ${index + 1}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryPurple,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (primaryBookPhoto != null) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFECFDF5),
                                        border = BorderStroke(0.5.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = "Cover Uploaded",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF047857),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = book.bookName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 2
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = book.subject,
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }

                        // Tap to enlarge / view cover action
                        IconButton(
                            onClick = {
                                if (!sanitizedBookPhoto.isNullOrBlank()) {
                                    val foundIdx = allUploadedPhotos.indexOfFirst { it.sanitizeMediaUrl() == sanitizedBookPhoto }
                                    if (foundIdx >= 0) {
                                        selectedPhotoIndex = foundIdx
                                        viewingPhotoIndex = foundIdx
                                    }
                                    viewingPhotoFullscreen = sanitizedBookPhoto
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "View Cover Photo",
                                tint = PrimaryPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Seller Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Seller Avatar with real-time dynamic propagation (No demo photos)
                        Surface(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape),
                            shape = CircleShape,
                            color = PrimaryPurpleLight
                        ) {
                            if (!dynamicSellerAvatarUri.isNullOrBlank() && (dynamicSellerAvatarUri.startsWith("https://", ignoreCase = true) || dynamicSellerAvatarUri.startsWith("data:image/", ignoreCase = true))) {
                                SubcomposeAsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(dynamicSellerAvatarUri)
                                        .crossfade(true)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .build(),
                                    contentDescription = "Seller Profile Picture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    loading = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(PrimaryPurpleLight.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = PrimaryPurple
                                            )
                                        }
                                    },
                                    error = {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = dynamicSellerName.take(1).uppercase(),
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryPurple
                                            )
                                        }
                                    }
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = dynamicSellerName.take(1).uppercase(),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryPurple
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = dynamicSellerName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dynamicSellerCollege,
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$dynamicSellerTech • $dynamicSellerSem",
                                fontSize = 12.sp,
                                color = AccentBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Direct Chat button
                        Surface(
                            onClick = { onMessageSeller(bookSet) },
                            modifier = Modifier.size(42.dp),
                            shape = CircleShape,
                            color = CardGlassSoft,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = "Chat",
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Phone call action
                        Surface(
                            onClick = {
                                val phone = dynamicSellerPhone.trim()
                                if (phone.isNotBlank()) {
                                    val cleanPhone = phone.filter { it.isDigit() || it == '+' }
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Could not open dialer: ${e.message}",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        "No phone number available for $dynamicSellerName.",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.size(42.dp),
                            shape = CircleShape,
                            color = CardGlassSoft,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Phone,
                                    contentDescription = "Call",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Popups with 'Pop' Entrance Animation
    PopAnimatedConfirmationDialog(
        isOpen = showDeleteConfirmDialog,
        onDismissRequest = { showDeleteConfirmDialog = false },
        onConfirm = {
            showDeleteConfirmDialog = false
            MarketplaceRepository.deleteSet(currentSet.id, requestingUserId = currentUser.id, requestingUserRole = currentUser.role)
            onNavigateBack()
        },
        title = "Delete Book Listing?",
        description = "Are you sure you want to delete '${currentSet.title}'? This action cannot be undone.",
        confirmButtonText = "Delete Listing",
        confirmButtonColor = Color(0xFFEF4444),
        icon = Icons.Outlined.Delete,
        iconColor = Color(0xFFEF4444),
        testTagConfirm = "btn_confirm_delete_popup"
    )

    PopAnimatedConfirmationDialog(
        isOpen = showMarkAsSoldConfirmDialog,
        onDismissRequest = { showMarkAsSoldConfirmDialog = false },
        onConfirm = {
            showMarkAsSoldConfirmDialog = false
            onMarkAsSold(currentSet)
            onNavigateBack()
        },
        title = "Mark as Sold?",
        description = "Have you completed the physical book exchange on campus with the buyer? Confirming will immediately remove this book listing from the database to conserve storage.",
        confirmButtonText = "Mark as Sold & Remove",
        confirmButtonColor = PrimaryPurple,
        icon = Icons.Outlined.CheckCircle,
        iconColor = PrimaryPurple,
        testTagConfirm = "btn_confirm_sold_popup"
    )

    // Fullscreen Photo Viewer Dialog with Photo Carousel & Multi-Photo Browsing
    if (viewingPhotoFullscreen != null) {
        Dialog(
            onDismissRequest = { viewingPhotoFullscreen = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                // Main High-Res Image with loading and fallback using Coil's loading and error state attributes exclusively
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .fillMaxHeight(0.78f)
                        .clip(RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    var scale by remember(viewingPhotoFullscreen) { mutableFloatStateOf(1f) }
                    var offsetX by remember(viewingPhotoFullscreen) { mutableFloatStateOf(0f) }
                    var offsetY by remember(viewingPhotoFullscreen) { mutableFloatStateOf(0f) }

                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(viewingPhotoFullscreen.sanitizeMediaUrl())
                            .crossfade(true)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .networkCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = "Full photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(viewingPhotoFullscreen) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 4f)
                                    if (scale > 1f) {
                                        val maxPanX = (size.width * (scale - 1)) / 2f
                                        val maxPanY = (size.height * (scale - 1)) / 2f
                                        offsetX = (offsetX + pan.x).coerceIn(-maxPanX, maxPanX)
                                        offsetY = (offsetY + pan.y).coerceIn(-maxPanY, maxPanY)
                                    } else {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            }
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX
                                translationY = offsetY
                            },
                        loading = {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    color = PrimaryPurple,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        },
                        error = {
                            // Coil's error state attribute exclusively: NO demo images shown
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AutoStories,
                                        contentDescription = "Photo unavailable",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Photo temporarily unavailable",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Image could not be loaded from remote storage",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    )
                }

                // Top Bar with Counter and Close Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (allUploadedPhotos.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Photo ${viewingPhotoIndex + 1} of ${allUploadedPhotos.size}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Close button
                    GlassIconButton(
                        onClick = { viewingPhotoFullscreen = null },
                        icon = Icons.Default.Close,
                        contentDescription = "Close photo",
                        tone = GlassIconTone.FROSTED_WHITE,
                        size = 40.dp
                    )
                }

                // Left Arrow (Previous Photo)
                if (allUploadedPhotos.size > 1 && viewingPhotoIndex > 0) {
                    IconButton(
                        onClick = {
                            viewingPhotoIndex--
                            selectedPhotoIndex = viewingPhotoIndex
                            viewingPhotoFullscreen = allUploadedPhotos[viewingPhotoIndex]
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 16.dp)
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Photo",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Right Arrow (Next Photo)
                if (allUploadedPhotos.size > 1 && viewingPhotoIndex < allUploadedPhotos.size - 1) {
                    IconButton(
                        onClick = {
                            viewingPhotoIndex++
                            selectedPhotoIndex = viewingPhotoIndex
                            viewingPhotoFullscreen = allUploadedPhotos[viewingPhotoIndex]
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Photo",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Bottom Thumbnail Quick-Selector Strip
                if (allUploadedPhotos.size > 1) {
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(bottom = 20.dp, start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(allUploadedPhotos) { idx, photoUri ->
                            val isCur = idx == viewingPhotoIndex
                            Surface(
                                modifier = Modifier
                                    .size(width = 44.dp, height = 60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewingPhotoIndex = idx
                                        selectedPhotoIndex = idx
                                        viewingPhotoFullscreen = photoUri
                                    },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    width = if (isCur) 2.5.dp else 1.dp,
                                    color = if (isCur) PrimaryPurple else Color.White.copy(alpha = 0.4f)
                                ),
                                color = Color.DarkGray
                            ) {
                                SubcomposeAsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(photoUri.sanitizeMediaUrl())
                                        .crossfade(true)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .build(),
                                    contentDescription = "Thumbnail ${idx + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    loading = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFF334155)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 1.5.dp,
                                                color = PrimaryPurple
                                            )
                                        }
                                    },
                                    error = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFF1E293B)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.AutoStories,
                                                contentDescription = "Unavailable",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun DetailBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CardGlassSoft,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
