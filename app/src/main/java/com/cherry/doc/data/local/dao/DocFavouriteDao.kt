package com.cherry.doc.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cherry.doc.data.local.entity.DocFavouriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocFavouriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pdf: DocFavouriteEntity)

    @Query("SELECT * FROM doc_favourite_table ORDER BY path DESC")
    fun getAll(): Flow<List<DocFavouriteEntity>>

    @Query("DELETE FROM doc_favourite_table WHERE path = :path")
    suspend fun deleteByPath(path: String)

    @Query("SELECT EXISTS(SELECT 1 FROM doc_favourite_table WHERE path = :path)")
    suspend fun isFavourite(path: String): Boolean

    @Query("UPDATE doc_favourite_table SET path = :newPath WHERE path = :oldPath")
    suspend fun updatePath(oldPath: String, newPath: String)

}
