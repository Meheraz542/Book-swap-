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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.MarketplaceRepository
import com.example.model.Conversation
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun InboxScreen(
    onNavigateToChat: (Conversation) -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val conversations by MarketplaceRepository.conversations.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredConversations = remember(conversations, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) conversations
        else conversations.filter {
            it.otherUserName.lowercase().contains(q) ||
                    it.setTitle.lowercase().contains(q) ||
                    it.lastMessage.lowercase().contains(q)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BgCanvas,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Header with profile icon, title, and new compose icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassIconButton(
                        onClick = onNavigateToProfile,
                        icon = Icons.Outlined.Person,
                        contentDescription = "Profile",
                        tone = GlassIconTone.FROSTED_WHITE,
                        size = 42.dp,
                        testTag = "inbox_btn_profile"
                    )

                    Text(
                        text = "Inbox",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    GlassIconButton(
                        onClick = {},
                        icon = Icons.Outlined.EditNote,
                        contentDescription = "New message",
                        tone = GlassIconTone.PRIMARY_PURPLE,
                        size = 42.dp,
                        testTag = "inbox_btn_new_message"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search conversations bar with Liquid Glass & Inner Shadow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .liquidGlass(
                            shape = RoundedCornerShape(26.dp),
                            backgroundColor = Color.White.copy(alpha = 0.90f),
                            borderBrush = GlassRimBrush,
                            borderWidth = 1.2.dp,
                            elevation = 4.dp,
                            shadowColor = SoftShadowColor,
                            innerShadowColor = Color(0x15000000),
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
                        Spacer(modifier = Modifier.width(10.dp))
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text("Search conversations...", color = TextMuted, fontSize = 14.sp)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("search_conversations_input"),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = PrimaryPurple
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filteredConversations.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp, start = 16.dp, end = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White.copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = PrimaryPurpleLight,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.Search,
                                            contentDescription = null,
                                            tint = PrimaryPurple,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = if (searchQuery.isNotBlank()) "No chats match your search" else "No messages yet",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = if (searchQuery.isNotBlank()) "Try searching with a different student name or book title." else "When you message book sellers or buyers contact you, your conversations will appear here.",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                items(filteredConversations) { conv ->
                    ConversationItem(
                        conversation = conv,
                        onClick = { onNavigateToChat(conv) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("conversation_item_${conversation.id}"),
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        elevation = 3.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User Avatar with potential online badge
            Box {
                Surface(
                    modifier = Modifier.size(52.dp),
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
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
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
                        text = conversation.otherUserName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = conversation.timestamp,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.lastMessage,
                        fontSize = 13.sp,
                        color = if (conversation.unreadCount > 0) TextPrimary else TextMuted,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    if (conversation.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = conversation.unreadCount.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
