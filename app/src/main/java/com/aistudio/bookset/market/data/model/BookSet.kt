package com.aistudio.bookset.market.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "book_sets")
data class BookSet(
    @PrimaryKey
    val id: String = "",
    val title: String = "",
    val category: String = "Literature",
    val booksCount: Int = 1,
    val bookTitles: List<String> = emptyList(),
    val originalPrice: Double = 0.0,
    val discountPrice: Double = 0.0,
    val condition: String = "Like New", // "New", "Like New", "Good"
    val description: String = "",
    val sellerId: String = "admin",
    val sellerName: String = "BookSet Verified",
    val sellerPhone: String = "+1 800-BOOK-SET",
    val rating: Float = 4.8f,
    val reviewCount: Int = 24,
    val imageUrl: String = "",
    val isFeatured: Boolean = false,
    val inStock: Boolean = true
)

@Entity(tableName = "mega_deals")
data class MegaDeal(
    @PrimaryKey
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val discountBadge: String = "50% OFF",
    val originalPrice: Double = 0.0,
    val dealPrice: Double = 0.0,
    val bookSetId: String = "",
    val endsInHours: Int = 24,
    val bannerColor: Long = 0xFF3F63F6
)

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val isAdmin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "orders")
data class OrderItem(
    @PrimaryKey
    val id: String = "",
    val bookSetId: String = "",
    val bookSetTitle: String = "",
    val price: Double = 0.0,
    val buyerId: String = "",
    val buyerName: String = "",
    val buyerPhone: String = "",
    val deliveryAddress: String = "",
    val status: String = "Confirmed", // "Pending", "Confirmed", "Shipped", "Delivered"
    val orderDate: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
