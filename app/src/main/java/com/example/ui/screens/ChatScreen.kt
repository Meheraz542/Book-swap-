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
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreVert
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

    val conversation = conversations.find { it.id == conversationId }
    val referencedSet = bookSets.find { it.id == conversation?.setId }

    var inputMessage by remember { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Real-time snapshot listener lifecycle for chat messages
    DisposableEffect(conversationId) {
        val registration = MarketplaceRepository.listenMessages(conversationId)
        onDispose {
            registration?.remove()
        }
    }

    LaunchedEffect(conversation?.messages?.size) {
        val count = conversation?.messages?.size ?: 0
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
                            color = PrimaryPurpleLight
                        ) {
                            Image(
                                painter = painterResource(id = conversation.otherUserAvatarRes ?: R.drawable.avatar_rahim_1789677826412),
                                contentDescription = conversation.otherUserName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
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

                    // User name & Online state
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = conversation.otherUserName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
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
                                text = "Online",
                                fontSize = 12.sp,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Phone Call
                    GlassIconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:01711223344"))
                            context.startActivity(intent)
                        },
                        icon = Icons.Outlined.Phone,
                        contentDescription = "Call",
                        tone = GlassIconTone.CYAN,
                        size = 40.dp,
                        testTag = "chat_call_button"
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Menu
                    GlassIconButton(
                        onClick = {},
                        icon = Icons.Outlined.MoreVert,
                        contentDescription = "More",
                        tone = GlassIconTone.FROSTED_WHITE,
                        size = 40.dp,
                        testTag = "chat_more_button"
                    )
                }
            }
        },
        bottomBar = {
            // Glass Message Composer with Smart Quick Chips
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
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
                                    MarketplaceRepository.sendMessage(conversationId, suggestion)
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
                                            MarketplaceRepository.sendMessage(conversationId, inputMessage.trim())
                                            inputMessage = ""
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
                                MarketplaceRepository.sendMessage(conversationId, inputMessage.trim())
                                inputMessage = ""
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
            items(conversation.messages, key = { it.id }, contentType = { "chat_msg" }) { msg ->
                ChatBubble(message = msg)
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
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
            // Incoming Received message (soft white neumorphic card)
            Surface(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp),
                        ambientColor = SoftShadowColor
                    ),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Text(
                    text = message.text,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = message.timestamp,
            fontSize = 11.sp,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}
