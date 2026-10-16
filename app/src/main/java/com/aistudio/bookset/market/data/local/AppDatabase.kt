package com.aistudio.bookset.market.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aistudio.bookset.market.data.model.BookSet
import com.aistudio.bookset.market.data.model.MegaDeal
import com.aistudio.bookset.market.data.model.OrderItem
import com.aistudio.bookset.market.data.model.UserProfile

@Database(
    entities = [BookSet::class, MegaDeal::class, UserProfile::class, OrderItem::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookSetDao(): BookSetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bookset_market_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
