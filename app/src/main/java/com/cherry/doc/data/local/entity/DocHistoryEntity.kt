package com.cherry.doc.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "doc_history_table")
data class DocHistoryEntity(
    @PrimaryKey
    val path: String,
    val updatedAt: Long = System.currentTimeMillis()
)