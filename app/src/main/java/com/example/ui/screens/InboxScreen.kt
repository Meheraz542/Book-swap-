package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
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
import coil.compose.AsyncImage
import com.example.R
import com.example.data.MarketplaceRepository
import com.example.model.Conversation
import com.example.ui.components.*
import com.example.ui.theme.*

private data class ConversationDisplayData(
    val name: String,
    val college: String,
    val avatarUri: String?,
    val avatarRes: Int?
)

@Composable
fun InboxScreen(
    onNavigateToChat: (Conversation) -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val conversations by MarketplaceRepository.conversations.collectAsState()
    val currentUser by MarketplaceRepository.currentUser.collectAsState()
    val allUsers by MarketplaceRepository.allUsers.collectAsState()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedConversationIds by remember { mutableStateOf(setOf<String>()) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedConversationIds = emptySet()
    }

    val filteredConversations = remember(conversations, allUsers, currentUser, searchQuery) {
        val q = searchQuery.trim().lowercase()
        // Filter out conversations deleted from this user's inbox
        val activeConversations = conversations.filter { conv ->
            currentUser.id !in conv.deletedByUserIds
        }
        // Sort conversations by most recent message, placing active chat at the top
        val sortedConversations = activeConversations.sortedWith(
            compareByDescending<Conversation> { it.updatedAt }
                .thenByDescending { it.timestamp.equals("Just now", ignoreCase = true) }
        )
        val list = sortedConversations.map { conv ->
            val otherUserId = conv.getOtherUserId(currentUser.id)
            val matchedUser = allUsers.find { it.id == otherUserId }
            val resolvedName = matchedUser?.name ?: conv.getEffectiveOtherUserName(currentUser.id)
            val resolvedCollege = matchedUser?.college ?: conv.getEffectiveOtherUserCollege(currentUser.id)
            val resolvedAvatarUri = matchedUser?.profilePhotoUri ?: conv.getEffectiveOtherUserAvatarUri(currentUser.id)
            val resolvedAvatarRes = matchedUser?.profilePhotoRes ?: conv.otherUserAvatarRes
            Pair(
                conv,
                ConversationDisplayData(
                    name = resolvedName,
                    college = resolvedCollege,
                    avatarUri = resolvedAvatarUri,
                    avatarRes = resolvedAvatarRes
                )
            )
        }
        if (q.isEmpty()) list
        else list.filter { (conv, display) ->
            display.name.lowercase().contains(q) ||
                    conv.setTitle.lowercase().contains(q) ||
                    conv.lastMessage.lowercase().contains(q)
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
                if (isSelectionMode) {
                    // Bulk Selection Mode Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GlassIconButton(
                                onClick = {
                                    isSelectionMode = false
                                    selectedConversationIds = emptySet()
                                },
                                icon = Icons.Default.Close,
                                contentDescription = "Cancel selection",
                                tone = GlassIconTone.FROSTED_WHITE,
                                size = 42.dp,
                                testTag = "inbox_btn_cancel_selection"
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${selectedConversationIds.size} Selected",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val allSelected = filteredConversations.isNotEmpty() &&
                                    selectedConversationIds.size == filteredConversations.size

                            Surface(
                                onClick = {
                                    selectedConversationIds = if (allSelected) {
                                        emptySet()
                                    } else {
                                        filteredConversations.map { it.first.id }.toSet()
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = PrimaryPurpleLight.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .height(40.dp)
                                    .testTag("inbox_btn_toggle_select_all")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                ) {
                                    Text(
                                        text = if (allSelected) "Deselect" else "Select All",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PrimaryPurple
                                    )
                                }
                            }

                            GlassIconButton(
                                onClick = {
                                    if (selectedConversationIds.isNotEmpty()) {
                                        showBulkDeleteDialog = true
                                    }
                                },
                                icon = Icons.Outlined.Delete,
                                contentDescription = "Delete selected chats",
                                tone = if (selectedConversationIds.isNotEmpty()) GlassIconTone.DANGER else GlassIconTone.FROSTED_WHITE,
                                size = 42.dp,
                                testTag = "inbox_btn_bulk_delete"
                            )
                        }
                    }
                } else {
                    // Standard Header with profile, title, and edit mode button
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
                            onClick = {
                                isSelectionMode = true
                                selectedConversationIds = emptySet()
                            },
                            icon = Icons.Outlined.EditNote,
                            contentDescription = "Edit / Select multiple chats",
                            tone = GlassIconTone.PRIMARY_PURPLE,
                            size = 42.dp,
                            testTag = "inbox_btn_edit_mode"
                        )
                    }
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
                items(filteredConversations, key = { it.first.id }, contentType = { "conversation" }) { (conv, display) ->
                    val isSelected = conv.id in selectedConversationIds
                    ConversationItem(
                        conversation = conv,
                        display = display,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onToggleSelect = {
                            selectedConversationIds = if (isSelected) {
                                selectedConversationIds - conv.id
                            } else {
                                selectedConversationIds + conv.id
                            }
                        },
                        onClick = {
                            if (isSelectionMode) {
                                selectedConversationIds = if (isSelected) {
                                    selectedConversationIds - conv.id
                                } else {
                                    selectedConversationIds + conv.id
                                }
                            } else {
                                MarketplaceRepository.markConversationAsRead(conv.id)
                                onNavigateToChat(conv)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showBulkDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            title = {
                Text(
                    text = "Delete ${selectedConversationIds.size} Chat${if (selectedConversationIds.size > 1) "s" else ""}?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "These chat threads will be permanently removed from your inbox. If either student sends a new message in the future, the thread will reappear automatically.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = selectedConversationIds.size
                        MarketplaceRepository.deleteConversations(selectedConversationIds.toList())
                        Toast.makeText(context, "$count chat${if (count > 1) "s" else ""} deleted from inbox", Toast.LENGTH_SHORT).show()
                        selectedConversationIds = emptySet()
                        isSelectionMode = false
                        showBulkDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.testTag("inbox_confirm_bulk_delete")
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBulkDeleteDialog = false },
                    modifier = Modifier.testTag("inbox_cancel_bulk_delete")
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
private fun ConversationItem(
    conversation: Conversation,
    display: ConversationDisplayData,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onClick: () -> Unit
) {
    val hasPending = conversation.messages.any { it.isPending }
    val isUnread = conversation.unreadCount > 0

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("conversation_item_${conversation.id}"),
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        elevation = if (isUnread || isSelected) 4.dp else 2.dp,
        borderBrush = if (isSelected) Brush.linearGradient(listOf(PrimaryPurple, PrimaryPurpleEnd)) else GlassRimBrush,
        backgroundColor = if (isSelected) PrimaryPurpleLight.copy(alpha = 0.25f) else CardGlass
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox for selection mode
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = PrimaryPurple,
                        uncheckedColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("checkbox_conversation_${conversation.id}")
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            // User Avatar with online status and unread indicator dot
            Box {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = Color(0xFFE2E8F0)
                ) {
                    if (!display.avatarUri.isNullOrBlank()) {
                        AsyncImage(
                            model = display.avatarUri,
                            contentDescription = display.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (display.avatarRes != null) {
                        Image(
                            painter = painterResource(id = display.avatarRes),
                            contentDescription = display.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "No avatar displayed",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
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

                // Red unread notification dot badge on avatar
                if (isUnread) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
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
                        text = display.name,
                        fontSize = 15.sp,
                        fontWeight = if (isUnread) FontWeight.ExtraBold else FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = conversation.timestamp,
                        fontSize = 12.sp,
                        color = if (isUnread) PrimaryPurple else TextMuted,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val messageSnippet = if (hasPending && conversation.lastMessage.isNotBlank()) {
                        "Sending: ${conversation.lastMessage}"
                    } else {
                        conversation.lastMessage.ifBlank { "Started conversation" }
                    }

                    Text(
                        text = messageSnippet,
                        fontSize = 13.sp,
                        color = if (isUnread) TextPrimary else if (hasPending) PrimaryPurple else TextMuted,
                        fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    if (isUnread) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .height(20.dp)
                                .defaultMinSize(minWidth = 20.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEF4444))
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (hasPending) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(PrimaryPurple)
                        )
                    }
                }
            }
        }
    }
}
