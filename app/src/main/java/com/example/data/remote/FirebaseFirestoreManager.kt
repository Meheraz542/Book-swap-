package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class FirebaseFirestoreManager(private val context: Context) {

    private val tag = "FirestoreManager"

    // Mandatory custom database ID resolution
    val firestore: FirebaseFirestore by lazy {
        try {
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(context) ?: throw IllegalStateException("initializeApp returned null")
                } catch (_: Exception) {
                    val options = com.google.firebase.FirebaseOptions.Builder()
                        .setApplicationId("1:986504877917:android:1f76f3f3338ce3a1eabab4")
                        .setApiKey("AIzaSyD2RDBEXhKVXADbPD-SsM0h5YPNgf9ntE4")
                        .setProjectId("studio-4383603118-a2874")
                        .setStorageBucket("studio-4383603118-a2874.firebasestorage.app")
                        .setGcmSenderId("986504877917")
                        .build()
                    FirebaseApp.initializeApp(context, options)
                }
            } else {
                FirebaseApp.getInstance()
            }
            val dbId = try { context.getString(R.string.firestore_database_id) } catch (_: Exception) { "" }
            if (dbId.isNotBlank()) {
                FirebaseFirestore.getInstance(app, dbId)
            } else {
                FirebaseFirestore.getInstance(app)
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to get custom named firestore instance, falling back: ${e.message}")
            try {
                FirebaseFirestore.getInstance()
            } catch (ex: Exception) {
                Log.e(tag, "Fallback also failed", ex)
                FirebaseFirestore.getInstance()
            }
        }
    }

    // -------------------------------------------------------------
    // USERS COLLECTION (/users)
    // -------------------------------------------------------------

    suspend fun saveUserProfile(user: UserProfile) {
        try {
            val userMap = hashMapOf(
                "id" to user.id,
                "name" to user.name,
                "email" to user.email,
                "phone" to user.phone,
                "college" to user.college,
                "technologyId" to user.technology.id,
                "semesterId" to user.currentSemester.id,
                "profilePhotoUri" to (user.profilePhotoUri ?: ""),
                "role" to user.role,
                "accountStatus" to user.accountStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("users").document(user.id)
                .set(userMap, SetOptions.merge())
                .await()
            Log.d(tag, "User saved to Firestore: ${user.id} (${user.email})")
        } catch (e: Exception) {
            Log.e(tag, "Failed to save user profile", e)
        }
    }

    suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            if (doc.exists()) docToUserProfile(doc) else null
        } catch (e: Exception) {
            Log.e(tag, "Failed to fetch user profile $userId", e)
            null
        }
    }

    fun listenAllUsers(onUsersUpdated: (List<UserProfile>) -> Unit): ListenerRegistration? {
        return firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error listening to users", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val users = snapshot.documents.mapNotNull { docToUserProfile(it) }
                    Log.d(tag, "Live users received from Firestore: ${users.size}")
                    onUsersUpdated(users)
                }
            }
    }

    suspend fun updateUserAccountStatus(userId: String, status: String) {
        try {
            firestore.collection("users").document(userId)
                .update("accountStatus", status)
                .await()
        } catch (e: Exception) {
            Log.e(tag, "Failed to update user status", e)
        }
    }

    suspend fun deleteUser(userId: String) {
        try {
            firestore.collection("users").document(userId).delete().await()
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete user", e)
        }
    }

    private fun docToUserProfile(doc: DocumentSnapshot): UserProfile? {
        val id = doc.getString("id") ?: doc.id
        val name = doc.getString("name") ?: return null
        val email = doc.getString("email") ?: ""
        val phone = doc.getString("phone") ?: ""
        val college = doc.getString("college") ?: "Feni Polytechnic Institute"
        val techId = doc.getString("technologyId") ?: "civil"
        val semId = doc.getString("semesterId") ?: "1"
        val photoUri = doc.getString("profilePhotoUri")?.takeIf { it.isNotBlank() }
        val role = doc.getString("role") ?: "USER"
        val status = doc.getString("accountStatus") ?: "ACTIVE"

        return UserProfile(
            id = id,
            name = name,
            email = email,
            phone = phone,
            college = college,
            technology = Technology.fromId(techId),
            currentSemester = Semester.fromId(semId),
            profilePhotoRes = null,
            profilePhotoUri = photoUri,
            role = role,
            accountStatus = status
        )
    }

    // -------------------------------------------------------------
    // BOOK SETS COLLECTION (/bookSets)
    // -------------------------------------------------------------

    suspend fun publishBookSet(set: BookSet) {
        try {
            val includedBooksList = set.includedBooks.map { book ->
                hashMapOf(
                    "id" to book.id,
                    "setId" to book.setId,
                    "bookName" to book.bookName,
                    "subject" to book.subject,
                    "author" to book.author,
                    "edition" to book.edition,
                    "bookCode" to book.bookCode,
                    "condition" to book.condition
                )
            }

            val map = hashMapOf(
                "id" to set.id,
                "sellerId" to set.sellerId,
                "sellerName" to set.sellerName,
                "sellerCollege" to set.sellerCollege,
                "sellerTech" to set.sellerTech,
                "sellerSem" to set.sellerSem,
                "sellerPhone" to set.sellerPhone,
                "sellerEmail" to set.sellerEmail,
                "title" to set.title,
                "subject" to set.subject,
                "technologyId" to set.technology.id,
                "semesterId" to set.semester.id,
                "college" to set.college,
                "totalPrice" to set.totalPrice,
                "overallCondition" to set.overallCondition.name,
                "description" to set.description,
                "imageUrls" to set.imageUrls,
                "status" to set.status.name,
                "createdAt" to set.createdAt,
                "includedBooks" to includedBooksList
            )

            firestore.collection("bookSets").document(set.id)
                .set(map, SetOptions.merge())
                .await()
            Log.d(tag, "BookSet published to Firestore: ${set.id}")
        } catch (e: Exception) {
            Log.e(tag, "Failed to publish book set", e)
            throw e
        }
    }

    fun listenAllBookSets(onSetsUpdated: (List<BookSet>) -> Unit): ListenerRegistration {
        return firestore.collection("bookSets")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error listening to book sets", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val sets = snapshot.documents.mapNotNull { docToBookSet(it) }
                    Log.d(tag, "Live bookSets received from Firestore: ${sets.size}")
                    onSetsUpdated(sets)
                }
            }
    }

    suspend fun updateBookSetStatus(setId: String, status: SetStatus) {
        try {
            firestore.collection("bookSets").document(setId)
                .update("status", status.name)
                .await()
        } catch (e: Exception) {
            Log.e(tag, "Failed to update bookSet status", e)
        }
    }

    suspend fun deleteBookSet(setId: String) {
        try {
            firestore.collection("bookSets").document(setId).delete().await()
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete bookSet", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun docToBookSet(doc: DocumentSnapshot): BookSet? {
        val id = doc.getString("id") ?: doc.id
        val sellerId = doc.getString("sellerId") ?: return null
        val title = doc.getString("title") ?: return null
        val sellerName = doc.getString("sellerName") ?: "Student"
        val sellerCollege = doc.getString("sellerCollege") ?: "Feni Polytechnic"
        val sellerTech = doc.getString("sellerTech") ?: "Civil"
        val sellerSem = doc.getString("sellerSem") ?: "1st"
        val sellerPhone = doc.getString("sellerPhone") ?: ""
        val sellerEmail = doc.getString("sellerEmail") ?: ""
        val subject = doc.getString("subject") ?: ""
        val techId = doc.getString("technologyId") ?: "civil"
        val semId = doc.getString("semesterId") ?: "1"
        val college = doc.getString("college") ?: "Feni Polytechnic Institute"
        val totalPrice = (doc.getLong("totalPrice") ?: 0L).toInt()
        val conditionStr = doc.getString("overallCondition") ?: "GOOD"
        val condition = try { SetCondition.valueOf(conditionStr) } catch (_: Exception) { SetCondition.GOOD }
        val description = doc.getString("description") ?: ""
        val imageUrls = (doc.get("imageUrls") as? List<String>) ?: emptyList()
        val statusStr = doc.getString("status") ?: "AVAILABLE"
        val status = try { SetStatus.valueOf(statusStr) } catch (_: Exception) { SetStatus.AVAILABLE }
        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

        val rawBooks = doc.get("includedBooks") as? List<Map<String, Any>> ?: emptyList()
        val includedBooks = rawBooks.map { bMap ->
            IncludedBook(
                id = bMap["id"] as? String ?: UUID.randomUUID().toString(),
                setId = id,
                bookName = bMap["bookName"] as? String ?: "",
                subject = bMap["subject"] as? String ?: "",
                author = bMap["author"] as? String ?: "",
                edition = bMap["edition"] as? String ?: "",
                bookCode = bMap["bookCode"] as? String ?: "",
                condition = bMap["condition"] as? String ?: "Good"
            )
        }

        return BookSet(
            id = id,
            sellerId = sellerId,
            sellerName = sellerName,
            sellerCollege = sellerCollege,
            sellerTech = sellerTech,
            sellerSem = sellerSem,
            sellerPhone = sellerPhone,
            sellerEmail = sellerEmail,
            title = title,
            subject = subject,
            technology = Technology.fromId(techId),
            semester = Semester.fromId(semId),
            college = college,
            totalPrice = totalPrice,
            overallCondition = condition,
            description = description,
            includedBooks = includedBooks,
            coverDrawableRes = null,
            imageUrls = imageUrls,
            status = status,
            isSaved = false,
            createdAt = createdAt
        )
    }

    // -------------------------------------------------------------
    // CONVERSATIONS & CHAT MESSAGES (/conversations/{id}/messages)
    // -------------------------------------------------------------

    suspend fun saveConversation(conv: Conversation) {
        try {
            val map = hashMapOf(
                "id" to conv.id,
                "buyerId" to conv.buyerId,
                "sellerId" to conv.sellerId,
                "participantUids" to listOf(conv.buyerId, conv.sellerId),
                "otherUserName" to conv.otherUserName,
                "otherUserCollege" to conv.otherUserCollege,
                "setId" to conv.setId,
                "setTitle" to conv.setTitle,
                "setPrice" to conv.setPrice,
                "lastMessage" to conv.lastMessage,
                "timestamp" to conv.timestamp,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("conversations").document(conv.id)
                .set(map, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(tag, "Failed to save conversation", e)
        }
    }

    fun listenConversations(userId: String, onConversationsUpdated: (List<Conversation>) -> Unit): ListenerRegistration? {
        if (userId.isBlank()) {
            return null
        }
        val isSuperAdmin = userId == "admin_master_1" || userId.contains("admin", ignoreCase = true)

        val query: Query = if (isSuperAdmin) {
            firestore.collection("conversations")
        } else {
            firestore.collection("conversations").whereArrayContains("participantUids", userId)
        }

        return query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(tag, "Error listening to conversations", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val id = doc.getString("id") ?: doc.id
                    val buyerId = doc.getString("buyerId") ?: ""
                    val sellerId = doc.getString("sellerId") ?: ""

                    Conversation(
                        id = id,
                        buyerId = buyerId,
                        sellerId = sellerId,
                        otherUserName = doc.getString("otherUserName") ?: "Student",
                        otherUserCollege = doc.getString("otherUserCollege") ?: "Feni Polytechnic",
                        otherUserAvatarRes = null,
                        setId = doc.getString("setId") ?: "",
                        setTitle = doc.getString("setTitle") ?: "",
                        setPrice = (doc.getLong("setPrice") ?: 0L).toInt(),
                        lastMessage = doc.getString("lastMessage") ?: "",
                        timestamp = doc.getString("timestamp") ?: "Just now",
                        unreadCount = 0,
                        isOnline = true,
                        messages = emptyList()
                    )
                }
                onConversationsUpdated(list)
            }
        }
    }

    suspend fun sendMessage(conversationId: String, message: ChatMessage) {
        try {
            val msgMap = hashMapOf(
                "id" to message.id,
                "senderId" to message.senderId,
                "senderName" to message.senderName,
                "text" to message.text,
                "timestamp" to message.timestamp,
                "createdAt" to System.currentTimeMillis()
            )

            // 1. Add message to subcollection
            firestore.collection("conversations").document(conversationId)
                .collection("messages").document(message.id)
                .set(msgMap)
                .await()

            // 2. Update conversation last message
            firestore.collection("conversations").document(conversationId)
                .update(
                    mapOf(
                        "lastMessage" to message.text,
                        "timestamp" to message.timestamp,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Log.d(tag, "Message sent to Firestore: ${message.id} in $conversationId")
        } catch (e: Exception) {
            Log.e(tag, "Failed to send message", e)
            throw e
        }
    }

    fun listenMessages(
        conversationId: String,
        currentUserId: String,
        onMessagesUpdated: (List<ChatMessage>) -> Unit
    ): ListenerRegistration {
        return firestore.collection("conversations").document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error listening to messages for $conversationId", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        val id = doc.getString("id") ?: doc.id
                        val senderId = doc.getString("senderId") ?: return@mapNotNull null
                        val senderName = doc.getString("senderName") ?: "Student"
                        val text = doc.getString("text") ?: ""
                        val timestamp = doc.getString("timestamp") ?: ""
                        val isMe = senderId == currentUserId

                        ChatMessage(
                            id = id,
                            senderId = senderId,
                            senderName = senderName,
                            text = text,
                            timestamp = timestamp,
                            isMe = isMe
                        )
                    }
                    Log.d(tag, "Live messages received for $conversationId: ${messages.size}")
                    onMessagesUpdated(messages)
                }
            }
    }

    // -------------------------------------------------------------
    // MEGA DEALS COLLECTION (/megaDeals)
    // -------------------------------------------------------------

    suspend fun saveMegaDeal(deal: MegaDeal) {
        try {
            val map = hashMapOf(
                "dealId" to deal.dealId,
                "setId" to deal.setId,
                "setTitle" to deal.setTitle,
                "buyerId" to deal.buyerId,
                "buyerName" to deal.buyerName,
                "sellerId" to deal.sellerId,
                "sellerName" to deal.sellerName,
                "finalPrice" to deal.finalPrice,
                "location" to deal.location,
                "date" to deal.date,
                "time" to deal.time,
                "status" to deal.status.name,
                "createdAt" to deal.createdAt
            )
            firestore.collection("megaDeals").document(deal.dealId)
                .set(map, SetOptions.merge())
                .await()
            Log.d(tag, "MegaDeal saved to Firestore: ${deal.dealId}")
        } catch (e: Exception) {
            Log.e(tag, "Failed to save MegaDeal", e)
        }
    }

    fun listenAllMegaDeals(onDealsUpdated: (List<MegaDeal>) -> Unit): ListenerRegistration {
        return firestore.collection("megaDeals")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error listening to mega deals", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val deals = snapshot.documents.mapNotNull { doc ->
                        val dealId = doc.getString("dealId") ?: doc.id
                        val setId = doc.getString("setId") ?: return@mapNotNull null
                        val setTitle = doc.getString("setTitle") ?: ""
                        val buyerId = doc.getString("buyerId") ?: ""
                        val buyerName = doc.getString("buyerName") ?: ""
                        val sellerId = doc.getString("sellerId") ?: ""
                        val sellerName = doc.getString("sellerName") ?: ""
                        val finalPrice = (doc.getLong("finalPrice") ?: 0L).toInt()
                        val location = doc.getString("location") ?: ""
                        val date = doc.getString("date") ?: ""
                        val time = doc.getString("time") ?: ""
                        val statusStr = doc.getString("status") ?: "PENDING"
                        val status = try { MegaDealStatus.valueOf(statusStr) } catch (_: Exception) { MegaDealStatus.PENDING }
                        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                        MegaDeal(
                            dealId = dealId,
                            setId = setId,
                            setTitle = setTitle,
                            buyerId = buyerId,
                            buyerName = buyerName,
                            sellerId = sellerId,
                            sellerName = sellerName,
                            finalPrice = finalPrice,
                            location = location,
                            date = date,
                            time = time,
                            status = status,
                            createdAt = createdAt
                        )
                    }
                    onDealsUpdated(deals)
                }
            }
    }

    suspend fun updateMegaDealStatus(dealId: String, status: MegaDealStatus) {
        try {
            firestore.collection("megaDeals").document(dealId)
                .update("status", status.name)
                .await()
        } catch (e: Exception) {
            Log.e(tag, "Failed to update mega deal status", e)
        }
    }
}
