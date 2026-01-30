package com.cherry.doc.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cherry.doc.App
import com.cherry.doc.data.local.dao.DocFavouriteDao
import com.cherry.doc.data.local.dao.DocHistoryDao
import com.cherry.doc.data.local.entity.DocFavouriteEntity
import com.cherry.doc.data.local.entity.DocHistoryEntity

@Database(
    entities = [
        DocFavouriteEntity::class,
        DocHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun docFavouriteDao(): DocFavouriteDao
    abstract fun docHistoryDao(): DocHistoryDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    App.instance.applicationContext,
                    AppDatabase::class.java,
                    "app_database.db"
                ).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}
