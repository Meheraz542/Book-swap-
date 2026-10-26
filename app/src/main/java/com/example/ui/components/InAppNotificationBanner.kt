package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.InAppMessageAlert
import kotlinx.coroutines.delay

/**
 * Premium in-app floating visual alert banner for real-time incoming messages.
 * Dynamically displays the sender's name, profile picture, college, message snippet,
 * and enables instant direct navigation to the chat thread on tap.
 */
@Composable
fun InAppNotificationBanner(
    alert: InAppMessageAlert?,
    onOpenChat: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(alert?.id) {
        if (alert != null) {
            try {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (_: Exception) {}
            // Auto-dismiss alert banner after 4.5 seconds of inactivity
            delay(4500)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = alert != null,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(animationSpec = tween(180)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(180)
        ) + fadeOut(animationSpec = tween(150)),
        modifier = modifier
    ) {
        if (alert != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = Color(0x60000000),
                            spotColor = Color(0x80000000)
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF23162C).copy(alpha = 0.96f),
                                    Color(0xFF140D1A).copy(alpha = 0.96f)
                                )
                            )
                        )
                        .border(
                            width = 1.2.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    Color(0xFFE11D48).copy(alpha = 0.85f),
                                    Color.White.copy(alpha = 0.35f),
                                    Color(0xFF7C3AED).copy(alpha = 0.65f)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable {
                            onOpenChat(alert.conversationId)
                            onDismiss()
                        }
                        .testTag("in_app_notification_banner"),
                    color = Color.Transparent,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sender Avatar with Online Live Indicator
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Surface(
                                modifier = Modifier.size(46.dp),
                                shape = CircleShape,
                                color = Color(0xFF2D1F38)
                            ) {
                                if (!alert.senderAvatarUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = alert.senderAvatarUri,
                                        contentDescription = "${alert.senderName}'s avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFFE11D48), Color(0xFF8B5CF6))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = alert.senderName.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                            }

                            // Glowing emerald real-time alert badge
                            Box(
                                modifier = Modifier
                                    .size(13.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                                    .border(1.5.dp, Color(0xFF140D1A), CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Sender Name, Snippet, and College Info
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Text(
                                        text = alert.senderName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Subtle "New" pill
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFE11D48).copy(alpha = 0.25f))
                                            .border(0.6.dp, Color(0xFFE11D48).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "CHAT",
                                            color = Color(0xFFFDA4AF),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }

                                Text(
                                    text = alert.timestamp,
                                    color = Color(0xFFA1A1AA),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            Spacer(modifier = Modifier.height(2.5.dp))

                            Text(
                                text = alert.messageText,
                                color = Color(0xFFE4E4E7),
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 17.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Dismiss button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_dismiss_in_app_banner")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss notification",
                                tint = Color(0xFFA1A1AA),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
