package com.cherry.doc.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_table")
data class DocScanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val path: String,
    val name: String,
    val createdAt: Long
)
