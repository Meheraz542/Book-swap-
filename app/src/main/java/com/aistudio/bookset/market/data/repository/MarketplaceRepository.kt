package com.aistudio.bookset.market.data.repository

import android.content.Context
import android.util.Log
import com.aistudio.bookset.market.data.local.AppDatabase
import com.aistudio.bookset.market.data.model.BookSet
import com.aistudio.bookset.market.data.model.ChatMessage
import com.aistudio.bookset.market.data.model.MegaDeal
import com.aistudio.bookset.market.data.model.OrderItem
import com.aistudio.bookset.market.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class MarketplaceRepository(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val db = AppDatabase.getDatabase(context).bookSetDao()
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _bookSets = MutableStateFlow<List<BookSet>>(emptyList())
    val bookSets: StateFlow<List<BookSet>> = _bookSets.asStateFlow()

    private val _megaDeals = MutableStateFlow<List<MegaDeal>>(emptyList())
    val megaDeals: StateFlow<List<MegaDeal>> = _megaDeals.asStateFlow()

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow<Boolean>(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _orders = MutableStateFlow<List<OrderItem>>(emptyList())
    val orders: StateFlow<List<OrderItem>> = _orders.asStateFlow()

    init {
        scope.launch {
            seedDefaultDataIfEmpty()
            observeLocalCache()
            checkCurrentAuthSession()
            attachFirestoreListeners()
        }
    }

    private fun checkCurrentAuthSession() {
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            val profile = UserProfile(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: "user@bookset.market",
                name = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "Reader",
                isAdmin = true // Allow instant admin access
            )
            _currentUser.value = profile
            _isLoggedIn.value = true
        }
    }

    private suspend fun observeLocalCache() {
        // Collect from Room so UI is instantly responsive
        scope.launch {
            db.getAllBookSets().collect { list ->
                if (list.isNotEmpty()) {
                    _bookSets.value = list
                }
            }
        }
        scope.launch {
            db.getAllMegaDeals().collect { deals ->
                if (deals.isNotEmpty()) {
                    _megaDeals.value = deals
                }
            }
        }
        scope.launch {
            db.getAllOrders().collect { orderList ->
                _orders.value = orderList
            }
        }
    }

    private fun attachFirestoreListeners() {
        try {
            // Listen to book_sets safely without crash if permission is denied
            firestore.collection("book_sets")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("MarketplaceRepository", "Firestore book_sets listen fallback: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        val sets = snapshot.documents.mapNotNull { it.toObject(BookSet::class.java) }
                        if (sets.isNotEmpty()) {
                            _bookSets.value = sets
                            scope.launch { db.insertBookSets(sets) }
                        }
                    }
                }

            // Listen to mega_deals
            firestore.collection("mega_deals")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("MarketplaceRepository", "Firestore mega_deals listen fallback: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        val deals = snapshot.documents.mapNotNull { it.toObject(MegaDeal::class.java) }
                        if (deals.isNotEmpty()) {
                            _megaDeals.value = deals
                            scope.launch { db.insertMegaDeals(deals) }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w("MarketplaceRepository", "Could not attach Firestore listeners: ${e.message}")
        }
    }

    private suspend fun seedDefaultDataIfEmpty() {
        val initialSets = listOf(
            BookSet(
                id = "bs_1",
                title = "Harry Potter Complete Series (1-7)",
                category = "Fantasy & Fiction",
                booksCount = 7,
                bookTitles = listOf(
                    "Philosopher's Stone", "Chamber of Secrets", "Prisoner of Azkaban",
                    "Goblet of Fire", "Order of the Phoenix", "Half-Blood Prince", "Deathly Hallows"
                ),
                originalPrice = 95.0,
                discountPrice = 58.0,
                condition = "Like New",
                description = "Boxed collector's anniversary hardcover edition with gilded edges and map inserts.",
                sellerName = "Hogwarts Vault",
                sellerPhone = "+1 888-294-2731",
                rating = 4.9f,
                reviewCount = 89,
                imageUrl = "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600",
                isFeatured = true
            ),
            BookSet(
                id = "bs_2",
                title = "The Lord of the Rings & The Hobbit Set",
                category = "Fantasy & Fiction",
                booksCount = 4,
                bookTitles = listOf("The Hobbit", "The Fellowship of the Ring", "The Two Towers", "The Return of the King"),
                originalPrice = 75.0,
                discountPrice = 42.0,
                condition = "New",
                description = "Illustrated deluxe 4-volume set featuring original maps of Middle-earth and red faux-leather binding.",
                sellerName = "Rivendell Archives",
                sellerPhone = "+1 888-348-1294",
                rating = 5.0f,
                reviewCount = 120,
                imageUrl = "https://images.unsplash.com/photo-1463320726281-696a485928c7?w=600",
                isFeatured = true
            ),
            BookSet(
                id = "bs_3",
                title = "Classic Science Fiction Trilogy Bundle",
                category = "Sci-Fi",
                booksCount = 3,
                bookTitles = listOf("Dune", "Foundation", "Neuromancer"),
                originalPrice = 54.0,
                discountPrice = 31.0,
                condition = "Like New",
                description = "The foundational pillars of world-building science fiction in compact paperback slipcase.",
                sellerName = "Galactic Press",
                sellerPhone = "+1 888-771-8842",
                rating = 4.7f,
                reviewCount = 46,
                imageUrl = "https://images.unsplash.com/photo-1532012164546-f432f2e379b2?w=600",
                isFeatured = false
            ),
            BookSet(
                id = "bs_4",
                title = "Oxford World History & Civilization Set",
                category = "Academic",
                booksCount = 5,
                bookTitles = listOf("Ancient Near East", "Classical Greece & Rome", "The Middle Ages", "The Renaissance", "Modern Era"),
                originalPrice = 140.0,
                discountPrice = 79.0,
                condition = "Good",
                description = "Comprehensive university standard historical curriculum set with timeline reference charts.",
                sellerName = "Academic Scholars Library",
                sellerPhone = "+1 888-912-3401",
                rating = 4.8f,
                reviewCount = 31,
                imageUrl = "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600",
                isFeatured = false
            ),
            BookSet(
                id = "bs_5",
                title = "Children's Bedtime Fairytales & Fables",
                category = "Children",
                booksCount = 6,
                bookTitles = listOf("Aesop's Fables", "Grimm's Tales", "Andersen Fables", "Peter Pan", "Alice in Wonderland", "The Secret Garden"),
                originalPrice = 60.0,
                discountPrice = 34.0,
                condition = "New",
                description = "Brightly illustrated full-color hardcover storybooks designed for early readers and evening storytime.",
                sellerName = "Storybook Haven",
                sellerPhone = "+1 888-444-1928",
                rating = 4.9f,
                reviewCount = 52,
                imageUrl = "https://images.unsplash.com/photo-1507842229457-79a6111f2679?w=600",
                isFeatured = true
            )
        )
        db.insertBookSets(initialSets)
        _bookSets.value = initialSets

        val initialDeals = listOf(
            MegaDeal(
                id = "md_1",
                title = "Weekend Mega Fantasy Bundle",
                subtitle = "Save 45% on top-rated complete collector book sets",
                discountBadge = "45% OFF",
                originalPrice = 95.0,
                dealPrice = 52.0,
                bookSetId = "bs_1",
                endsInHours = 18,
                bannerColor = 0xFF3F63F6
            ),
            MegaDeal(
                id = "md_2",
                title = "Middle-Earth Illustrated Super Deal",
                subtitle = "Hardcover with gilded edges + Middle-earth map",
                discountBadge = "55% OFF",
                originalPrice = 75.0,
                dealPrice = 39.0,
                bookSetId = "bs_2",
                endsInHours = 36,
                bannerColor = 0xFF10B981
            )
        )
        db.insertMegaDeals(initialDeals)
        _megaDeals.value = initialDeals
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        _currentUser.value = profile
        _isLoggedIn.value = true
        db.insertUserProfile(profile)

        // Try syncing to Firestore safely without blocking or throwing unhandled exception
        try {
            firestore.collection("users").document(profile.uid).set(profile)
        } catch (e: Exception) {
            Log.w("MarketplaceRepository", "Offline or Firestore write skipped: ${e.message}")
        }
    }

    fun directAdminLogin() {
        val adminUser = UserProfile(
            uid = "admin_${System.currentTimeMillis()}",
            email = "admin@bookset.market",
            name = "System Administrator",
            phone = "+1 800-BOOK-ADMIN",
            isAdmin = true
        )
        _currentUser.value = adminUser
        _isLoggedIn.value = true
        scope.launch {
            db.insertUserProfile(adminUser)
        }
    }

    fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.w("MarketplaceRepository", "Auth signOut: ${e.message}")
        }
        _currentUser.value = null
        _isLoggedIn.value = false
    }

    suspend fun addBookSet(bookSet: BookSet) {
        db.insertBookSet(bookSet)
        val current = _bookSets.value.toMutableList()
        current.add(0, bookSet)
        _bookSets.value = current

        try {
            firestore.collection("book_sets").document(bookSet.id).set(bookSet)
        } catch (e: Exception) {
            Log.w("MarketplaceRepository", "Firestore write fallback: ${e.message}")
        }
    }

    suspend fun deleteBookSet(id: String) {
        db.deleteBookSet(id)
        _bookSets.value = _bookSets.value.filter { it.id != id }

        try {
            firestore.collection("book_sets").document(id).delete()
        } catch (e: Exception) {
            Log.w("MarketplaceRepository", "Firestore delete fallback: ${e.message}")
        }
    }

    suspend fun createOrder(order: OrderItem) {
        db.insertOrder(order)
        val current = _orders.value.toMutableList()
        current.add(0, order)
        _orders.value = current

        try {
            firestore.collection("orders").document(order.id).set(order)
        } catch (e: Exception) {
            Log.w("MarketplaceRepository", "Firestore order write fallback: ${e.message}")
        }
    }

    fun sendMessage(text: String, recipientName: String) {
        val user = _currentUser.value
        val msg = ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = "conv_general",
            senderId = user?.uid ?: "user_me",
            senderName = user?.name ?: "Me",
            text = text,
            timestamp = System.currentTimeMillis()
        )
        val updated = _messages.value.toMutableList()
        updated.add(msg)
        _messages.value = updated

        // Automatic polite seller response simulation
        scope.launch {
            kotlinx.coroutines.delay(1200)
            val reply = ChatMessage(
                id = UUID.randomUUID().toString(),
                conversationId = "conv_general",
                senderId = "seller",
                senderName = recipientName,
                text = "Hello! Yes, the book set is in excellent condition and ready for immediate secure delivery. Let me know if you need more photos!",
                timestamp = System.currentTimeMillis()
            )
            val updatedWithReply = _messages.value.toMutableList()
            updatedWithReply.add(reply)
            _messages.value = updatedWithReply
        }
    }
}
