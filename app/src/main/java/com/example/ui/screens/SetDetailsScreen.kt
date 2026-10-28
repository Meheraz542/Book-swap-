package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.MarketplaceRepository
import com.example.model.BookSet
import com.example.model.SetStatus
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
    val sellerUser = (if (currentUser.id == bookSet.sellerId) currentUser else null)
        ?: allUsers.find { it.id == bookSet.sellerId }
        ?: MarketplaceRepository.getCachedUserProfile(bookSet.sellerId)
    val sellerAvatarUri = sellerUser?.profilePhotoUri
    val sellerAvatarRes = sellerUser?.profilePhotoRes
    val isOwner = currentUser.id == bookSet.sellerId

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showMarkAsSoldConfirmDialog by remember { mutableStateOf(false) }

    val allUploadedPhotos = remember(bookSet) {
        val list = mutableListOf<String>()
        bookSet.imageUrls.filter { it.isNotBlank() }.forEach { uri ->
            if (uri !in list) list.add(uri)
        }
        bookSet.includedBooks.forEach { book ->
            book.imageUris.filter { it.isNotBlank() }.forEach { uri ->
                if (uri !in list) list.add(uri)
            }
        }
        list
    }
    var selectedPhotoIndex by remember(allUploadedPhotos) { mutableIntStateOf(0) }
    var viewingPhotoFullscreen by remember { mutableStateOf<String?>(null) }

    // Increment actual book view interaction in telemetry
    LaunchedEffect(bookSet.id) {
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

                // Single Delete Button replacing Like & Save icons
                GlassIconButton(
                    onClick = { showDeleteConfirmDialog = true },
                    icon = Icons.Outlined.Delete,
                    contentDescription = "Delete Listing",
                    tone = GlassIconTone.DANGER,
                    size = 44.dp,
                    testTag = "btn_details_delete"
                )
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
                    if (bookSet.status == SetStatus.SOLD) {
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
                                    text = "Tk ${bookSet.totalPrice}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF059669)
                                )
                            }

                            GlassIconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${bookSet.sellerPhone}"))
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
                                onClick = { onMessageSeller(bookSet) },
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
            // 3D Hero Book Presentation
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Soft background glow
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(PrimaryPurpleLight.copy(alpha = 0.6f), Color.Transparent)
                                )
                            )
                    )

                    // Book stack image
                    Surface(
                        modifier = Modifier
                            .width(170.dp)
                            .height(230.dp)
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(12.dp),
                                ambientColor = DeepShadowColor,
                                spotColor = DeepShadowColor
                            )
                            .clickable {
                                if (allUploadedPhotos.isNotEmpty()) {
                                    viewingPhotoFullscreen = allUploadedPhotos.getOrNull(selectedPhotoIndex)
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White
                    ) {
                        if (allUploadedPhotos.isNotEmpty()) {
                            val activePhoto = allUploadedPhotos.getOrElse(selectedPhotoIndex) { allUploadedPhotos.first() }
                            AsyncImage(
                                model = activePhoto,
                                contentDescription = bookSet.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (bookSet.coverDrawableRes != null) {
                            Image(
                                painter = painterResource(id = bookSet.coverDrawableRes),
                                contentDescription = bookSet.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Photo count badge
                    if (allUploadedPhotos.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 12.dp, bottom = 12.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF334155).copy(alpha = 0.85f))
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

            // All Uploaded Book Cover Photos Gallery
            if (allUploadedPhotos.size > 1) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "Uploaded Book Photos (${allUploadedPhotos.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(allUploadedPhotos) { idx, photoUri ->
                                val isSelected = idx == selectedPhotoIndex
                                Surface(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedPhotoIndex = idx },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) PrimaryPurple else Color(0xFFE2E8F0)
                                    ),
                                    shadowElevation = if (isSelected) 4.dp else 1.dp
                                ) {
                                    AsyncImage(
                                        model = photoUri,
                                        contentDescription = "Cover Photo ${idx + 1}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
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
                        text = bookSet.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Subject: ${bookSet.subject}",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Badges Grid - Showing Semester and Department only
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailBadge("${bookSet.semester.displayName}")
                        DetailBadge("${bookSet.technology.displayName}")
                    }
                }
            }

            // Condition & Price Card
            item {
                val estimatedRetail = (bookSet.totalPrice * 2.2).toInt()
                val savings = estimatedRetail - bookSet.totalPrice

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
                            ConditionBadge(condition = bookSet.overallCondition)
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
                                    text = "Tk ${bookSet.totalPrice}",
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
                                text = "Meet inside ${bookSet.sellerCollege}. Verify all books before paying.",
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
                        text = bookSet.description,
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
                            text = "${bookSet.includedBooks.size} Books",
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

            itemsIndexed(bookSet.includedBooks) { index, book ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = PrimaryPurpleLight
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = String.format("%02d", index + 1),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryPurple
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = book.bookName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = book.subject,
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }

                        if (book.imageUris.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                book.imageUris.take(2).forEach { imgUri ->
                                    AsyncImage(
                                        model = imgUri,
                                        contentDescription = "Book photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                            .clickable { viewingPhotoFullscreen = imgUri }
                                    )
                                }
                            }
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
                        // Seller Avatar with real-time dynamic propagation
                        Surface(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape),
                            shape = CircleShape,
                            color = Color(0xFFE2E8F0)
                        ) {
                            if (!sellerAvatarUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = sellerAvatarUri,
                                    contentDescription = "Seller Profile Picture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (sellerAvatarRes != null) {
                                Image(
                                    painter = painterResource(id = sellerAvatarRes),
                                    contentDescription = "Seller Profile Picture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = "Seller Avatar",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bookSet.sellerName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = bookSet.sellerCollege,
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${bookSet.sellerTech} • ${bookSet.sellerSem}",
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
                                val phone = bookSet.sellerPhone.trim()
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
                                        "No phone number available for ${bookSet.sellerName}.",
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
            MarketplaceRepository.deleteSet(bookSet.id)
            onNavigateBack()
        },
        title = "Delete Book Listing?",
        description = "Are you sure you want to delete '${bookSet.title}'? This action cannot be undone.",
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
            onMarkAsSold(bookSet)
        },
        title = "Mark as Sold?",
        description = "Have you completed the physical book exchange on campus with the buyer?",
        confirmButtonText = "Mark as Sold",
        confirmButtonColor = PrimaryPurple,
        icon = Icons.Outlined.CheckCircle,
        iconColor = PrimaryPurple,
        testTagConfirm = "btn_confirm_sold_popup"
    )

    // Fullscreen Photo Viewer Dialog
    if (viewingPhotoFullscreen != null) {
        Dialog(
            onDismissRequest = { viewingPhotoFullscreen = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { viewingPhotoFullscreen = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = viewingPhotoFullscreen,
                    contentDescription = "Full photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .fillMaxHeight(0.85f)
                        .clip(RoundedCornerShape(16.dp))
                )

                // Close button
                GlassIconButton(
                    onClick = { viewingPhotoFullscreen = null },
                    icon = Icons.Outlined.CheckCircle,
                    contentDescription = "Close photo",
                    tone = GlassIconTone.FROSTED_WHITE,
                    size = 40.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(16.dp)
                )
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
