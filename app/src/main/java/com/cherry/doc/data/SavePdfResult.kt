package com.cherry.doc.data

import android.net.Uri

sealed class SavePdfResult {
    data class Success(
        val uri: Uri?,
        val path: String,
    ) : SavePdfResult()

    data class Error(val reason: String) : SavePdfResult()
}
