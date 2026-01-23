package com.cherry.doc.util

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import java.io.File

object FileManager {
    fun deleteFileSmart(
        context: Context,
        path: String,
    ): Boolean {

        if (path.isBlank()) return false

        val file = File(path)
        if (!file.exists()) return false

        // 1️⃣ File nằm trong app sandbox → xoá trực tiếp
        if (path.startsWith(context.filesDir.path)
            || path.startsWith(context.cacheDir.path)
        ) {
            return file.delete()
        }

        // 2️⃣ Android 11+ + có All Files Access → xoá trực tiếp
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                return file.delete()
            }
        }

        // 3️⃣ Android 10+ → xoá qua MediaStore (CHUẨN)
        return deleteViaMediaStore(context, path)
    }

    private fun deleteViaMediaStore(
        context: Context,
        path: String,
    ): Boolean {

        val collection = MediaStore.Files.getContentUri("external")

        val projection = arrayOf(MediaStore.Files.FileColumns._ID)
        val selection = MediaStore.Files.FileColumns.DATA + "=?"
        val selectionArgs = arrayOf(path)

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->

            if (cursor.moveToFirst()) {
                val id = cursor.getLong(0)
                val deleteUri = ContentUris.withAppendedId(collection, id)

                return context.contentResolver.delete(deleteUri, null, null) > 0
            }
        }

        return false
    }

    fun isPdfEncrypted(file: File): Boolean {
        return try {
            PDDocument.load(file).close()
            false
        } catch (e: InvalidPasswordException) {
            true
        }
    }

    fun unlockPdfToCache(
        context: Context,
        src: File,
        password: String
    ): File? {
        try {
            // 1️⃣ Thử bằng password user
            return unlockInternal(context, src, password)
        } catch (_: InvalidPasswordException) {
            // 2️⃣ Thử password rỗng (owner-only PDF)
            return try {
                unlockInternal(context, src, "")
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun unlockInternal(
        context: Context,
        src: File,
        password: String
    ): File {
        val document = PDDocument.load(src, password)

        document.isAllSecurityToBeRemoved = true

        val outFile = File(
            context.cacheDir,
            "unlocked_${System.currentTimeMillis()}.pdf"
        )

        document.save(outFile)
        document.close()

        return outFile
    }


}