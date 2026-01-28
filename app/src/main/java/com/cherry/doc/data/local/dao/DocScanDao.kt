package com.cherry.doc.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cherry.doc.data.local.entity.DocScanEntity

@Dao
interface DocScanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pdf: DocScanEntity)

    @Query("SELECT * FROM pdf_table ORDER BY createdAt DESC")
    suspend fun getAll(): List<DocScanEntity>

    @Delete
    suspend fun delete(pdf: DocScanEntity)
}
