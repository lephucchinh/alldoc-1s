package com.cherry.doc.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cherry.doc.data.local.entity.DocHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pdf: DocHistoryEntity)

    @Query("""
        SELECT * FROM doc_history_table 
        ORDER BY updatedAt DESC
    """)
    fun getAll(): Flow<List<DocHistoryEntity>>

    @Delete
    suspend fun delete(pdf: DocHistoryEntity)
}
