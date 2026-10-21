package com.example.model

enum class Technology(val id: String, val displayName: String, val shortName: String) {
    CIVIL("civil", "Civil Technology", "Civil"),
    ELECTRICAL("electrical", "Electrical Technology", "Electrical"),
    MECHANICAL("mechanical", "Mechanical Technology", "Mechanical"),
    POWER("power", "Power Technology", "Power"),
    CSE("cse", "Computer Science & Technology", "Computer"),
    AIDT("aidt", "Architecture & Interior Design Technology", "AIDT"),
    TELECOM("telecom", "Telecommunication Technology", "Telecom");

    companion object {
        fun fromId(id: String): Technology = entries.find {
            it.id.equals(id, ignoreCase = true) || (id.equals("architecture", ignoreCase = true) && it == AIDT)
        } ?: CIVIL
    }
}

enum class Semester(val id: String, val displayName: String, val shortName: String) {
    SEM_1("1", "1st Semester", "1st"),
    SEM_2("2", "2nd Semester", "2nd"),
    SEM_3("3", "3rd Semester", "3rd"),
    SEM_4("4", "4th Semester", "4th"),
    SEM_5("5", "5th Semester", "5th"),
    SEM_6("6", "6th Semester", "6th"),
    SEM_7("7", "7th Semester", "7th");

    companion object {
        fun fromId(id: String): Semester = entries.find { it.id == id } ?: SEM_1
    }
}

enum class SetCondition(val label: String) {
    LIKE_NEW("Like New"),
    GOOD("Good"),
    USED("Used"),
    HEAVILY_USED("Heavily Used")
}

enum class SetStatus {
    AVAILABLE,
    SOLD,
    REMOVED
}

data class IncludedBook(
    val id: String,
    val setId: String,
    val bookName: String,
    val subject: String,
    val author: String = "",
    val edition: String = "",
    val bookCode: String = "",
    val condition: String = "Good",
    val imageUris: List<String> = emptyList()
)

data class BookSet(
    val id: String,
    val sellerId: String,
    val sellerName: String,
    val sellerCollege: String,
    val sellerTech: String,
    val sellerSem: String,
    val sellerPhone: String,
    val sellerEmail: String,
    val title: String,
    val subject: String,
    val technology: Technology,
    val semester: Semester,
    val college: String,
    val totalPrice: Int,
    val overallCondition: SetCondition,
    val description: String,
    val includedBooks: List<IncludedBook> = emptyList(),
    val coverDrawableRes: Int? = null,
    val imageUrls: List<String> = emptyList(),
    val status: SetStatus = SetStatus.AVAILABLE,
    val isSaved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val college: String,
    val technology: Technology,
    val currentSemester: Semester,
    val profilePhotoRes: Int? = null,
    val profilePhotoUri: String? = null,
    val role: String = "USER", // "USER" or "ADMIN"
    val accountStatus: String = "ACTIVE" // "ACTIVE", "SUSPENDED", "BANNED"
)

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: String,
    val isMe: Boolean
)

data class Conversation(
    val id: String,
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
    val isOnline: Boolean = true,
    val messages: List<ChatMessage> = emptyList()
)

enum class MegaDealStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,
    CANCELLED
}

data class MegaDeal(
    val dealId: String,
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
    val status: MegaDealStatus = MegaDealStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)

data class AppNotification(
    val id: String,
    val type: String,
    val title: String,
    val message: String,
    val timeAgo: String,
    val isRead: Boolean = false,
    val relatedSetId: String? = null,
    val relatedDealId: String? = null
)

data class AppTrafficStats(
    val dailyActiveUsers: Int = 0,
    val totalVisitsToday: Int = 0,
    val bookChecksToday: Int = 0,
    val chatInquiriesToday: Int = 0,
    val fpiTrafficShare: Int = 0,
    val fciTrafficShare: Int = 0,
    val peakHour: String = "No traffic yet",
    val newSignupsToday: Int = 0
)
