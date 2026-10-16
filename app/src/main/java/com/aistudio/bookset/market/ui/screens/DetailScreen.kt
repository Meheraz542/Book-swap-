package com.aistudio.bookset.market.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aistudio.bookset.market.data.model.BookSet
import com.aistudio.bookset.market.data.model.OrderItem
import com.aistudio.bookset.market.data.repository.MarketplaceRepository
import com.aistudio.bookset.market.ui.components.NeumorphicButton
import com.aistudio.bookset.market.ui.components.NeumorphicCard
import com.aistudio.bookset.market.ui.components.NeumorphicRecessedBox
import com.aistudio.bookset.market.ui.theme.AccentCoral
import com.aistudio.bookset.market.ui.theme.AccentGreen
import com.aistudio.bookset.market.ui.theme.GoldAccent
import com.aistudio.bookset.market.ui.theme.NeumorphicBackground
import com.aistudio.bookset.market.ui.theme.NeumorphicDarkShadow
import com.aistudio.bookset.market.ui.theme.NeumorphicSurface
import com.aistudio.bookset.market.ui.theme.PrimaryBlue
import com.aistudio.bookset.market.ui.theme.TextMuted
import com.aistudio.bookset.market.ui.theme.TextPrimary
import com.aistudio.bookset.market.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun DetailScreen(
    bookSet: BookSet,
    repository: MarketplaceRepository,
    onBack: () -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var showOrderDialog by remember { mutableStateOf(false) }
    var orderSuccess by remember { mutableStateOf(false) }

    var buyerName by remember { mutableStateOf("") }
    var buyerPhone by remember { mutableStateOf("") }
    var buyerAddress by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeumorphicBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(4.dp, CircleShape, ambientColor = NeumorphicDarkShadow)
                        .background(NeumorphicSurface, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple()
                        ) { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Text(
                    text = "Book Set Details",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(4.dp, CircleShape, ambientColor = NeumorphicDarkShadow)
                        .background(NeumorphicSurface, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple()
                        ) { onNavigateToChat(bookSet.sellerName) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Chat with Seller",
                        tint = PrimaryBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Hero Cover Card
            NeumorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                if (bookSet.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = bookSet.imageUrl,
                        contentDescription = bookSet.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PrimaryBlue.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = "Cover Placeholder",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Info Block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bookSet.category,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Text(
                        text = bookSet.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = bookSet.condition,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Price & Rating Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$${bookSet.discountPrice.toInt()}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    if (bookSet.originalPrice > bookSet.discountPrice) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$${bookSet.originalPrice.toInt()}",
                            fontSize = 16.sp,
                            color = TextMuted,
                            textDecoration = TextDecoration.LineThrough
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AccentCoral.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            val percent = ((1.0 - (bookSet.discountPrice / bookSet.originalPrice)) * 100).toInt()
                            Text(
                                text = "-$percent%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentCoral
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = GoldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = " ${bookSet.rating} (${bookSet.reviewCount} reviews)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Volumes Included Card
            if (bookSet.bookTitles.isNotEmpty()) {
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Included Volumes (${bookSet.booksCount} Books)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        bookSet.bookTitles.forEachIndexed { idx, title ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Check",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Vol ${idx + 1}: $title",
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Description Card
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Description & Condition",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = bookSet.description,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Seller Info Card
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Seller",
                                tint = PrimaryBlue
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = bookSet.sellerName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = bookSet.sellerPhone,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    NeumorphicButton(
                        onClick = { onNavigateToChat(bookSet.sellerName) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "Chat",
                            color = PrimaryBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NeumorphicButton(
                    onClick = { showOrderDialog = true },
                    backgroundColor = PrimaryBlue,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Order",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Order Now",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Order Placement Dialog
    if (showOrderDialog) {
        AlertDialog(
            onDismissRequest = { showOrderDialog = false },
            title = {
                Text(
                    text = "Confirm Order",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Total Price: $${bookSet.discountPrice.toInt()}",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = buyerName,
                        onValueChange = { buyerName = it },
                        label = { Text("Your Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = buyerPhone,
                        onValueChange = { buyerPhone = it },
                        label = { Text("Contact Phone") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = buyerAddress,
                        onValueChange = { buyerAddress = it },
                        label = { Text("Shipping Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newOrder = OrderItem(
                            id = "ord_${UUID.randomUUID().toString().take(8)}",
                            bookSetId = bookSet.id,
                            bookSetTitle = bookSet.title,
                            price = bookSet.discountPrice,
                            buyerName = buyerName.ifBlank { "Valued Reader" },
                            buyerPhone = buyerPhone.ifBlank { "+1 800-BOOK-BUY" },
                            deliveryAddress = buyerAddress.ifBlank { "Standard Delivery Address" },
                            status = "Confirmed",
                            orderDate = System.currentTimeMillis()
                        )
                        scope.launch {
                            repository.createOrder(newOrder)
                            showOrderDialog = false
                            orderSuccess = true
                        }
                    }
                ) {
                    Text("Place Order", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOrderDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (orderSuccess) {
        AlertDialog(
            onDismissRequest = { orderSuccess = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = AccentGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Order Confirmed!")
                }
            },
            text = {
                Text("Your book set order for '${bookSet.title}' has been placed successfully. You can track status in your profile.")
            },
            confirmButton = {
                TextButton(onClick = { orderSuccess = false }) {
                    Text("Done", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                }
            }
        )
    }
}
