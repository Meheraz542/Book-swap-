package com.aistudio.bookset.market.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aistudio.bookset.market.data.model.BookSet
import com.aistudio.bookset.market.data.model.MegaDeal
import com.aistudio.bookset.market.data.model.OrderItem
import com.aistudio.bookset.market.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface BookSetDao {
    @Query("SELECT * FROM book_sets")
    fun getAllBookSets(): Flow<List<BookSet>>

    @Query("SELECT * FROM book_sets WHERE id = :id LIMIT 1")
    suspend fun getBookSetById(id: String): BookSet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookSets(bookSets: List<BookSet>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookSet(bookSet: BookSet)

    @Query("DELETE FROM book_sets WHERE id = :id")
    suspend fun deleteBookSet(id: String)

    @Query("SELECT * FROM mega_deals")
    fun getAllMegaDeals(): Flow<List<MegaDeal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMegaDeals(deals: List<MegaDeal>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMegaDeal(deal: MegaDeal)

    @Query("SELECT * FROM user_profiles WHERE uid = :uid LIMIT 1")
    suspend fun getUserProfile(uid: String): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(user: UserProfile)

    @Query("SELECT * FROM orders ORDER BY orderDate DESC")
    fun getAllOrders(): Flow<List<OrderItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderItem)
}
