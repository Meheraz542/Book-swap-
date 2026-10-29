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
                    val apiKey = com.example.BuildConfig.GOOGLE_API_KEY.ifEmpty { "" }
                    val options = com.google.firebase.FirebaseOptions.Builder()
                        .setApplicationId("1:986504877917:android:1f76f3f3338ce3a1eabab4")
                        .setApiKey(apiKey)
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

    suspend fun saveUserProfile(user: UserProfile, passwordHash: String? = null) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
        try {
            val userMap = hashMapOf<String, Any?>(
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
            if (!passwordHash.isNullOrBlank()) {
                userMap["passwordHash"] = passwordHash
            }
            firestore.collection("users").document(user.id)
                .set(userMap, SetOptions.merge())
                .await()
            Log.d(tag, "User saved to Firestore: ${user.id} (${user.email})")
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) {
                Log.d(tag, "Profile save coroutine cancelled cleanly on navigation: ${e.message}")
            } else {
                Log.w(tag, "Failed to save user profile: ${e.message}")
            }
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
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
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

    fun listenUserProfile(userId: String, onProfileUpdated: (UserProfile) -> Unit): ListenerRegistration? {
        if (userId.isBlank()) return null
        return firestore.collection("users").document(userId)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error listening to user profile: $userId", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val profile = docToUserProfile(snapshot)
                    if (profile != null) {
                        onProfileUpdated(profile)
                    }
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
                    "condition" to book.condition,
                    "imageUris" to book.imageUris
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

    suspend fun fetchBookSetsOnce(forceServer: Boolean = true): List<BookSet> {
        return try {
            val snapshot = if (forceServer) {
                try {
                    firestore.collection("bookSets")
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .get(com.google.firebase.firestore.Source.SERVER)
                        .await()
                } catch (ex: Exception) {
                    Log.w(tag, "Server fetch failed, falling back to cache/default: ${ex.message}")
                    firestore.collection("bookSets")
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .get()
                        .await()
                }
            } else {
                firestore.collection("bookSets")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
            }
            val sets = snapshot.documents.mapNotNull { docToBookSet(it) }
            Log.d(tag, "Fetched ${sets.size} book sets from Firestore (forceServer=$forceServer)")
            sets
        } catch (e: Exception) {
            Log.e(tag, "Failed to fetch book sets once", e)
            emptyList()
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
                condition = bMap["condition"] as? String ?: "Good",
                imageUris = (bMap["imageUris"] as? List<String>) ?: emptyList()
            )
        }

        val parsedTech = Technology.fromId(techId)
        val defaultCoverRes = when (parsedTech) {
            Technology.CIVIL, Technology.AIDT -> R.drawable.book_cover_civil_1789677775416
            Technology.ELECTRICAL, Technology.TELECOM -> R.drawable.book_cover_elec_1789677789123
            Technology.MECHANICAL, Technology.POWER -> R.drawable.book_cover_mech_1789677808779
            Technology.CSE -> R.drawable.book_cover_cse_1789735549141
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
            technology = parsedTech,
            semester = Semester.fromId(semId),
            college = college,
            totalPrice = totalPrice,
            overallCondition = condition,
            description = description,
            includedBooks = includedBooks,
            coverDrawableRes = defaultCoverRes,
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
                "otherUserAvatarUri" to (conv.otherUserAvatarUri ?: ""),
                "buyerName" to conv.buyerName,
                "buyerCollege" to conv.buyerCollege,
                "buyerAvatarUri" to (conv.buyerAvatarUri ?: ""),
                "sellerName" to conv.sellerName,
                "sellerCollege" to conv.sellerCollege,
                "sellerAvatarUri" to (conv.sellerAvatarUri ?: ""),
                "setId" to conv.setId,
                "setTitle" to conv.setTitle,
                "setPrice" to conv.setPrice,
                "lastMessage" to conv.lastMessage,
                "timestamp" to conv.timestamp,
                "deletedByUserIds" to conv.deletedByUserIds,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("conversations").document(conv.id)
                .set(map, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(tag, "Failed to save conversation", e)
        }
    }

    fun listenConversations(
        userId: String,
        onConversationsUpdated: (List<Conversation>) -> Unit,
        onNewMessageAlert: ((InAppMessageAlert) -> Unit)? = null
    ): ListenerRegistration? {
        if (userId.isBlank()) {
            return null
        }
        val isSuperAdmin = userId == "admin_master_1" || userId.contains("admin", ignoreCase = true)

        val query: Query = if (isSuperAdmin) {
            firestore.collection("conversations")
        } else {
            firestore.collection("conversations").whereArrayContains("participantUids", userId)
        }

        var isInitialLoad = true

        return query.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) {
                Log.e(tag, "Error listening to conversations", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                // Real-time alert detection for new incoming messages from other participants
                if (!isInitialLoad) {
                    for (change in snapshot.documentChanges) {
                        if (change.type == DocumentChange.Type.MODIFIED || change.type == DocumentChange.Type.ADDED) {
                            val doc = change.document
                            val lastSenderId = doc.getString("lastSenderId") ?: ""
                            val lastMessage = doc.getString("lastMessage") ?: ""
                            val hasPendingWrites = doc.metadata.hasPendingWrites()
                            val isFromMe = lastSenderId == userId || lastSenderId.isBlank()

                            if (!hasPendingWrites && !isFromMe && lastMessage.isNotBlank()) {
                                val convId = doc.getString("id") ?: doc.id
                                val buyerId = doc.getString("buyerId") ?: ""
                                val sellerId = doc.getString("sellerId") ?: ""
                                val buyerName = doc.getString("buyerName") ?: ""
                                val sellerName = doc.getString("sellerName") ?: ""
                                val buyerAvatarUri = doc.getString("buyerAvatarUri")?.takeIf { it.isNotBlank() }
                                val sellerAvatarUri = doc.getString("sellerAvatarUri")?.takeIf { it.isNotBlank() }
                                val buyerCollege = doc.getString("buyerCollege") ?: ""
                                val sellerCollege = doc.getString("sellerCollege") ?: ""

                                val senderName = doc.getString("lastSenderName")
                                    ?.takeIf { it.isNotBlank() }
                                    ?: (if (lastSenderId == buyerId) buyerName else sellerName).ifBlank { "Student" }

                                val senderAvatarUri = doc.getString("lastSenderAvatarUri")
                                    ?.takeIf { it.isNotBlank() }
                                    ?: (if (lastSenderId == buyerId) buyerAvatarUri else sellerAvatarUri)

                                val senderCollege = if (lastSenderId == buyerId) buyerCollege else sellerCollege

                                val alert = InAppMessageAlert(
                                    conversationId = convId,
                                    senderId = lastSenderId,
                                    senderName = senderName,
                                    senderAvatarUri = senderAvatarUri,
                                    senderCollege = senderCollege,
                                    messageText = lastMessage,
                                    timestamp = doc.getString("timestamp") ?: "Just now"
                                )
                                Log.d(tag, "🔔 Real-time incoming message alert detected from $senderName in $convId: '$lastMessage'")
                                onNewMessageAlert?.invoke(alert)
                            }
                        }
                    }
                }
                isInitialLoad = false

                val list = snapshot.documents.mapNotNull { doc ->
                    val id = doc.getString("id") ?: doc.id
                    val buyerId = doc.getString("buyerId") ?: ""
                    val sellerId = doc.getString("sellerId") ?: ""
                    val buyerName = doc.getString("buyerName") ?: ""
                    val buyerCollege = doc.getString("buyerCollege") ?: ""
                    val buyerAvatarUri = doc.getString("buyerAvatarUri")?.takeIf { it.isNotBlank() }
                    val sellerName = doc.getString("sellerName") ?: ""
                    val sellerCollege = doc.getString("sellerCollege") ?: ""
                    val sellerAvatarUri = doc.getString("sellerAvatarUri")?.takeIf { it.isNotBlank() }
                    val storedOtherName = doc.getString("otherUserName") ?: "Student"
                    val storedOtherCollege = doc.getString("otherUserCollege") ?: "Feni Polytechnic"
                    val storedOtherAvatar = doc.getString("otherUserAvatarUri")?.takeIf { it.isNotBlank() }

                    val resolvedOtherName = if (userId == sellerId) {
                        buyerName.ifBlank { storedOtherName }
                    } else {
                        sellerName.ifBlank { storedOtherName }
                    }

                    val resolvedOtherCollege = if (userId == sellerId) {
                        buyerCollege.ifBlank { storedOtherCollege }
                    } else {
                        sellerCollege.ifBlank { storedOtherCollege }
                    }

                    val resolvedOtherAvatarUri = if (userId == sellerId) {
                        buyerAvatarUri ?: storedOtherAvatar
                    } else {
                        sellerAvatarUri ?: storedOtherAvatar
                    }

                    val rawUpdatedAt = when (val uVal = doc.get("updatedAt")) {
                        is Long -> uVal
                        is Number -> uVal.toLong()
                        is com.google.firebase.Timestamp -> uVal.toDate().time
                        else -> 0L
                    }
                    val hasPendingWrites = doc.metadata.hasPendingWrites()
                    val updatedAt = if (rawUpdatedAt == 0L && hasPendingWrites) {
                        System.currentTimeMillis()
                    } else {
                        rawUpdatedAt
                    }

                    val unreadForUser = (doc.getLong("unreadCount_$userId") ?: doc.getLong("unreadCount") ?: 0L).toInt()
                    val lastSenderId = doc.getString("lastSenderId") ?: ""
                    val finalUnread = if (lastSenderId == userId) 0 else unreadForUser
                    val deletedByUserIds = (doc.get("deletedByUserIds") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

                    Conversation(
                        id = id,
                        buyerId = buyerId,
                        sellerId = sellerId,
                        otherUserName = resolvedOtherName,
                        otherUserCollege = resolvedOtherCollege,
                        otherUserAvatarRes = null,
                        otherUserAvatarUri = resolvedOtherAvatarUri,
                        buyerName = buyerName,
                        buyerCollege = buyerCollege,
                        buyerAvatarUri = buyerAvatarUri,
                        sellerName = sellerName,
                        sellerCollege = sellerCollege,
                        sellerAvatarUri = sellerAvatarUri,
                        setId = doc.getString("setId") ?: "",
                        setTitle = doc.getString("setTitle") ?: "",
                        setPrice = (doc.getLong("setPrice") ?: 0L).toInt(),
                        lastMessage = doc.getString("lastMessage") ?: "",
                        timestamp = doc.getString("timestamp") ?: "Just now",
                        unreadCount = finalUnread,
                        isOnline = true,
                        messages = emptyList(),
                        updatedAt = updatedAt,
                        deletedByUserIds = deletedByUserIds
                    )
                }.sortedByDescending { it.updatedAt }
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
                "senderAvatarUri" to (message.senderAvatarUri ?: ""),
                "createdAt" to System.currentTimeMillis()
            )

            val convDoc = try {
                firestore.collection("conversations").document(conversationId).get().await()
            } catch (_: Exception) { null }

            val buyerId = convDoc?.getString("buyerId") ?: ""
            val sellerId = convDoc?.getString("sellerId") ?: ""
            val recipientId = if (message.senderId == buyerId) sellerId else buyerId

            val updateMap = mutableMapOf<String, Any>(
                "lastMessage" to message.text,
                "timestamp" to message.timestamp,
                "lastSenderId" to message.senderId,
                "lastSenderName" to message.senderName,
                "lastSenderAvatarUri" to (message.senderAvatarUri ?: ""),
                "deletedByUserIds" to emptyList<String>(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (recipientId.isNotBlank()) {
                updateMap["unreadCount_$recipientId"] = FieldValue.increment(1)
            }
            updateMap["unreadCount"] = FieldValue.increment(1)

            // 1. Add message to subcollection
            firestore.collection("conversations").document(conversationId)
                .collection("messages").document(message.id)
                .set(msgMap)
                .await()

            // 2. Update conversation last message & unread state
            firestore.collection("conversations").document(conversationId)
                .update(updateMap)
                .await()
            Log.d(tag, "Message sent to Firestore: ${message.id} in $conversationId (recipient: $recipientId)")
        } catch (e: Exception) {
            Log.e(tag, "Failed to send message", e)
            throw e
        }
    }

    suspend fun markConversationDeletedForUser(conversationId: String, userId: String) {
        if (conversationId.isBlank() || userId.isBlank()) return
        try {
            firestore.collection("conversations").document(conversationId)
                .update("deletedByUserIds", FieldValue.arrayUnion(userId))
                .await()
            Log.d(tag, "Marked conversation $conversationId deleted for user $userId in Firestore")
        } catch (e: Exception) {
            Log.w(tag, "markConversationDeletedForUser failed: ${e.message}")
        }
    }

    suspend fun markConversationRead(conversationId: String, userId: String) {
        try {
            val updateMap = mutableMapOf<String, Any>(
                "unreadCount" to 0
            )
            if (userId.isNotBlank()) {
                updateMap["unreadCount_$userId"] = 0
            }
            firestore.collection("conversations").document(conversationId)
                .update(updateMap)
                .await()
            Log.d(tag, "Marked conversation read on Firestore: $conversationId for user $userId")
        } catch (e: Exception) {
            Log.w(tag, "Failed to mark conversation read on Firestore: ${e.message}")
        }
    }

    /**
     * Detailed logs handler and diagnostic tracer for Firestore message listeners.
     */
    object FirestoreMessageTraceLogger {
        private const val TRACE_TAG = "FirestoreMessageTrace"

        fun logSnapshotEvent(
            conversationId: String,
            snapshot: QuerySnapshot?,
            error: FirebaseFirestoreException?
        ) {
            if (error != null) {
                Log.e(TRACE_TAG, "❌ [ERROR] SnapshotListener failure for conversation '$conversationId': code=${error.code}, message='${error.message}'", error)
                return
            }
            if (snapshot == null) {
                Log.w(TRACE_TAG, "⚠️ [WARN] Snapshot payload is null for conversation '$conversationId'")
                return
            }
            val isFromCache = snapshot.metadata.isFromCache
            val hasPendingWrites = snapshot.metadata.hasPendingWrites()
            val totalDocs = snapshot.documents.size
            val changes = snapshot.documentChanges

            Log.d(
                TRACE_TAG,
                "📡 [SNAPSHOT RECEIVED] Conversation: '$conversationId' | TotalDocs: $totalDocs | Changes: ${changes.size} | isFromCache: $isFromCache | hasPendingWrites: $hasPendingWrites"
            )

            for (change in changes) {
                val doc = change.document
                val type = change.type // ADDED, MODIFIED, REMOVED
                val docId = doc.id
                val senderId = doc.getString("senderId") ?: doc.getString("sender_id") ?: doc.getString("userId") ?: "UNKNOWN"
                val textSnippet = (doc.getString("text") ?: doc.getString("message") ?: "").take(35)
                val createdAtVal = doc.get("createdAt")

                when (type) {
                    DocumentChange.Type.ADDED -> {
                        Log.d(TRACE_TAG, "  🟢 [MSG ADDED] id='$docId', sender='$senderId', text='$textSnippet...', createdAt=$createdAtVal")
                    }
                    DocumentChange.Type.MODIFIED -> {
                        Log.d(TRACE_TAG, "  🟡 [MSG MODIFIED] id='$docId', sender='$senderId', text='$textSnippet...', createdAt=$createdAtVal")
                    }
                    DocumentChange.Type.REMOVED -> {
                        Log.w(TRACE_TAG, "  🚨 [MSG DISAPPEARED / REMOVED FROM QUERY] id='$docId', sender='$senderId' was deleted or excluded from query!")
                    }
                }
            }
        }

        fun logMappingSummary(
            conversationId: String,
            totalRawDocs: Int,
            mappedCount: Int,
            droppedDocs: List<String>
        ) {
            if (droppedDocs.isNotEmpty()) {
                Log.w(
                    TRACE_TAG,
                    "⚠️ [MAPPING WARNING] Conv '$conversationId': mapped $mappedCount/$totalRawDocs messages. Dropped document IDs (missing essential content): $droppedDocs"
                )
            } else {
                Log.d(
                    TRACE_TAG,
                    "✅ [MAPPING SUCCESS] Conv '$conversationId': successfully mapped all $mappedCount messages from Firestore documents."
                )
            }
        }
    }

    fun listenMessages(
        conversationId: String,
        currentUserId: String,
        onMessagesUpdated: (List<ChatMessage>) -> Unit
    ): ListenerRegistration {
        Log.d(tag, "Attaching Firestore SnapshotListener for conversation messages: $conversationId (user: $currentUserId)")
        return firestore.collection("conversations").document(conversationId)
            .collection("messages")
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                FirestoreMessageTraceLogger.logSnapshotEvent(conversationId, snapshot, error)

                if (error != null) {
                    Log.e(tag, "Error listening to messages for $conversationId", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val droppedDocIds = mutableListOf<String>()
                    val messages = snapshot.documents.mapNotNull { doc ->
                        val id = doc.getString("id") ?: doc.id

                        // Robust multi-key resolution for sender ID to prevent dropping valid messages
                        val senderId = doc.getString("senderId")
                            ?: doc.getString("sender_id")
                            ?: doc.getString("userId")
                            ?: doc.getString("authorId")
                            ?: doc.getString("fromId")
                            ?: ""

                        val text = doc.getString("text")
                            ?: doc.getString("message")
                            ?: doc.getString("content")
                            ?: ""

                        // Only drop if both senderId and text are completely missing or invalid
                        if (senderId.isBlank() && text.isBlank()) {
                            Log.w(tag, "Dropping empty message document $id with data: ${doc.data}")
                            droppedDocIds.add(id)
                            return@mapNotNull null
                        }

                        val senderName = doc.getString("senderName")
                            ?: doc.getString("sender_name")
                            ?: doc.getString("authorName")
                            ?: "Student"

                        val timestamp = doc.getString("timestamp")
                            ?: doc.getString("time")
                            ?: "Just now"

                        val senderAvatarUri = doc.getString("senderAvatarUri")?.takeIf { it.isNotBlank() }
                            ?: doc.getString("avatarUri")?.takeIf { it.isNotBlank() }
                            ?: doc.getString("sender_avatar")?.takeIf { it.isNotBlank() }

                        val isMe = senderId == currentUserId
                        val hasPendingWrites = doc.metadata.hasPendingWrites()

                        // Extract creation timestamp for reliable in-memory chronological ordering
                        val rawCreatedAt = when (val cVal = doc.get("createdAt")) {
                            is Long -> cVal
                            is Number -> cVal.toLong()
                            is com.google.firebase.Timestamp -> cVal.toDate().time
                            else -> 0L
                        }
                        val createdAt = if (rawCreatedAt == 0L && hasPendingWrites) {
                            System.currentTimeMillis()
                        } else {
                            rawCreatedAt
                        }

                        ChatMessage(
                            id = id,
                            senderId = senderId.ifBlank { "unknown_sender" },
                            senderName = senderName,
                            text = text,
                            timestamp = timestamp,
                            isMe = isMe,
                            senderAvatarUri = senderAvatarUri,
                            senderAvatarRes = null,
                            isPending = hasPendingWrites
                        ) to createdAt
                    }

                    // Sort chronologically using creation timestamp or document order
                    val sortedMessages = messages.sortedWith(
                        compareBy<Pair<ChatMessage, Long>> { it.second }
                            .thenBy { it.first.timestamp }
                            .thenBy { it.first.id }
                    ).map { it.first }

                    FirestoreMessageTraceLogger.logMappingSummary(
                        conversationId = conversationId,
                        totalRawDocs = snapshot.documents.size,
                        mappedCount = sortedMessages.size,
                        droppedDocs = droppedDocIds
                    )

                    onMessagesUpdated(sortedMessages)
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
