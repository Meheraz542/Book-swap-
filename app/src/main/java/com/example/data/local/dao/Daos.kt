package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isCurrentSession = 1 LIMIT 1")
    fun getCurrentUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE isCurrentSession = 1 LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("UPDATE users SET isCurrentSession = 0")
    suspend fun clearCurrentSession()

    @Query("UPDATE users SET isCurrentSession = 1 WHERE id = :userId")
    suspend fun setCurrentSession(userId: String)

    @Transaction
    suspend fun switchActiveSession(userId: String) {
        clearCurrentSession()
        setCurrentSession(userId)
    }

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: String)

    @Query("DELETE FROM users WHERE role != 'ADMIN'")
    suspend fun clearNonAdminUsers()

    @Query("UPDATE users SET profilePhotoRes = NULL")
    suspend fun clearAllProfilePhotoRes()
}

@Dao
interface BookSetDao {
    @Transaction
    @Query("SELECT * FROM book_sets ORDER BY createdAt DESC")
    fun getAllBookSetsWithBooksFlow(): Flow<List<BookSetWithBooks>>

    @Transaction
    @Query("SELECT * FROM book_sets ORDER BY createdAt DESC")
    suspend fun getAllBookSetsWithBooks(): List<BookSetWithBooks>

    @Transaction
    @Query("SELECT * FROM book_sets WHERE id = :id LIMIT 1")
    suspend fun getBookSetWithBooksById(id: String): BookSetWithBooks?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookSet(bookSet: BookSetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncludedBooks(books: List<IncludedBookEntity>)

    @Transaction
    suspend fun insertBookSetWithBooks(bookSet: BookSetEntity, books: List<IncludedBookEntity>) {
        insertBookSet(bookSet)
        // Clean previous books if updating
        deleteBooksForSet(bookSet.id)
        insertIncludedBooks(books)
    }

    @Query("DELETE FROM included_books WHERE setId = :setId")
    suspend fun deleteBooksForSet(setId: String)

    @Query("DELETE FROM book_sets WHERE id = :id")
    suspend fun deleteBookSetById(id: String)

    @Query("DELETE FROM book_sets WHERE id = :id")
    suspend fun deleteBookSet(id: String)

    @Query("UPDATE book_sets SET isSaved = :isSaved WHERE id = :id")
    suspend fun updateSavedStatus(id: String, isSaved: Boolean)

    @Query("UPDATE book_sets SET status = :status WHERE id = :id")
    suspend fun updateBookSetStatus(id: String, status: String)

    @Query("UPDATE book_sets SET sellerName = :name, sellerCollege = :college, sellerTech = :tech, sellerSem = :sem, sellerPhone = :phone WHERE sellerId = :sellerId")
    suspend fun updateSellerInfoForSets(sellerId: String, name: String, college: String, tech: String, sem: String, phone: String)

    @Query("SELECT id FROM book_sets WHERE isSaved = 1")
    fun getSavedBookSetIdsFlow(): Flow<List<String>>

    @Query("DELETE FROM book_sets")
    suspend fun clearAllBookSets()

    @Query("DELETE FROM included_books")
    suspend fun clearAllIncludedBooks()
}

@Dao
interface MegaDealDao {
    @Query("SELECT * FROM mega_deals ORDER BY createdAt DESC")
    fun getAllMegaDealsFlow(): Flow<List<MegaDealEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMegaDeal(deal: MegaDealEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMegaDeals(deals: List<MegaDealEntity>)

    @Query("UPDATE mega_deals SET status = :status WHERE dealId = :dealId")
    suspend fun updateMegaDealStatus(dealId: String, status: String)

    @Query("DELETE FROM mega_deals")
    suspend fun clearAllMegaDeals()
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM conversations ORDER BY timestamp DESC")
    fun getAllConversationsFlow(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :convId")
    fun getMessagesForConversationFlow(convId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages")
    fun getAllMessagesFlow(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conv: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(convs: List<ConversationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(msg: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(msgs: List<ChatMessageEntity>)

    @Query("UPDATE conversations SET lastMessage = :lastMsg, timestamp = :timestamp WHERE id = :convId")
    suspend fun updateConversationSnippet(convId: String, lastMsg: String, timestamp: String)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :convId")
    suspend fun markConversationRead(convId: String)

    @Query("SELECT * FROM conversations WHERE id = :convId LIMIT 1")
    suspend fun getConversationById(convId: String): ConversationEntity?

    @Query("UPDATE conversations SET deletedByUserIds = :deletedByUserIds WHERE id = :convId")
    suspend fun updateDeletedByUserIds(convId: String, deletedByUserIds: String)

    @Query("DELETE FROM conversations")
    suspend fun clearAllConversations()

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotificationsFlow(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notif: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifs: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM notifications")
    suspend fun clearAllNotifications()
}
