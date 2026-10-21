package com.example.data.local.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.model.UserProfile
import com.example.model.Technology
import com.example.model.Semester

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val passwordHash: String,
    val phone: String,
    val college: String,
    val technologyId: String,
    val semesterId: String,
    val profilePhotoRes: Int? = null,
    val profilePhotoUri: String? = null,
    val role: String = "USER",
    val accountStatus: String = "ACTIVE",
    val isCurrentSession: Boolean = false
)

@Entity(tableName = "book_sets")
data class BookSetEntity(
    @PrimaryKey val id: String,
    val sellerId: String,
    val sellerName: String,
    val sellerCollege: String,
    val sellerTech: String,
    val sellerSem: String,
    val sellerPhone: String,
    val sellerEmail: String,
    val title: String,
    val subject: String,
    val technologyId: String,
    val semesterId: String,
    val college: String,
    val totalPrice: Int,
    val overallCondition: String,
    val description: String,
    val coverDrawableRes: Int? = null,
    val imageUrlsString: String = "",
    val status: String = "AVAILABLE",
    val isSaved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "included_books",
    foreignKeys = [
        ForeignKey(
            entity = BookSetEntity::class,
            parentColumns = ["id"],
            childColumns = ["setId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["setId"])]
)
data class IncludedBookEntity(
    @PrimaryKey val id: String,
    val setId: String,
    val bookName: String,
    val subject: String,
    val author: String = "",
    val edition: String = "",
    val bookCode: String = "",
    val condition: String = "Good",
    val image1Uri: String = "",
    val image2Uri: String = ""
)

data class BookSetWithBooks(
    @Embedded val bookSet: BookSetEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "setId"
    )
    val books: List<IncludedBookEntity>
)

@Entity(tableName = "mega_deals")
data class MegaDealEntity(
    @PrimaryKey val dealId: String,
    val setId: String,
    val setTitle: String,
    val buyerId: String,
    val buyerName: String,
    val sellerId: String,
    val sellerName: String,
    val finalPrice: Int,
    val location: String,
    val date: String,
    val time: String,
    val status: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val buyerId: String,
    val sellerId: String,
    val otherUserName: String,
    val otherUserCollege: String,
    val otherUserAvatarRes: Int? = null,
    val setId: String,
    val setTitle: String,
    val setPrice: Int,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = true
)

@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["conversationId"])]
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: String,
    val isMe: Boolean
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val message: String,
    val timeAgo: String,
    val isRead: Boolean = false,
    val relatedSetId: String? = null,
    val relatedDealId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

fun UserEntity.toUserProfile(): UserProfile {
    return UserProfile(
        id = id,
        name = name,
        email = email,
        phone = phone,
        college = college,
        technology = Technology.fromId(technologyId),
        currentSemester = Semester.fromId(semesterId),
        profilePhotoRes = profilePhotoRes,
        profilePhotoUri = profilePhotoUri,
        role = role,
        accountStatus = accountStatus
    )
}

fun UserEntity.toDomain(): UserProfile = toUserProfile()
