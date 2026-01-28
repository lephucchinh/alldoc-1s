package com.cherry.doc.data.model

import android.net.Uri

sealed class SaveImagesResult {
    data class Success(val uris: List<Uri>) : SaveImagesResult()
    data class Error(val reason: String) : SaveImagesResult()
}

