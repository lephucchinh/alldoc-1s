package com.cherry.doc.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "doc_favourite_table")
data class DocFavouriteEntity(
    @PrimaryKey val path: String
)
