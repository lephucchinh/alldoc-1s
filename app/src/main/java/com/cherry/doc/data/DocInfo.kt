package com.cherry.doc.data

import android.os.Parcelable
import com.cherry.doc.R
import kotlinx.parcelize.Parcelize
import java.io.File

/*
 * -----------------------------------------------------------------
 * Copyright (C) 2018-2028, by Victor, All rights reserved.
 * -----------------------------------------------------------------
 * File: DocInfo
 * Author: Victor
 * Date: 2023/10/26 10:30
 * Description:
 * -----------------------------------------------------------------
 */

@Parcelize
class DocInfo : Parcelable {

    var album: String? = null
    var fileName: String? = null
    var path: String? = null
    var mimeType: String? = null
    var lastModified: String? = null
    var fileSize: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DocInfo) return false
        return path == other.path
    }

    override fun hashCode(): Int {
        return path?.hashCode() ?: 0
    }

    fun getTypeIcon(): Int {
        if (fileName?.lowercase()?.endsWith("pdf") == true) {
            return com.cherry.lib.doc.R.drawable.pdf_ic
        }
        if (fileName?.lowercase()?.endsWith("doc") == true) {
            return com.cherry.lib.doc.R.drawable.file_doc
        }
        if (fileName?.lowercase()?.endsWith("docx") == true) {
            return com.cherry.lib.doc.R.drawable.file_doc
        }
        if (fileName?.lowercase()?.endsWith("xls") == true) {
            return com.cherry.lib.doc.R.drawable.file_xls
        }
        if (fileName?.lowercase()?.endsWith("xlsx") == true) {
            return com.cherry.lib.doc.R.drawable.file_xls
        }
        if (fileName?.lowercase()?.endsWith("ppt") == true) {
            return com.cherry.lib.doc.R.drawable.ppt_ic
        }
        if (fileName?.lowercase()?.endsWith("pptx") == true) {
            return com.cherry.lib.doc.R.drawable.ppt_ic
        }
        if (fileName?.lowercase()?.endsWith("txt") == true) {
            return com.cherry.lib.doc.R.drawable.file_txt
        }
        return -1
    }

    fun getFileType(): String? {
        return try {
            val type = path ?: ""
            type.substring(type.lastIndexOf(".")).split(".")[1].uppercase()
        } catch (e: Exception) {
            mimeType
        }
    }

    fun getNormalizedFileType(): String? {
        val ext = try {
            path?.substringAfterLast('.', "")?.lowercase()
        } catch (e: Exception) {
            null
        }

        return when (ext) {
            "doc", "docx" -> "DOC"
            "xls", "xlsx" -> "XLS"
            "ppt", "pptx" -> "PPT"
            "pdf" -> "PDF"
            "txt" -> "TXT"
            else -> mimeType?.uppercase()
        }
    }

    fun renameFileAndReturnNew(inputName: String): DocInfo? {
        val oldPath = path ?: return null
        val oldFile = File(oldPath)
        if (!oldFile.exists()) return null

        val parentDir = oldFile.parentFile ?: return null
        val ext = oldFile.extension

        var cleanName = inputName.trim()
        if (cleanName.isBlank()) return null

        if (cleanName.lowercase().endsWith(".${ext.lowercase()}")) {
            cleanName = cleanName.substringBeforeLast(".")
        }

        if (cleanName.contains(Regex("[\\\\/:*?\"<>|]"))) return null

        val newFile = File(parentDir, "$cleanName.$ext")
        if (newFile.exists()) return null

        return if (oldFile.renameTo(newFile)) {
            DocInfo().apply {
                album = this@DocInfo.album
                fileName = newFile.name
                path = newFile.absolutePath
                mimeType = this@DocInfo.mimeType
                lastModified = this@DocInfo.lastModified
                fileSize = this@DocInfo.fileSize
            }
        } else null
    }
}
