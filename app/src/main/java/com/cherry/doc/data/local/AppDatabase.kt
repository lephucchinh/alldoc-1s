package com.cherry.doc.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cherry.doc.data.local.dao.DocScanDao
import com.cherry.doc.data.local.entity.DocScanEntity

@Database(
    entities = [DocScanEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun docScanDao(): DocScanDao

    class Singleton private constructor(context: Context) {

        val database: AppDatabase = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "app_database.db"
        ).build()

        companion object {
            @Volatile
            private var INSTANCE: Singleton? = null

            fun getInstance(context: Context): Singleton {
                return INSTANCE ?: synchronized(this) {
                    INSTANCE ?: Singleton(context).also {
                        INSTANCE = it
                    }
                }
            }
        }
    }
}