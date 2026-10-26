package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.MarketplaceRepository
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    onNavigateBack: () -> Unit,
    onNavigateToSetDetails: (BookSet) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val conversations by MarketplaceRepository.conversations.collectAsState()
    val bookSets by MarketplaceRepository.bookSets.collectAsState()
    val currentUser by MarketplaceRepository.currentUser.collectAsState()
    val allUsers by MarketplaceRepository.allUsers.collectAsState()

    val conversation = conversations.find { it.id == conversationId }
    val referencedSet = bookSets.find { it.id == conversation?.setId }

    val otherUserId = conversation?.getOtherUserId(currentUser.id) ?: ""

    // Dynamically resolve other user's profile from cache, allUsers, and repository
    var otherUserProfile by remember(otherUserId) {
        mutableStateOf(
            MarketplaceRepository.getCachedUserProfile(otherUserId)
                ?: allUsers.find { it.id == otherUserId }
        )
    }

    LaunchedEffect(otherUserId, allUsers) {
        if (otherUserId.isNotBlank()) {
            val cached = MarketplaceRepository.getCachedUserProfile(otherUserId)
                ?: allUsers.find { it.id == otherUserId }
            if (cached != null) {
                otherUserProfile = cached
            }
            val remote = MarketplaceRepository.getUserProfileAsync(otherUserId)
            if (remote != null) {
                otherUserProfile = remote
            }
        }
    }

    val matchedOtherUser = otherUserProfile ?: allUsers.find { it.id == otherUserId }
    val otherUserName = otherUserProfile?.name?.takeIf { it.isNotBlank() }
        ?: matchedOtherUser?.name
        ?: conversation?.getEffectiveOtherUserName(currentUser.id)
        ?.takeIf { it.isNotBlank() }
        ?: "Student"
    val otherUserCollege = otherUserProfile?.college?.takeIf { it.isNotBlank() }
        ?: matchedOtherUser?.college
        ?: conversation?.getEffectiveOtherUserCollege(currentUser.id)
        ?: ""
    val otherUserAvatarUri = otherUserProfile?.profilePhotoUri
        ?: matchedOtherUser?.profilePhotoUri
        ?: conversation?.getEffectiveOtherUserAvatarUri(currentUser.id)
    val otherUserAvatarRes = otherUserProfile?.profilePhotoRes
        ?: matchedOtherUser?.profilePhotoRes
        ?: conversation?.otherUserAvatarRes

    // Resolve real phone number directly from other participant's profile
    val otherUserPhone = otherUserProfile?.phone?.trim()?.takeIf { it.isNotBlank() }
        ?: matchedOtherUser?.phone?.trim()?.takeIf { it.isNotBlank() }
        ?: (if (conversation?.sellerId == otherUserId) referencedSet?.sellerPhone?.trim()?.takeIf { it.isNotBlank() } else null)
        ?: ""

    var inputMessage by remember { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    var showMenuDropdown by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Keep local list of messages to ensure zero temporary disappearance during sending/syncing
    val initialMsgs = remember(conversationId) { conversation?.messages ?: emptyList() }
    var localMessages by remember(conversationId) { mutableStateOf(initialMsgs) }

    LaunchedEffect(conversationId) {
        MarketplaceRepository.markConversationAsRead(conversationId)
    }

    LaunchedEffect(conversation?.messages) {
        val currentList = conversation?.messages ?: emptyList()
        if (currentList.isNotEmpty()) {
            val liveIds = currentList.map { it.id }.toSet()
            val pending = localMessages.filter { it.id !in liveIds }
            localMessages = (currentList + pending)
        }
    }

    // Real-time snapshot listener lifecycle for chat messages
    DisposableEffect(conversationId) {
        val registration = MarketplaceRepository.listenMessages(conversationId) { liveMsgs ->
            if (liveMsgs.isNotEmpty()) {
                val liveIds = liveMsgs.map { it.id }.toSet()
                val pending = localMessages.filter { it.id !in liveIds }
                localMessages = (liveMsgs + pending)
            }
        }
        onDispose {
            registration?.remove()
        }
    }

    LaunchedEffect(localMessages.size) {
        val count = localMessages.size
        if (count > 0) {
            val totalItems = count + if (referencedSet != null) 1 else 0
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    if (conversation == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Conversation not found", color = TextMuted)
        }
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BgCanvas,
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars),
                color = Color.White.copy(alpha = 0.95f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back
                    GlassIconButton(
                        onClick = onNavigateBack,
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tone = GlassIconTone.FROSTED_WHITE,
                        size = 40.dp,
                        testTag = "chat_back_button"
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Avatar with online status
                    Box {
                        Surface(
                            modifier = Modifier.size(42.dp),
                            shape = CircleShape,
                            color = Color(0xFFE2E8F0)
                        ) {
                            if (!otherUserAvatarUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = otherUserAvatarUri,
                                    contentDescription = otherUserName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (otherUserAvatarRes != null) {
                                Image(
                                    painter = painterResource(id = otherUserAvatarRes),
                                    contentDescription = otherUserName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = "No avatar displayed",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                        if (conversation.isOnline) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(11.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // User name & College/Online state
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = otherUserName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (otherUserCollege.isNotBlank()) "$otherUserCollege • Online" else "Online",
                                fontSize = 11.sp,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }

                    // Phone Call
                    GlassIconButton(
                        onClick = {
                            if (otherUserPhone.isNotBlank()) {
                                val cleanPhone = otherUserPhone.filter { it.isDigit() || it == '+' }
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
                                    "No phone number available in ${otherUserName}'s profile.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        icon = Icons.Outlined.Phone,
                        contentDescription = "Call $otherUserName",
                        tone = GlassIconTone.CYAN,
                        size = 40.dp,
                        testTag = "chat_call_button"
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Three-dot options menu with Delete
                    Box {
                        GlassIconButton(
                            onClick = { showMenuDropdown = true },
                            icon = Icons.Outlined.MoreVert,
                            contentDescription = "More options",
                            tone = GlassIconTone.FROSTED_WHITE,
                            size = 40.dp,
                            testTag = "chat_more_button"
                        )

                        DropdownMenu(
                            expanded = showMenuDropdown,
                            onDismissRequest = { showMenuDropdown = false },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .background(Color.White)
                                .testTag("chat_options_menu")
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Delete",
                                        color = Color(0xFFEF4444),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                },
                                onClick = {
                                    showMenuDropdown = false
                                    showDeleteConfirmDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Delete",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                modifier = Modifier.testTag("chat_menu_delete_option")
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Glass Message Composer with Smart Quick Chips
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding(),
                color = Color.White.copy(alpha = 0.96f),
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Smart Suggestion Chips
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val suggestions = listOf(
                            "Can you lower the price?",
                            "Can we meet on campus tomorrow?",
                            "Are all the book pages intact?",
                            "Are these books still available?"
                        )
                        items(suggestions) { suggestion ->
                            LiquidGlassChip(
                                text = suggestion,
                                selected = false,
                                onClick = {
                                    val sentMsg = MarketplaceRepository.sendMessage(conversationId, suggestion)
                                    localMessages = (localMessages + sentMsg).distinctBy { it.id }
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 10.dp, top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    // Plus attachment
                    GlassIconButton(
                        onClick = {},
                        icon = Icons.Default.Add,
                        contentDescription = "Attach",
                        tone = GlassIconTone.FROSTED_WHITE,
                        size = 40.dp,
                        testTag = "chat_attach_button"
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Text field
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = CardGlassSoft,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = inputMessage,
                                onValueChange = { inputMessage = it },
                                placeholder = { Text("Type a message...", color = TextMuted, fontSize = 14.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_input_text"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (inputMessage.isNotBlank()) {
                                            val textToSend = inputMessage.trim()
                                            inputMessage = ""
                                            val sentMsg = MarketplaceRepository.sendMessage(conversationId, textToSend)
                                            localMessages = (localMessages + sentMsg).distinctBy { it.id }
                                        }
                                    }
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = PrimaryPurple
                                )
                            )

                            Icon(
                                imageVector = Icons.Outlined.Image,
                                contentDescription = "Photos",
                                tint = TextMuted,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Send Button
                    GlassIconButton(
                        onClick = {
                            if (inputMessage.isNotBlank()) {
                                val textToSend = inputMessage.trim()
                                inputMessage = ""
                                val sentMsg = MarketplaceRepository.sendMessage(conversationId, textToSend)
                                localMessages = (localMessages + sentMsg).distinctBy { it.id }
                            }
                        },
                        icon = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tone = GlassIconTone.PRIMARY_PURPLE,
                        size = 44.dp,
                        testTag = "chat_send_button"
                    )
                }
            }
        }
    }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Book set context banner at top
            if (referencedSet != null) {
                item {
                    Surface(
                        onClick = { onNavigateToSetDetails(referencedSet) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 2.dp, shape = RoundedCornerShape(18.dp), ambientColor = SoftShadowColor),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFE2E8F0))
                            ) {
                                if (referencedSet.coverDrawableRes != null) {
                                    Image(
                                        painter = painterResource(id = referencedSet.coverDrawableRes),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(referencedSet.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary, maxLines = 1)
                                Text("${referencedSet.semester.shortName} Sem • ${referencedSet.includedBooks.size} Books • Tk ${referencedSet.totalPrice}", fontSize = 12.sp, color = TextMuted)
                            }
                            Text("View Set", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryPurple)
                        }
                    }
                }
            }

            // Chat Messages list
            items(localMessages, key = { it.id }, contentType = { "chat_msg" }) { msg ->
                ChatBubble(
                    message = msg,
                    allUsers = allUsers,
                    counterpartyName = otherUserName,
                    counterpartyAvatarUri = otherUserAvatarUri,
                    counterpartyAvatarRes = otherUserAvatarRes
                )
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Delete Chat",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this chat thread from your inbox? If you or $otherUserName send a new message in the future, the thread will reappear in your inbox automatically.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        MarketplaceRepository.deleteConversation(conversationId)
                        android.widget.Toast.makeText(context, "Chat deleted from inbox", android.widget.Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.testTag("dialog_confirm_delete_chat")
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    modifier = Modifier.testTag("dialog_cancel_delete_chat")
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    allUsers: List<UserProfile>,
    counterpartyName: String,
    counterpartyAvatarUri: String?,
    counterpartyAvatarRes: Int?
) {
    // Single Source of Truth: Asynchronously fetch the sender's profile using message.senderId
    // before rendering the message bubble, ensuring accurate user name and avatar display.
    val senderProfile by produceState<UserProfile?>(
        initialValue = MarketplaceRepository.getCachedUserProfile(message.senderId),
        key1 = message.senderId
    ) {
        value = MarketplaceRepository.getUserProfileAsync(message.senderId)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isMe) Alignment.End else Alignment.Start
    ) {
        if (message.text.startsWith("🔥 Mega Deal")) {
            // System Deal Notification Bubble
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(18.dp), ambientColor = SoftShadowColor),
                shape = RoundedCornerShape(18.dp),
                color = PrimaryPurpleLight.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.3f))
            ) {
                Text(
                    text = message.text,
                    color = PrimaryPurple,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(14.dp),
                    lineHeight = 18.sp
                )
            }
        } else if (message.isMe) {
            // Outgoing Sent message (soft lavender/purple gradient glass bubble)
            Surface(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp),
                        ambientColor = PrimaryPurple.copy(alpha = 0.2f)
                    ),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF8C86F8), Color(0xFFA685F8))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = message.text,
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        } else {
            // Incoming Received message with asynchronously resolved sender profile (single source of truth)
            val resolvedSenderName = senderProfile?.name
                ?: message.senderName.takeIf { it.isNotBlank() }
                ?: counterpartyName.takeIf { it.isNotBlank() }
                ?: "Student"

            val resolvedAvatarUri = senderProfile?.profilePhotoUri
                ?: message.senderAvatarUri
                ?: counterpartyAvatarUri

            val resolvedAvatarRes = senderProfile?.profilePhotoRes
                ?: message.senderAvatarRes
                ?: counterpartyAvatarRes

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Start
            ) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = Color(0xFFE2E8F0)
                ) {
                    if (!resolvedAvatarUri.isNullOrBlank()) {
                        AsyncImage(
                            model = resolvedAvatarUri,
                            contentDescription = resolvedSenderName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (resolvedAvatarRes != null) {
                        Image(
                            painter = painterResource(id = resolvedAvatarRes),
                            contentDescription = resolvedSenderName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = resolvedSenderName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryPurple,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                    )

                    Surface(
                        modifier = Modifier
                            .widthIn(max = 260.dp)
                            .shadow(
                                elevation = 3.dp,
                                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp),
                                ambientColor = SoftShadowColor
                            ),
                        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Text(
                            text = message.text,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                text = message.timestamp,
                fontSize = 11.sp,
                color = TextMuted
            )
            if (message.isMe) {
                Spacer(modifier = Modifier.width(4.dp))
                if (message.isPending) {
                    Text(
                        text = "• sending...",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                } else {
                    Text(
                        text = "• sent",
                        fontSize = 10.sp,
                        color = Color(0xFF10B981)
                    )
                }
            }
        }
    }
}
