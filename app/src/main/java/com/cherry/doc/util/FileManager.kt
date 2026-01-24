package com.cherry.doc.util

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.cherry.doc.data.SaveImagesResult
import com.cherry.doc.data.SavePdfResult
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

    fun renameAndSavePdfToExternal(
        context: Context,
        sourceFile: File,
        newName: String,
        subFolder: String = "MyPDF"
    ): SavePdfResult {

        if (!sourceFile.exists()) {
            return SavePdfResult.Error("Source file not found")
        }

        val resolver = context.contentResolver
        val fileName = if (newName.endsWith(".pdf", true)) newName else "$newName.pdf"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOCUMENTS}/$subFolder"
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = resolver.insert(
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            contentValues
        ) ?: return SavePdfResult.Error("Cannot create external file")

        return try {
            resolver.openOutputStream(uri)?.use { output ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: return SavePdfResult.Error("Cannot open output stream")

            // đánh dấu ghi xong
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)

            // xoá file tạm
            sourceFile.delete()

            SavePdfResult.Success(uri)

        } catch (e: Exception) {
            e.printStackTrace()
            resolver.delete(uri, null, null)
            SavePdfResult.Error(e.message ?: "Unknown error")
        }
    }



    fun saveImagesToExternal(
        context: Context,
        images: List<Uri>,
        baseName: String,
        subFolder: String = "ScannedImages"
    ): SaveImagesResult {

        if (images.isEmpty()) {
            return SaveImagesResult.Error("Image list is empty")
        }

        val resolver = context.contentResolver
        val savedUris = mutableListOf<Uri>()

        images.forEachIndexed { index, sourceUri ->

            val fileName = "${baseName}_${index + 1}.jpg"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/$subFolder"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val targetUri = resolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return SaveImagesResult.Error("Cannot create image uri")

            try {
                resolver.openInputStream(sourceUri)?.use { input ->
                    resolver.openOutputStream(targetUri)?.use { output ->
                        input.copyTo(output)
                    }
                } ?: return SaveImagesResult.Error("Cannot open image stream")

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(targetUri, contentValues, null, null)

                savedUris.add(targetUri)

            } catch (e: Exception) {
                e.printStackTrace()
                resolver.delete(targetUri, null, null)
                return SaveImagesResult.Error(e.message ?: "Unknown error")
            }
        }

        return SaveImagesResult.Success(savedUris)
    }



}