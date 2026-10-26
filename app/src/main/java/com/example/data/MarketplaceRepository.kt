package com.example.data

import android.content.Context
import com.example.R
import com.example.data.auth.AuthManager
import com.example.data.auth.AuthResult
import com.example.data.auth.EmailOtpManager
import com.example.data.auth.OtpResult
import com.example.data.auth.PendingRegistration
import com.example.data.local.AppDatabase
import com.example.data.local.entities.*
import com.example.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import android.util.Log
import com.example.data.remote.FirebaseFirestoreManager

object MarketplaceRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var database: AppDatabase? = null
    private var authManager: AuthManager? = null
    private var emailOtpManager: EmailOtpManager? = null
    var firestoreManager: FirebaseFirestoreManager? = null

    // Real-time Gmail OTP State
    private val _latestOtpCode = MutableStateFlow<String?>(null)
    val latestOtpCode: StateFlow<String?> = _latestOtpCode.asStateFlow()

    private val _pendingOtpEmail = MutableStateFlow<String?>(null)
    val pendingOtpEmail: StateFlow<String?> = _pendingOtpEmail.asStateFlow()

    // Default Super Admin profile for seamless testing & initial state
    private val emptyGuestProfile = UserProfile(
        id = "",
        name = "Student",
        email = "",
        phone = "",
        college = "Feni Polytechnic Institute",
        technology = Technology.CIVIL,
        currentSemester = Semester.SEM_1,
        profilePhotoRes = null,
        role = "USER",
        accountStatus = "ACTIVE"
    )

    val defaultAdminProfile = UserProfile(
        id = "admin_master_1",
        name = "Super Admin",
        email = "mdmaheraz65@gmail.com",
        phone = "01712-345678",
        college = "Feni Computer Institute",
        technology = Technology.CSE,
        currentSemester = Semester.SEM_3,
        profilePhotoRes = null,
        role = "ADMIN",
        accountStatus = "ACTIVE"
    )

    // Current User profile (default admin profile for test harness, but unauthenticated until login)
    private val _currentUser = MutableStateFlow(defaultAdminProfile)
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    // Logged in state: check Firebase Authentication directly on app start
    private val _isLoggedIn = MutableStateFlow(
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null
        } catch (e: Exception) {
            false
        }
    )
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // All Book Sets
    private val _bookSets = MutableStateFlow<List<BookSet>>(emptyList())
    val bookSets: StateFlow<List<BookSet>> = _bookSets.asStateFlow()

    // Saved Sets
    private val _savedSetIds = MutableStateFlow<Set<String>>(emptySet())
    val savedSetIds: StateFlow<Set<String>> = _savedSetIds.asStateFlow()

    // Conversations
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    // Mega Deals
    private val _megaDeals = MutableStateFlow<List<MegaDeal>>(emptyList())
    val megaDeals: StateFlow<List<MegaDeal>> = _megaDeals.asStateFlow()

    // Notifications
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    // Real-Time In-App Visual Alert Banner
    private val _activeInAppAlert = MutableStateFlow<InAppMessageAlert?>(null)
    val activeInAppAlert: StateFlow<InAppMessageAlert?> = _activeInAppAlert.asStateFlow()

    fun triggerInAppAlert(alert: InAppMessageAlert) {
        _activeInAppAlert.value = alert
    }

    fun dismissInAppAlert() {
        _activeInAppAlert.value = null
    }

    // Currently focused chat conversation ID to prevent redundant alerts when actively viewing chat
    private val _activeChatConversationId = MutableStateFlow<String?>(null)
    val activeChatConversationId: StateFlow<String?> = _activeChatConversationId.asStateFlow()

    fun setActiveChatConversationId(id: String?) {
        _activeChatConversationId.value = id
    }

    // All Registered Users for Admin panel
    private val _allUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val allUsers: StateFlow<List<UserProfile>> = _allUsers.asStateFlow()

    // App Traffic & Analytics Stats for Admin Panel
    private val _trafficStats = MutableStateFlow(AppTrafficStats())
    val trafficStats: StateFlow<AppTrafficStats> = _trafficStats.asStateFlow()

    // Single source of truth in-memory cache for user profiles across messaging and app
    private val _userProfileCache = java.util.concurrent.ConcurrentHashMap<String, UserProfile>()

    init {
        // Pristine clean initial state - no demo data
        _allUsers.value = listOf(defaultAdminProfile)
        _userProfileCache[defaultAdminProfile.id] = defaultAdminProfile
    }

    /**
     * Single Source of Truth for fetching user profiles asynchronously in the messaging module.
     * Evaluates in-memory cache -> current user -> allUsers -> local Room DB -> remote Cloud Firestore.
     */
    suspend fun getUserProfileAsync(userId: String): UserProfile? = withContext(Dispatchers.IO) {
        if (userId.isBlank() || userId == "unknown_sender") return@withContext null

        // 1. In-memory profile cache
        _userProfileCache[userId]?.let { return@withContext it }

        // 2. Active session user
        val activeUser = _currentUser.value
        if (activeUser.id == userId && activeUser.id.isNotBlank()) {
            _userProfileCache[userId] = activeUser
            return@withContext activeUser
        }

        // 3. Registered users directory
        _allUsers.value.find { it.id == userId }?.let {
            _userProfileCache[userId] = it
            return@withContext it
        }

        // 4. Local Room SQLite Database
        val db = database
        if (db != null) {
            val dbUser = db.userDao().getUserById(userId)
            if (dbUser != null) {
                val profile = dbUser.toDomain()
                _userProfileCache[userId] = profile
                return@withContext profile
            }
        }

        // 5. Remote Cloud Firestore Fetch
        val fs = firestoreManager
        if (fs != null) {
            try {
                val remoteProfile = fs.getUserProfile(userId)
                if (remoteProfile != null) {
                    _userProfileCache[userId] = remoteProfile
                    db?.userDao()?.insertUser(remoteProfile.toEntity())
                    return@withContext remoteProfile
                }
            } catch (e: Exception) {
                Log.w("MarketplaceRepo", "Remote profile lookup failed for $userId: ${e.message}")
            }
        }

        null
    }

    /**
     * Synchronous immediate profile lookup from cached single source of truth.
     */
    fun getCachedUserProfile(userId: String): UserProfile? {
        if (userId.isBlank()) return null
        if (_currentUser.value.id == userId && _currentUser.value.id.isNotBlank()) return _currentUser.value
        return _userProfileCache[userId] ?: _allUsers.value.find { it.id == userId }
    }

    fun initialize(context: Context) {
        val db = AppDatabase.getInstance(context)
        database = db
        val firestore = FirebaseFirestoreManager(context)
        firestoreManager = firestore
        val auth = AuthManager(context, firestore)
        authManager = auth
        emailOtpManager = EmailOtpManager(context.applicationContext)

        // Attach persistent Firebase Auth State Listener to ensure user sessions remain active
        // across app restarts and consistently propagate authentication state to the repository layer.
        try {
            val firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
            firebaseAuth.addAuthStateListener { authInstance ->
                val fbUser = authInstance.currentUser
                Log.d(
                    "MarketplaceRepo",
                    "FirebaseAuth.AuthStateListener invoked: uid=${fbUser?.uid}, email=${fbUser?.email}"
                )
                scope.launch(Dispatchers.IO) {
                    val currentDb = database ?: return@launch
                    if (fbUser != null) {
                        val email = fbUser.email?.trim()?.lowercase() ?: ""
                        val existing = if (email.isNotEmpty()) {
                            currentDb.userDao().getUserByEmail(email)
                        } else {
                            currentDb.userDao().getUserById(fbUser.uid)
                        }

                        val userEntity = if (existing != null) {
                            currentDb.userDao().switchActiveSession(existing.id)
                            existing
                        } else {
                            val isAdmin = email.equals("mdmaheraz65@gmail.com", ignoreCase = true)
                            val newEntity = UserEntity(
                                id = fbUser.uid,
                                name = fbUser.displayName ?: if (isAdmin) "Super Admin" else "Student User",
                                email = email,
                                passwordHash = "",
                                phone = "01712-345678",
                                college = if (isAdmin) "Feni Computer Institute" else "Feni Polytechnic Institute",
                                technologyId = if (isAdmin) Technology.CSE.id else Technology.CIVIL.id,
                                semesterId = if (isAdmin) Semester.SEM_3.id else Semester.SEM_1.id,
                                profilePhotoRes = null,
                                profilePhotoUri = fbUser.photoUrl?.toString(),
                                role = if (isAdmin) "ADMIN" else "USER",
                                accountStatus = "ACTIVE",
                                isCurrentSession = true
                            )
                            currentDb.userDao().insertUser(newEntity)
                            currentDb.userDao().switchActiveSession(newEntity.id)
                            newEntity
                        }
                        val domainProfile = userEntity.toDomain()
                        _currentUser.value = domainProfile
                        _isLoggedIn.value = true
                        _userProfileCache[domainProfile.id] = domainProfile
                        Log.d("MarketplaceRepo", "Propagated active session to repo for ${domainProfile.id} (${domainProfile.email})")

                        try {
                            firestoreManager?.saveUserProfile(domainProfile)
                        } catch (e: Exception) {
                            Log.d("MarketplaceRepo", "Async profile sync note on auth listener: ${e.message}")
                        }
                    } else {
                        // When Firebase Auth has no user, check if local Room session was stored
                        val currentLocal = currentDb.userDao().getCurrentUser()
                        if (currentLocal != null && currentLocal.isCurrentSession) {
                            val localDomain = currentLocal.toDomain()
                            _currentUser.value = localDomain
                            _isLoggedIn.value = true
                            _userProfileCache[localDomain.id] = localDomain
                            Log.d("MarketplaceRepo", "Restored local session from Room: ${localDomain.id}")
                        } else {
                            _isLoggedIn.value = false
                            Log.d("MarketplaceRepo", "No active session detected across Firebase Auth and Room.")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MarketplaceRepo", "Failed to register FirebaseAuth AuthStateListener", e)
        }

        // Real-Time Cloud Firestore Listeners for cross-device synchronization
        try {
            // Public catalog listeners (allowed by firestore.rules)
            firestore.listenAllBookSets { firestoreSets ->
                _bookSets.value = firestoreSets
                Log.d("MarketplaceRepo", "Synced ${firestoreSets.size} bookSets from Firestore in real-time")
                database?.let { db ->
                    scope.launch(Dispatchers.IO) {
                        for (set in firestoreSets) {
                            db.bookSetDao().insertBookSetWithBooks(
                                set.toEntity(),
                                set.includedBooks.map { it.toEntity(set.id) }
                            )
                        }
                    }
                }
            }

            firestore.listenAllMegaDeals { firestoreDeals ->
                _megaDeals.value = firestoreDeals
            }
        } catch (e: Exception) {
            Log.e("MarketplaceRepo", "Failed to start public Firestore listeners", e)
        }

        // Real-Time Cloud Firestore Listeners for User Directory and Conversations
        try {
            // Listen to all users for admin directory, cache, and live sync
            firestore.listenAllUsers { firestoreUsers ->
                if (firestoreUsers.isNotEmpty()) {
                    _allUsers.value = firestoreUsers
                    for (u in firestoreUsers) {
                        _userProfileCache[u.id] = u
                    }
                    val current = _currentUser.value
                    if (current.id.isNotBlank()) {
                        val matched = firestoreUsers.find { it.id == current.id }
                        if (matched != null && (matched.name != current.name || matched.college != current.college || matched.profilePhotoUri != current.profilePhotoUri || matched.phone != current.phone || matched.role != current.role || matched.accountStatus != current.accountStatus)) {
                            _currentUser.value = matched
                            syncUserToRoom(matched)
                        }
                    }
                    Log.d("MarketplaceRepo", "Synced ${firestoreUsers.size} users from Firestore in real-time")
                }
            }

            // Dynamically listen to current user profile & conversations for active session
            var conversationsReg: com.google.firebase.firestore.ListenerRegistration? = null
            var userProfileReg: com.google.firebase.firestore.ListenerRegistration? = null
            scope.launch {
                currentUser.collect { user ->
                    userProfileReg?.remove()
                    userProfileReg = null
                    conversationsReg?.remove()
                    conversationsReg = null
                    if (user.id.isNotBlank()) {
                        userProfileReg = firestore.listenUserProfile(user.id) { updatedProfile ->
                            if (updatedProfile != _currentUser.value) {
                                _currentUser.value = updatedProfile
                                _userProfileCache[updatedProfile.id] = updatedProfile
                                _allUsers.update { list ->
                                    list.map { if (it.id == updatedProfile.id) updatedProfile else it }
                                }
                                syncUserToRoom(updatedProfile)
                                Log.d("MarketplaceRepo", "Real-time user profile updated: ${updatedProfile.name}")
                            }
                        }
                        conversationsReg = firestore.listenConversations(
                            userId = user.id,
                            onConversationsUpdated = { firestoreConvs ->
                                _conversations.update { currentList ->
                                    val currentMap = currentList.associateBy { it.id }
                                    val remoteIds = firestoreConvs.map { it.id }.toSet()
                                    val mergedRemote = firestoreConvs.map { remoteConv ->
                                        val existing = currentMap[remoteConv.id]
                                        val preservedMessages = if (remoteConv.messages.isNotEmpty()) {
                                            val remoteMsgIds = remoteConv.messages.map { it.id }.toSet()
                                            val pendingLocal = (existing?.messages ?: emptyList()).filter { it.id !in remoteMsgIds }
                                            (remoteConv.messages + pendingLocal)
                                        } else {
                                            existing?.messages ?: emptyList()
                                        }
                                        remoteConv.copy(messages = preservedMessages)
                                    }
                                    val localOnlyConvs = currentList.filter { it.id !in remoteIds }
                                    (mergedRemote + localOnlyConvs).sortedByDescending { it.updatedAt }
                                }
                                val updatedConvs = _conversations.value
                                database?.let { db ->
                                    scope.launch(Dispatchers.IO) {
                                        for (conv in updatedConvs) {
                                            db.chatDao().insertConversation(conv.toEntity())
                                        }
                                    }
                                }
                            },
                            onNewMessageAlert = { rawAlert ->
                                // Trigger in-app visual alert only if the recipient is NOT currently inside this exact conversation
                                if (_activeChatConversationId.value != rawAlert.conversationId) {
                                    scope.launch {
                                        val senderProfile = getUserProfileAsync(rawAlert.senderId)
                                        val resolvedAlert = if (senderProfile != null) {
                                            rawAlert.copy(
                                                senderName = senderProfile.name.ifBlank { rawAlert.senderName },
                                                senderAvatarUri = senderProfile.profilePhotoUri ?: rawAlert.senderAvatarUri,
                                                senderCollege = senderProfile.college.ifBlank { rawAlert.senderCollege }
                                            )
                                        } else {
                                            rawAlert
                                        }
                                        _activeInAppAlert.value = resolvedAlert

                                        // Persist as notification in Notifications screen
                                        val notif = AppNotification(
                                            id = UUID.randomUUID().toString(),
                                            type = "CHAT_MESSAGE",
                                            title = "New message from ${resolvedAlert.senderName}",
                                            message = resolvedAlert.messageText,
                                            timeAgo = "Just now",
                                            isRead = false,
                                            relatedConversationId = resolvedAlert.conversationId,
                                            senderAvatarUri = resolvedAlert.senderAvatarUri
                                        )
                                        addNotification(notif)
                                        Log.d("MarketplaceRepo", "Pushed in-app alert & notification for message from ${resolvedAlert.senderName}")
                                    }
                                } else {
                                    Log.d("MarketplaceRepo", "Suppressed in-app notification banner for active conversation ${rawAlert.conversationId}")
                                }
                            }
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MarketplaceRepo", "Failed to start Firestore listeners", e)
        }

        scope.launch(Dispatchers.IO) {
            // Seed Super Admin if empty
            auth.seedDefaultUsersIfEmpty()

            // Purge legacy demo seed data from Room DB if present
            cleanDemoDataFromDatabaseIfPresent(db)

            // Ensure all existing user records have blank default profile picture (no avatar displayed)
            db.userDao().clearAllProfilePhotoRes()

            // Check Firebase Authentication on app start
            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (firebaseUser != null) {
                val email = firebaseUser.email?.trim()?.lowercase() ?: ""
                val existing = if (email.isNotEmpty()) {
                    db.userDao().getUserByEmail(email)
                } else {
                    db.userDao().getUserById(firebaseUser.uid)
                }

                if (existing != null) {
                    db.userDao().switchActiveSession(existing.id)
                    _currentUser.value = existing.toDomain().copy(profilePhotoRes = null)
                    _isLoggedIn.value = true
                } else {
                    val isAdmin = email.equals("mdmaheraz65@gmail.com", ignoreCase = true)
                    val newEntity = UserEntity(
                        id = firebaseUser.uid,
                        name = firebaseUser.displayName ?: if (isAdmin) "Super Admin" else "Student User",
                        email = email,
                        passwordHash = "",
                        phone = "01712-345678",
                        college = if (isAdmin) "Feni Computer Institute" else "Feni Polytechnic Institute",
                        technologyId = if (isAdmin) Technology.CSE.id else Technology.CIVIL.id,
                        semesterId = if (isAdmin) Semester.SEM_3.id else Semester.SEM_1.id,
                        profilePhotoRes = null,
                        profilePhotoUri = null,
                        role = if (isAdmin) "ADMIN" else "USER",
                        accountStatus = "ACTIVE",
                        isCurrentSession = true
                    )
                    db.userDao().insertUser(newEntity)
                    db.userDao().switchActiveSession(newEntity.id)
                    _currentUser.value = newEntity.toDomain()
                    _isLoggedIn.value = true
                }
            } else {
                // If not in Firebase Auth, check if local Room session was active
                val currentLocal = db.userDao().getCurrentUser()
                if (currentLocal != null && currentLocal.isCurrentSession) {
                    _currentUser.value = currentLocal.toDomain().copy(profilePhotoRes = null)
                    _isLoggedIn.value = true
                } else {
                    _isLoggedIn.value = false
                }
            }

            // Collect real-time updates from Room DB
            launch {
                db.userDao().getCurrentUserFlow().collect { userEntity ->
                    if (userEntity != null && userEntity.isCurrentSession) {
                        _currentUser.value = userEntity.toDomain().copy(profilePhotoRes = null)
                        _isLoggedIn.value = true
                    } else if (userEntity == null) {
                        _isLoggedIn.value = false
                    }
                }
            }

            launch {
                db.userDao().getAllUsersFlow().collect { users ->
                    if (users.isNotEmpty()) {
                        _allUsers.value = users.map { it.toDomain() }
                    } else {
                        _allUsers.value = listOf(defaultAdminProfile)
                    }
                }
            }

            launch {
                db.bookSetDao().getAllBookSetsWithBooksFlow().collect { list ->
                    if (_bookSets.value.isEmpty() && list.isNotEmpty()) {
                        _bookSets.value = list.map { it.toDomain() }
                    }
                }
            }

            launch {
                db.bookSetDao().getSavedBookSetIdsFlow().collect { ids ->
                    _savedSetIds.value = ids.toSet()
                }
            }

            launch {
                db.megaDealDao().getAllMegaDealsFlow().collect { deals ->
                    _megaDeals.value = deals.map { it.toDomain() }
                }
            }

            launch {
                kotlinx.coroutines.flow.combine(
                    db.chatDao().getAllConversationsFlow(),
                    db.chatDao().getAllMessagesFlow()
                ) { convs, messages ->
                    val curUserId = _currentUser.value.id
                    if (convs.isNotEmpty()) {
                        convs.map { it.toDomain(messages, curUserId) }
                    } else {
                        emptyList()
                    }
                }.collect { domainConvs ->
                    val currentConvs = _conversations.value
                    if (currentConvs.isEmpty()) {
                        _conversations.value = domainConvs
                    } else {
                        val domainMap = domainConvs.associateBy { it.id }
                        _conversations.update { list ->
                            list.map { conv ->
                                val fromRoom = domainMap[conv.id]
                                if (fromRoom != null && fromRoom.messages.isNotEmpty()) {
                                    val mergedMsgs = (fromRoom.messages + conv.messages).distinctBy { it.id }
                                    conv.copy(
                                        messages = mergedMsgs,
                                        lastMessage = mergedMsgs.lastOrNull()?.text ?: conv.lastMessage,
                                        timestamp = mergedMsgs.lastOrNull()?.timestamp ?: conv.timestamp
                                    )
                                } else {
                                    conv
                                }
                            }
                        }
                    }
                }
            }

            launch {
                db.notificationDao().getAllNotificationsFlow().collect { notifs ->
                    _notifications.value = notifs.map { it.toDomain() }
                }
            }
        }
    }

    private suspend fun cleanDemoDataFromDatabaseIfPresent(db: AppDatabase) {
        // Purge hardcoded mock data so app is 100% clean
        val demoCheck = db.bookSetDao().getBookSetWithBooksById("set_civil_4th_1")
        if (demoCheck != null) {
            db.bookSetDao().clearAllBookSets()
            db.bookSetDao().clearAllIncludedBooks()
            db.chatDao().clearAllConversations()
            db.chatDao().clearAllMessages()
            db.megaDealDao().clearAllMegaDeals()
            db.notificationDao().clearAllNotifications()
            db.userDao().clearNonAdminUsers()
        }
    }

    // Toggle Save / Bookmark
    fun toggleSaveSet(setId: String) {
        val currentlySaved = _savedSetIds.value.contains(setId)
        val newSavedState = !currentlySaved

        _savedSetIds.update { set ->
            if (currentlySaved) set - setId else set + setId
        }
        _bookSets.update { list ->
            list.map { if (it.id == setId) it.copy(isSaved = newSavedState) else it }
        }

        database?.let { db ->
            scope.launch(Dispatchers.IO) {
                db.bookSetDao().updateSavedStatus(setId, newSavedState)
            }
        }
    }

    // Publish New Book Set
    fun publishSet(
        title: String,
        subject: String,
        technology: Technology,
        semester: Semester,
        college: String,
        totalPrice: Int,
        overallCondition: SetCondition,
        description: String,
        includedBooks: List<IncludedBook>,
        coverDrawableRes: Int? = null,
        imageUrls: List<String> = emptyList()
    ): BookSet {
        val user = _currentUser.value
        val newId = "set_" + UUID.randomUUID().toString().take(8)

        val coverRes = coverDrawableRes ?: when (technology) {
            Technology.CIVIL, Technology.AIDT -> R.drawable.book_cover_civil_1789677775416
            Technology.ELECTRICAL, Technology.TELECOM -> R.drawable.book_cover_elec_1789677789123
            Technology.MECHANICAL, Technology.POWER -> R.drawable.book_cover_mech_1789677808779
            Technology.CSE -> R.drawable.book_cover_cse_1789735549141
        }

        val newSet = BookSet(
            id = newId,
            sellerId = user.id,
            sellerName = user.name,
            sellerCollege = user.college,
            sellerTech = user.technology.displayName,
            sellerSem = user.currentSemester.displayName,
            sellerPhone = user.phone,
            sellerEmail = user.email,
            title = title,
            subject = subject,
            technology = technology,
            semester = semester,
            college = college.ifBlank { user.college },
            totalPrice = totalPrice,
            overallCondition = overallCondition,
            description = description,
            includedBooks = includedBooks,
            coverDrawableRes = coverRes,
            imageUrls = imageUrls,
            status = SetStatus.AVAILABLE
        )

        _bookSets.update { listOf(newSet) + it }

        database?.let { db ->
            scope.launch(Dispatchers.IO) {
                val entity = newSet.toEntity()
                val bookEntities = includedBooks.map { it.toEntity(newId) }
                db.bookSetDao().insertBookSetWithBooks(entity, bookEntities)
            }
        }

        firestoreManager?.let { fs ->
            scope.launch(Dispatchers.IO) {
                try {
                    fs.publishBookSet(newSet)
                } catch (e: Exception) {
                    Log.e("MarketplaceRepo", "Failed to sync bookSet to Firestore", e)
                }
            }
        }

        return newSet
    }

    // Send a message
    fun sendMessage(conversationId: String, text: String): ChatMessage {
        val user = _currentUser.value
        val now = System.currentTimeMillis()
        val newMsg = ChatMessage(
            id = "msg_" + UUID.randomUUID().toString().take(8),
            senderId = user.id,
            senderName = user.name,
            text = text,
            timestamp = "Just now",
            isMe = true,
            senderAvatarUri = user.profilePhotoUri,
            senderAvatarRes = user.profilePhotoRes,
            isPending = true
        )
        recordChatInquiry()
        _conversations.update { list ->
            val updated = list.map { conv ->
                if (conv.id == conversationId) {
                    val updatedMsgs = (conv.messages + newMsg).distinctBy { it.id }
                    conv.copy(
                        messages = updatedMsgs,
                        lastMessage = text,
                        timestamp = "Just now",
                        updatedAt = now,
                        deletedByUserIds = emptyList() // Thread automatically reappears in inbox on new message
                    )
                } else conv
            }
            updated.sortedByDescending { it.updatedAt }
        }

        database?.let { db ->
            scope.launch(Dispatchers.IO) {
                db.chatDao().insertMessage(newMsg.toEntity(conversationId))
                db.chatDao().updateConversationSnippet(conversationId, text, "Just now")
                db.chatDao().updateDeletedByUserIds(conversationId, "")
            }
        }

        firestoreManager?.let { fs ->
            scope.launch(Dispatchers.IO) {
                try {
                    fs.sendMessage(conversationId, newMsg)
                } catch (e: Exception) {
                    Log.e("MarketplaceRepo", "Failed to sync message to Firestore", e)
                }
            }
        }
        return newMsg
    }

    // Attach real-time listener to conversation messages
    fun listenMessages(
        conversationId: String,
        onMessagesUpdated: (List<ChatMessage>) -> Unit = {}
    ): com.google.firebase.firestore.ListenerRegistration? {
        val fs = firestoreManager ?: return null
        val currentUserId = _currentUser.value.id
        Log.d("MarketplaceRepo", "Attaching listenMessages for $conversationId (currentUserId=$currentUserId)")
        return fs.listenMessages(conversationId, currentUserId) { liveMsgs ->
            Log.d("MarketplaceRepo", "listenMessages callback: received ${liveMsgs.size} messages for conversation $conversationId")
            val liveIds = liveMsgs.map { it.id }.toSet()
            var currentMergedMessages = liveMsgs
            val now = System.currentTimeMillis()

            _conversations.update { list ->
                val updated = list.map { conv ->
                    if (conv.id == conversationId) {
                        // Preserve in-flight local messages that haven't appeared in remote yet
                        val pendingLocal = conv.messages.filter { it.id !in liveIds }
                        val merged = (liveMsgs + pendingLocal)
                        currentMergedMessages = merged
                        val latestTime = merged.lastOrNull()?.timestamp ?: conv.timestamp
                        val shouldRevive = liveMsgs.size > conv.messages.size
                        conv.copy(
                            messages = merged,
                            lastMessage = merged.lastOrNull()?.text ?: conv.lastMessage,
                            timestamp = latestTime,
                            updatedAt = now,
                            deletedByUserIds = if (shouldRevive) emptyList() else conv.deletedByUserIds
                        )
                    } else conv
                }
                updated.sortedByDescending { it.updatedAt }
            }
            onMessagesUpdated(currentMergedMessages)
            database?.let { db ->
                scope.launch(Dispatchers.IO) {
                    for (msg in liveMsgs) {
                        db.chatDao().insertMessage(msg.toEntity(conversationId))
                    }
                }
            }
        }
    }

    // Delete single conversation thread from the current user's inbox
    fun deleteConversation(conversationId: String) {
        deleteConversations(listOf(conversationId))
    }

    // Bulk delete conversation threads from the current user's inbox
    fun deleteConversations(conversationIds: List<String>) {
        if (conversationIds.isEmpty()) return
        val currentUserId = _currentUser.value.id
        val idSet = conversationIds.toSet()

        _conversations.update { list ->
            list.map { conv ->
                if (conv.id in idSet) {
                    val updatedIds = (conv.deletedByUserIds + currentUserId).distinct()
                    conv.copy(deletedByUserIds = updatedIds)
                } else conv
            }
        }

        database?.let { db ->
            scope.launch(Dispatchers.IO) {
                for (id in conversationIds) {
                    val entity = db.chatDao().getConversationById(id)
                    if (entity != null) {
                        val currentList = entity.deletedByUserIds.split(",").filter { it.isNotBlank() }
                        val updated = (currentList + currentUserId).distinct().joinToString(",")
                        db.chatDao().updateDeletedByUserIds(id, updated)
                    }
                }
            }
        }

        firestoreManager?.let { fs ->
            scope.launch(Dispatchers.IO) {
                for (id in conversationIds) {
                    fs.markConversationDeletedForUser(id, currentUserId)
                }
            }
        }
    }

    // Mark conversation read
    fun markConversationAsRead(conversationId: String) {
        val currentUserId = _currentUser.value.id
        _conversations.update { list ->
            list.map { conv ->
                if (conv.id == conversationId) {
                    conv.copy(unreadCount = 0)
                } else conv
            }
        }
        database?.let { db ->
            scope.launch(Dispatchers.IO) {
                db.chatDao().markConversationRead(conversationId)
            }
        }
        firestoreManager?.let { fs ->
            scope.launch(Dispatchers.IO) {
                fs.markConversationRead(conversationId, currentUserId)
            }
        }
    }

    // Start or get conversation for a book set
    fun startOrGetConversation(set: BookSet): String {
        val currentUser = _currentUser.value
        val currentUserId = currentUser.id
        val sellerId = set.sellerId

        val existing = _conversations.value.find { conv ->
            conv.setId == set.id && (
                (conv.buyerId == currentUserId && conv.sellerId == sellerId) ||
                (conv.buyerId == sellerId && conv.sellerId == currentUserId)
            )
        }
        if (existing != null) {
            // If user previously deleted this thread from inbox, automatically un-hide it
            if (existing.deletedByUserIds.contains(currentUserId)) {
                _conversations.update { list ->
                    list.map { if (it.id == existing.id) it.copy(deletedByUserIds = it.deletedByUserIds - currentUserId) else it }
                }
                database?.let { db ->
                    scope.launch(Dispatchers.IO) {
                        db.chatDao().updateDeletedByUserIds(existing.id, "")
                    }
                }
            }
            return existing.id
        }

        recordChatInquiry()
        val newId = "conv_" + UUID.randomUUID().toString().take(8)
        val initialMsg = ChatMessage(
            id = "msg_" + UUID.randomUUID().toString().take(8),
            senderId = currentUserId,
            senderName = currentUser.name,
            text = "Hello! Is the complete set '${set.title}' available?",
            timestamp = "Just now",
            isMe = true,
            senderAvatarUri = currentUser.profilePhotoUri,
            senderAvatarRes = currentUser.profilePhotoRes
        )

        val sellerUser = _allUsers.value.find { it.id == sellerId }
        val buyerAvatarUri = currentUser.profilePhotoUri
        val sellerAvatarUri = sellerUser?.profilePhotoUri

        val newConv = Conversation(
            id = newId,
            buyerId = currentUserId,
            sellerId = sellerId,
            otherUserName = set.sellerName,
            otherUserCollege = set.sellerCollege,
            otherUserAvatarRes = null,
            otherUserAvatarUri = sellerAvatarUri,
            buyerName = currentUser.name,
            buyerCollege = currentUser.college,
            buyerAvatarUri = buyerAvatarUri,
            sellerName = set.sellerName,
            sellerCollege = set.sellerCollege,
            sellerAvatarUri = sellerAvatarUri,
            setId = set.id,
            setTitle = set.title,
            setPrice = set.totalPrice,
            lastMessage = "Started conversation about ${set.title}",
            timestamp = "Just now",
            unreadCount = 0,
            isOnline = true,
            messages = listOf(initialMsg),
            updatedAt = System.currentTimeMillis()
        )
        _conversations.update { (listOf(newConv) + it).sortedByDescending { c -> c.updatedAt } }

        database?.let { db ->
            scope.launch(Dispatchers.IO) {
                db.chatDao().insertConversation(newConv.toEntity())
                db.chatDao().insertMessage(initialMsg.toEntity(newId))
            }
        }

        firestoreManager?.let { fs ->
            scope.launch(Dispatchers.IO) {
                try {
                    fs.saveConversation(newConv)
                    fs.sendMessage(newId, initialMsg)
                } catch (e: Exception) {
                    Log.e("MarketplaceRepo", "Failed to sync new conversation to Firestore", e)
                }
            }
        }

        return newId
    }

    // Create a Mega Deal
    fun createMegaDeal(
        set: BookSet,
        finalPrice: Int,
        location: String,
        date: String,
        time: String
    ): MegaDeal {
        val user = _currentUser.value
        val newDeal = MegaDeal(
            dealId = "deal_" + UUID.randomUUID().toString().take(8),
            setId = set.id,
            setTitle = set.title,
            buyerId = user.id,
            buyerName = user.name,
            sellerId = set.sellerId,
            sellerName = set.sellerName,
            finalPrice = finalPrice,
            location = location,
            date = date,
            time = time,
            status = MegaDealStatus.PENDING
        )
        _megaDeals.update { listOf(newDeal) + it }

        val conv = _conversations.value.find { it.setId == set.id }
        if (conv != null) {
            val systemMsg = ChatMessage(
                id = "m_deal_" + UUID.randomUUID().toString().take(6),
                senderId = user.id,
                senderName = user.name,
                text = "🔥 Mega Deal Request Sent: Tk $finalPrice at $location on $date ($time)",
                timestamp = "Just now",
                isMe = true
            )
            _conversations.update { list ->
                list.map {
                    if (it.id == conv.id) it.copy(messages = it.messages + systemMsg, lastMessage = "🔥 Mega Deal Requested")
                    else it
                }
            }
            database?.let { db ->
                scope.launch(Dispatchers.IO) {
                    db.chatDao().insertMessage(systemMsg.toEntity(conv.id))
                    db.chatDao().updateConversationSnippet(conv.id, systemMsg.text, "Just now")
                }
            }
        }

        val notif = AppNotification(
            id = "notif_" + UUID.randomUUID().toString().take(6),
            type = "MEGA_DEAL_REQUEST",
            title = "Mega Deal Request Created",
            message = "Waiting for ${set.sellerName} to confirm deal for '${set.title}'.",
            timeAgo = "Just now",
            relatedSetId = set.id,
            relatedDealId = newDeal.dealId
        )
        _notifications.update { listOf(notif) + it }

        database?.let { db ->
            scope.launch(Dispatchers.IO) {
                db.megaDealDao().insertMegaDeal(newDeal.toEntity())
                db.notificationDao().insertNotification(notif.toEntity())
            }
        }

        return newDeal
    }

    // Confirm Mega Deal
    fun confirmMegaDeal(dealId: String) {
        _megaDeals.update { list ->
            list.map { if (it.dealId == dealId) it.copy(status = MegaDealStatus.CONFIRMED) else it }
        }
        val deal = _megaDeals.value.find { it.dealId == dealId } ?: return
        val notif = AppNotification(
            id = "notif_" + UUID.randomUUID().toString().take(6),
            type = "MEGA_DEAL_CONFIRMED",
            title = "Mega Deal Confirmed! ✅",
            message = "Deal confirmed for '${deal.setTitle}' at Tk ${deal.finalPrice}. Meet at ${deal.location}.",
            timeAgo = "Just now",
            relatedSetId = deal.setId,
            relatedDealId = dealId
        )
        _notifications.update { listOf(notif) + it }

        database?.let { db ->
            scope.launch(Dispatchers.IO) {
                db.megaDealDao().updateMegaDealStatus(dealId, MegaDealStatus.CONFIRMED.name)
                db.notificationDao().insertNotification(notif.toEntity())
            }
        }
    }

    // Decline Mega Deal
    fun declineMegaDeal(dealId: String) {
        _megaDeals.update { list ->
            list.map { if (it.dealId == dealId) it.copy(status = MegaDealStatus.CANCELLED) else it }
        }
        database?.let { db ->
            scope.launch(Dispatchers.IO) {
                db.megaDealDao().updateMegaDealStatus(dealId, MegaDealStatus.CANCELLED.name)
            }
        }
    }

    // Physical Exchange Completed -> Mark Set As SOLD
    fun markSetAsSold(dealId: String?, setId: String) {
        _bookSets.update { list ->
            list.map { if (it.id == setId) it.copy(status = SetStatus.SOLD) else it }
        }
        if (dealId != null) {
            _megaDeals.update { list ->
                list.map { if (it.dealId == dealId) it.copy(status = MegaDealStatus.COMPLETED) else it }
            }
        }
        val set = _bookSets.value.find { it.id == setId }
        val notif = AppNotification(
            id = "notif_" + UUID.randomUUID().toString().take(6),
            type = "SET_SOLD",
            title = "Book Set Sold! 🎓",
            message = "Physical exchange completed for '${set?.title ?: "Set"}'. Marked as SOLD.",
            timeAgo = "Just now",
            relatedSetId = setId
        )
        _notifications.update { listOf(notif) + it }

        database?.let { db ->
            scope.launch(Dispatchers.IO) {
                db.bookSetDao().updateBookSetStatus(setId, SetStatus.SOLD.name)
                if (dealId != null) {
                    db.megaDealDao().updateMegaDealStatus(dealId, MegaDealStatus.COMPLETED.name)
                }
                db.notificationDao().insertNotification(notif.toEntity())
            }
        }
    }

    // Update Profile
    fun updateUserProfile(
        name: String,
        email: String = _currentUser.value.email,
        phone: String,
        college: String,
        tech: Technology,
        sem: Semester
    ) {
        val updated = _currentUser.value.copy(
            name = name,
            email = email,
            phone = phone,
            college = college,
            technology = tech,
            currentSemester = sem
        )
        _currentUser.value = updated
        _userProfileCache[updated.id] = updated
        _allUsers.update { list ->
            list.map { if (it.id == updated.id) updated else it }
        }

        database?.let { db ->
            scope.launch(Dispatchers.IO) {
                val currentEntity = db.userDao().getUserById(updated.id)
                if (currentEntity != null) {
                    db.userDao().updateUser(
                        currentEntity.copy(
                            name = name,
                            email = email,
                            phone = phone,
                            college = college,
                            technologyId = tech.id,
                            semesterId = sem.id
                        )
                    )
                }
                try {
                    firestoreManager?.saveUserProfile(updated)
                } catch (e: Exception) {
                    Log.w("MarketplaceRepo", "Failed to sync updated profile to Firestore: ${e.message}")
                }
            }
        }
    }

    // Update Profile Photo
    fun updateProfilePhotoUri(uri: String?) {
        val updated = _currentUser.value.copy(profilePhotoUri = uri)
        _currentUser.value = updated
        _userProfileCache[updated.id] = updated
        _allUsers.update { list ->
            list.map { if (it.id == updated.id) updated else it }
        }

        database?.let { db ->
            scope.launch(Dispatchers.IO) {
                val currentEntity = db.userDao().getUserById(updated.id)
                if (currentEntity != null) {
                    db.userDao().updateUser(currentEntity.copy(profilePhotoUri = uri))
                }
                try {
                    firestoreManager?.saveUserProfile(updated)
                } catch (e: Exception) {
                    Log.w("MarketplaceRepo", "Failed to sync photo to Firestore: ${e.message}")
                }
            }
        }
    }

    private fun syncUserToRoom(user: UserProfile) {
        database?.let { db ->
            scope.launch(Dispatchers.IO) {
                val currentEntity = db.userDao().getUserById(user.id)
                if (currentEntity != null) {
                    db.userDao().updateUser(
                        currentEntity.copy(
                            name = user.name,
                            email = user.email,
                            phone = user.phone,
                            college = user.college,
                            technologyId = user.technology.id,
                            semesterId = user.currentSemester.id,
                            profilePhotoUri = user.profilePhotoUri,
                            role = user.role,
                            accountStatus = user.accountStatus
                        )
                    )
                }
            }
        }
    }

    // Authentication: Login & Logout & SignUp
    fun logout() {
        _isLoggedIn.value = false
        scope.launch(Dispatchers.IO) {
            authManager?.signOut()
        }
    }

    fun restoreSession() {
        _isLoggedIn.value = true
    }

    suspend fun performSignIn(email: String, password: String): AuthResult = withContext(Dispatchers.IO + NonCancellable) {
        val auth = authManager ?: return@withContext AuthResult.Error("Authentication service is initializing. Please try again.")
        val result = auth.signIn(email, password)
        if (result is AuthResult.Success) {
            _currentUser.value = result.user
            _isLoggedIn.value = true
        } else {
            _isLoggedIn.value = false
        }
        result
    }

    suspend fun performSignUp(
        name: String,
        email: String,
        password: String,
        phone: String,
        college: String,
        technology: Technology,
        semester: Semester
    ): AuthResult = withContext(Dispatchers.IO + NonCancellable) {
        val auth = authManager ?: return@withContext AuthResult.Error("Authentication service is initializing. Please try again.")
        val result = auth.signUp(name, email, password, phone, college, technology, semester)
        if (result is AuthResult.Success) {
            _currentUser.value = result.user
            _isLoggedIn.value = true
        } else {
            _isLoggedIn.value = false
        }
        result
    }

    fun addNotification(notif: AppNotification) {
        _notifications.update { listOf(notif) + it }
        database?.let { db ->
            scope.launch(Dispatchers.IO) {
                db.notificationDao().insertNotification(notif.toEntity())
            }
        }
    }

    suspend fun initiateSignUpWithOtp(
        name: String,
        email: String,
        password: String,
        phone: String,
        college: String,
        technology: Technology,
        semester: Semester
    ): OtpResult {
        val otpMgr = emailOtpManager ?: return OtpResult.Error("System initializing. Please try again.")
        val result = otpMgr.initiateOtp(
            name = name,
            email = email,
            password = password,
            phone = phone,
            college = college,
            technology = technology,
            semester = semester
        )
        if (result is OtpResult.CodeSent) {
            _latestOtpCode.value = result.otpCode
            _pendingOtpEmail.value = result.email

            val notif = AppNotification(
                id = "notif_otp_" + System.currentTimeMillis(),
                type = "SECURITY",
                title = "Gmail Verification OTP",
                message = "Your account verification OTP code is: ${result.otpCode} (valid for 5 minutes).",
                timeAgo = "Just now"
            )
            addNotification(notif)
        }
        return result
    }

    suspend fun resendSignUpOtp(): OtpResult {
        val otpMgr = emailOtpManager ?: return OtpResult.Error("System initializing. Please try again.")
        val result = otpMgr.resendOtp()
        if (result is OtpResult.CodeSent) {
            _latestOtpCode.value = result.otpCode
            _pendingOtpEmail.value = result.email

            val notif = AppNotification(
                id = "notif_otp_" + System.currentTimeMillis(),
                type = "SECURITY",
                title = "New OTP Code Sent",
                message = "Your new verification OTP code is: ${result.otpCode} (valid for 5 minutes).",
                timeAgo = "Just now"
            )
            addNotification(notif)
        }
        return result
    }

    suspend fun verifySignUpOtp(enteredOtp: String): AuthResult {
        val otpMgr = emailOtpManager ?: return AuthResult.Error("System initializing. Please try again.")
        val verifyResult = otpMgr.verifyOtp(enteredOtp)
        if (verifyResult.isFailure) {
            return AuthResult.Error(verifyResult.exceptionOrNull()?.message ?: "Invalid OTP code.")
        }

        val pending = verifyResult.getOrThrow()
        val authResult = performSignUp(
            name = pending.name,
            email = pending.email,
            password = pending.password,
            phone = pending.phone,
            college = pending.college,
            technology = pending.technology,
            semester = pending.semester
        )

        if (authResult is AuthResult.Success) {
            _latestOtpCode.value = null
            _pendingOtpEmail.value = null

            val welcomeNotif = AppNotification(
                id = "notif_welcome_" + System.currentTimeMillis(),
                type = "WELCOME",
                title = "Email Verification Successful! 🎉",
                message = "Welcome ${pending.name}! Your Gmail account has been successfully verified and activated.",
                timeAgo = "Just now"
            )
            addNotification(welcomeNotif)
        }

        return authResult
    }

    fun cancelPendingOtp() {
        emailOtpManager?.cancelPending()
        _latestOtpCode.value = null
        _pendingOtpEmail.value = null
    }

    suspend fun performGoogleSignIn(
        googleUid: String,
        email: String,
        displayName: String?,
        photoUrl: String?,
        college: String = "Feni Polytechnic Institute",
        technology: Technology = Technology.CIVIL,
        semester: Semester = Semester.SEM_1
    ): AuthResult {
        val auth = authManager
        if (auth == null) {
            val user = UserProfile(
                id = googleUid,
                name = displayName?.ifBlank { null } ?: "Student User",
                email = email,
                phone = "01712-345678",
                college = college,
                technology = technology,
                currentSemester = semester,
                profilePhotoRes = null,
                profilePhotoUri = photoUrl,
                role = if (email.equals("mdmaheraz65@gmail.com", ignoreCase = true)) "ADMIN" else "USER",
                accountStatus = "ACTIVE"
            )
            _currentUser.value = user
            _isLoggedIn.value = true
            return AuthResult.Success(user)
        }
        val result = auth.signInWithGoogleUser(
            googleUid = googleUid,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl,
            college = college,
            technology = technology,
            semester = semester
        )
        if (result is AuthResult.Success) {
            _currentUser.value = result.user
            _isLoggedIn.value = true
        }
        return result
    }

    fun login(
        name: String = "Super Admin",
        email: String = "mdmaheraz65@gmail.com",
        phone: String = "01712-345678",
        college: String = "Feni Computer Institute",
        tech: Technology = Technology.CSE,
        sem: Semester = Semester.SEM_3
    ) {
        val updated = _currentUser.value.copy(
            name = name,
            email = email,
            phone = phone,
            college = college,
            technology = tech,
            currentSemester = sem,
            role = if (email == "mdmaheraz65@gmail.com") "ADMIN" else "USER"
        )
        _currentUser.value = updated
        _isLoggedIn.value = true

        scope.launch(Dispatchers.IO) {
            val db = database ?: return@launch
            val existing = db.userDao().getUserByEmail(email)
            if (existing != null) {
                db.userDao().clearCurrentSession()
                db.userDao().setCurrentSession(existing.id)
            }
        }
    }

    // Admin: Suspend User
    fun adminSuspendUser(userId: String) {
        _allUsers.update { list ->
            list.map { if (it.id == userId) it.copy(accountStatus = "SUSPENDED") else it }
        }
        scope.launch(Dispatchers.IO) {
            database?.let { db ->
                val user = db.userDao().getUserById(userId)
                if (user != null) {
                    db.userDao().updateUser(user.copy(accountStatus = "SUSPENDED"))
                }
            }
        }
    }

    // Admin: Ban User
    fun adminBanUser(userId: String) {
        _allUsers.update { list ->
            list.map { if (it.id == userId) it.copy(accountStatus = "BANNED") else it }
        }
        scope.launch(Dispatchers.IO) {
            database?.let { db ->
                val user = db.userDao().getUserById(userId)
                if (user != null) {
                    db.userDao().updateUser(user.copy(accountStatus = "BANNED"))
                }
            }
        }
    }

    // Admin: Restore User
    fun adminRestoreUser(userId: String) {
        _allUsers.update { list ->
            list.map { if (it.id == userId) it.copy(accountStatus = "ACTIVE") else it }
        }
        scope.launch(Dispatchers.IO) {
            database?.let { db ->
                val user = db.userDao().getUserById(userId)
                if (user != null) {
                    db.userDao().updateUser(user.copy(accountStatus = "ACTIVE"))
                }
            }
        }
    }

    // Admin: Remove Inappropriate Set
    fun adminRemoveSet(setId: String) {
        _bookSets.update { list ->
            list.map { if (it.id == setId) it.copy(status = SetStatus.REMOVED) else it }
        }
        scope.launch(Dispatchers.IO) {
            database?.bookSetDao()?.updateBookSetStatus(setId, SetStatus.REMOVED.name)
            firestoreManager?.updateBookSetStatus(setId, SetStatus.REMOVED)
        }
    }

    // Admin: Restore Listing to Feed
    fun adminRestoreSet(setId: String) {
        _bookSets.update { list ->
            list.map { if (it.id == setId) it.copy(status = SetStatus.AVAILABLE) else it }
        }
        scope.launch(Dispatchers.IO) {
            database?.bookSetDao()?.updateBookSetStatus(setId, SetStatus.AVAILABLE.name)
            firestoreManager?.updateBookSetStatus(setId, SetStatus.AVAILABLE)
        }
    }

    // Admin: Permanently Delete Listing
    fun adminDeleteSet(setId: String) {
        _bookSets.update { list ->
            list.filter { it.id != setId }
        }
        scope.launch(Dispatchers.IO) {
            database?.bookSetDao()?.deleteBookSet(setId)
            firestoreManager?.deleteBookSet(setId)
        }
    }

    // Admin: Block User Permanently
    fun adminBlockUser(userId: String) {
        _allUsers.update { list ->
            list.map { if (it.id == userId) it.copy(accountStatus = "BLOCKED") else it }
        }
        scope.launch(Dispatchers.IO) {
            database?.let { db ->
                val user = db.userDao().getUserById(userId)
                if (user != null) {
                    db.userDao().updateUser(user.copy(accountStatus = "BLOCKED"))
                }
            }
            firestoreManager?.updateUserAccountStatus(userId, "BLOCKED")
        }
    }

    // Admin: Delete User Permanently
    fun adminDeleteUser(userId: String) {
        _allUsers.update { list ->
            list.filter { it.id != userId }
        }
        scope.launch(Dispatchers.IO) {
            database?.userDao()?.deleteUser(userId)
            firestoreManager?.deleteUser(userId)
        }
    }

    // Admin: Provision / Register Student User
    fun adminCreateUser(
        name: String,
        email: String,
        phone: String,
        college: String,
        technology: Technology,
        semester: Semester,
        role: String = "USER"
    ) {
        val newUserId = "user_" + UUID.randomUUID().toString().take(8)
        val profile = UserProfile(
            id = newUserId,
            name = name,
            email = email,
            phone = phone,
            college = college,
            technology = technology,
            currentSemester = semester,
            profilePhotoRes = null,
            role = role,
            accountStatus = "ACTIVE"
        )
        _allUsers.update { it + profile }
        scope.launch(Dispatchers.IO) {
            database?.userDao()?.insertUser(
                UserEntity(
                    id = newUserId,
                    name = name,
                    email = email,
                    passwordHash = "polytechnic123",
                    phone = phone,
                    college = college,
                    technologyId = technology.id,
                    semesterId = semester.id,
                    profilePhotoRes = null,
                    role = role,
                    accountStatus = "ACTIVE",
                    isCurrentSession = false
                )
            )
        }
    }

    // Admin: Reset / Refresh Traffic Telemetry Counters to Zero
    fun resetTrafficStats() {
        _trafficStats.value = AppTrafficStats(
            dailyActiveUsers = 0,
            totalVisitsToday = 0,
            bookChecksToday = 0,
            chatInquiriesToday = 0,
            fpiTrafficShare = 0,
            fciTrafficShare = 0,
            peakHour = "No traffic yet",
            newSignupsToday = 0
        )
    }

    fun deleteSet(setId: String) {
        adminDeleteSet(setId)
    }

    // Record Book check / page view from actual user interaction
    fun recordBookCheck() {
        _trafficStats.update { it.copy(bookChecksToday = it.bookChecksToday + 1) }
    }

    // Record App Visit from actual user launch / navigation
    fun recordVisit() {
        _trafficStats.update {
            val visits = it.totalVisitsToday + 1
            it.copy(
                totalVisitsToday = visits,
                dailyActiveUsers = if (it.dailyActiveUsers == 0) 1 else it.dailyActiveUsers
            )
        }
    }

    // Record Chat inquiry from actual user message
    fun recordChatInquiry() {
        _trafficStats.update {
            it.copy(chatInquiriesToday = it.chatInquiriesToday + 1)
        }
    }

    // Record new signup from actual user registration
    fun recordNewSignup() {
        _trafficStats.update {
            it.copy(newSignupsToday = it.newSignupsToday + 1)
        }
    }

    // Mark notification as read
    fun markNotificationRead(id: String) {
        _notifications.update { list ->
            list.map { if (it.id == id) it.copy(isRead = true) else it }
        }
        scope.launch(Dispatchers.IO) {
            database?.notificationDao()?.markAsRead(id)
        }
    }

    fun markAllNotificationsRead() {
        _notifications.update { list -> list.map { it.copy(isRead = true) } }
        scope.launch(Dispatchers.IO) {
            database?.notificationDao()?.markAllAsRead()
        }
    }

    // Converters between entities and domain models
    private fun UserEntity.toDomain(): UserProfile {
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

    private fun BookSetWithBooks.toDomain(): BookSet {
        val cond = try { SetCondition.valueOf(bookSet.overallCondition) } catch (e: Exception) { SetCondition.GOOD }
        val stat = try { SetStatus.valueOf(bookSet.status) } catch (e: Exception) { SetStatus.AVAILABLE }
        val imgs = if (bookSet.imageUrlsString.isBlank()) emptyList() else bookSet.imageUrlsString.split("||")
        return BookSet(
            id = bookSet.id,
            sellerId = bookSet.sellerId,
            sellerName = bookSet.sellerName,
            sellerCollege = bookSet.sellerCollege,
            sellerTech = bookSet.sellerTech,
            sellerSem = bookSet.sellerSem,
            sellerPhone = bookSet.sellerPhone,
            sellerEmail = bookSet.sellerEmail,
            title = bookSet.title,
            subject = bookSet.subject,
            technology = Technology.fromId(bookSet.technologyId),
            semester = Semester.fromId(bookSet.semesterId),
            college = bookSet.college,
            totalPrice = bookSet.totalPrice,
            overallCondition = cond,
            description = bookSet.description,
            includedBooks = books.map { it.toDomain() },
            coverDrawableRes = bookSet.coverDrawableRes,
            imageUrls = imgs,
            status = stat,
            isSaved = bookSet.isSaved,
            createdAt = bookSet.createdAt
        )
    }

    private fun IncludedBookEntity.toDomain(): IncludedBook {
        val imgs = listOfNotNull(image1Uri.ifBlank { null }, image2Uri.ifBlank { null })
        return IncludedBook(
            id = id,
            setId = setId,
            bookName = bookName,
            subject = subject,
            author = author,
            edition = edition,
            bookCode = bookCode,
            condition = condition,
            imageUris = imgs
        )
    }

    private fun BookSet.toEntity(): BookSetEntity {
        return BookSetEntity(
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
            technologyId = technology.id,
            semesterId = semester.id,
            college = college,
            totalPrice = totalPrice,
            overallCondition = overallCondition.name,
            description = description,
            coverDrawableRes = coverDrawableRes,
            imageUrlsString = imageUrls.joinToString("||"),
            status = status.name,
            isSaved = isSaved,
            createdAt = createdAt
        )
    }

    private fun IncludedBook.toEntity(setId: String): IncludedBookEntity {
        return IncludedBookEntity(
            id = id,
            setId = setId,
            bookName = bookName,
            subject = subject,
            author = author,
            edition = edition,
            bookCode = bookCode,
            condition = condition,
            image1Uri = imageUris.getOrNull(0) ?: "",
            image2Uri = imageUris.getOrNull(1) ?: ""
        )
    }

    private fun ConversationEntity.toDomain(messages: List<ChatMessageEntity>, currentUserId: String = ""): Conversation {
        return Conversation(
            id = id,
            buyerId = buyerId,
            sellerId = sellerId,
            otherUserName = otherUserName,
            otherUserCollege = otherUserCollege,
            otherUserAvatarRes = otherUserAvatarRes,
            otherUserAvatarUri = otherUserAvatarUri,
            buyerName = buyerName,
            buyerCollege = buyerCollege,
            buyerAvatarUri = buyerAvatarUri,
            sellerName = sellerName,
            sellerCollege = sellerCollege,
            sellerAvatarUri = sellerAvatarUri,
            setId = setId,
            setTitle = setTitle,
            setPrice = setPrice,
            lastMessage = lastMessage,
            timestamp = timestamp,
            unreadCount = unreadCount,
            isOnline = isOnline,
            messages = messages.filter { it.conversationId == id }.map { it.toDomain(currentUserId) },
            deletedByUserIds = deletedByUserIds.split(",").filter { it.isNotBlank() }
        )
    }

    private fun Conversation.toEntity(): ConversationEntity {
        return ConversationEntity(
            id = id,
            buyerId = buyerId,
            sellerId = sellerId,
            otherUserName = otherUserName,
            otherUserCollege = otherUserCollege,
            otherUserAvatarRes = otherUserAvatarRes,
            otherUserAvatarUri = otherUserAvatarUri,
            buyerName = buyerName,
            buyerCollege = buyerCollege,
            buyerAvatarUri = buyerAvatarUri,
            sellerName = sellerName,
            sellerCollege = sellerCollege,
            sellerAvatarUri = sellerAvatarUri,
            setId = setId,
            setTitle = setTitle,
            setPrice = setPrice,
            lastMessage = lastMessage,
            timestamp = timestamp,
            unreadCount = unreadCount,
            isOnline = isOnline,
            deletedByUserIds = deletedByUserIds.joinToString(",")
        )
    }

    private fun ChatMessageEntity.toDomain(currentUserId: String = ""): ChatMessage {
        return ChatMessage(
            id = id,
            senderId = senderId,
            senderName = senderName,
            text = text,
            timestamp = timestamp,
            isMe = if (currentUserId.isNotBlank()) senderId == currentUserId else isMe,
            senderAvatarUri = senderAvatarUri,
            senderAvatarRes = senderAvatarRes
        )
    }

    private fun ChatMessage.toEntity(convId: String): ChatMessageEntity {
        return ChatMessageEntity(
            id = id,
            conversationId = convId,
            senderId = senderId,
            senderName = senderName,
            text = text,
            timestamp = timestamp,
            isMe = isMe,
            senderAvatarUri = senderAvatarUri,
            senderAvatarRes = senderAvatarRes
        )
    }

    private fun MegaDealEntity.toDomain(): MegaDeal {
        val stat = try { MegaDealStatus.valueOf(status) } catch (e: Exception) { MegaDealStatus.PENDING }
        return MegaDeal(
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
            status = stat,
            createdAt = createdAt
        )
    }

    private fun MegaDeal.toEntity(): MegaDealEntity {
        return MegaDealEntity(
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
            status = status.name,
            createdAt = createdAt
        )
    }

    private fun NotificationEntity.toDomain(): AppNotification {
        return AppNotification(
            id = id,
            type = type,
            title = title,
            message = message,
            timeAgo = timeAgo,
            isRead = isRead,
            relatedSetId = relatedSetId,
            relatedDealId = relatedDealId
        )
    }

    private fun AppNotification.toEntity(): NotificationEntity {
        return NotificationEntity(
            id = id,
            type = type,
            title = title,
            message = message,
            timeAgo = timeAgo,
            isRead = isRead,
            relatedSetId = relatedSetId,
            relatedDealId = relatedDealId
        )
    }
}
