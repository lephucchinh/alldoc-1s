package com.cherry.doc.util

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.cherry.doc.data.model.PdfCheckResult
import com.cherry.doc.data.model.SaveImagesResult
import com.cherry.doc.data.model.SavePdfResult
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import java.io.File
import java.io.FileInputStream
import kotlin.math.min

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
        password: String,
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

    fun removePdfPasswordByPath(
        context: Context,
        path: String,
        password: String
    ): SavePdfResult {

        val TAG = "RemovePdfPassword"

        val inputFile = File(path)
        if (!inputFile.exists() || inputFile.length() < 10) {
            Log.e(TAG, "Invalid PDF file: $path")
            Toast.makeText(context, "Invalid PDF file", Toast.LENGTH_SHORT).show()
            return SavePdfResult.Error("Invalid PDF file")
        }

        // 1️⃣ file tạm (cache)
        val tempFile = File.createTempFile(
            "unlock_",
            ".pdf",
            context.cacheDir
        )

        return try {
            // 2️⃣ load + remove security → save temp
            PDDocument.load(inputFile, password).use { document ->
                document.isAllSecurityToBeRemoved = true
                document.save(tempFile)
            }

            // 3️⃣ VERIFY: mở lại KHÔNG password
            try {
                PDDocument.load(tempFile).use { }
            } catch (e: Exception) {
                throw IllegalStateException("Unlock verification failed")
            }

            // 4️⃣ xoá file gốc
            if (!inputFile.delete()) {
                throw IllegalStateException("Cannot delete original PDF")
            }

            // 5️⃣ ghi đè file mới
            if (!tempFile.copyTo(inputFile, overwrite = true).exists()) {
                throw IllegalStateException("Failed to overwrite PDF")
            }

            Log.d(TAG, "PDF password removed and replaced: ${inputFile.absolutePath}")
            Toast.makeText(context, "PDF unlocked successfully", Toast.LENGTH_SHORT).show()

            SavePdfResult.Success(
                uri = null,
                path = inputFile.absolutePath
            )

        } catch (e: com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException) {
            Log.e(TAG, "Incorrect password", e)
            Toast.makeText(context, "Incorrect password", Toast.LENGTH_SHORT).show()
            SavePdfResult.Error("Incorrect password")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove PDF password", e)
            Toast.makeText(context, "Failed to unlock PDF", Toast.LENGTH_SHORT).show()
            SavePdfResult.Error(
                e.message ?: "Failed to remove PDF password"
            )

        } finally {
            // dọn file tạm nếu còn
            if (tempFile.exists()) tempFile.delete()
        }
    }



    private fun unlockInternal(
        context: Context,
        src: File,
        password: String,
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
        subFolder: String = "MyPDF",
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
            val realPath = buildExternalFilePath(subFolder, fileName)

            SavePdfResult.Success(uri, path = realPath)

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
        subFolder: String = "ScannedImages",
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


    fun createPdfFromImagesToExternal(
        context: Context,
        images: List<Uri>,
        fileName: String,
        subFolder: String = "MyPDF"
    ): SavePdfResult {

        if (images.isEmpty()) {
            return SavePdfResult.Error("Image list is empty")
        }

        val resolver = context.contentResolver
        val pdfDocument = PdfDocument()
        var currentPage: PdfDocument.Page? = null

        val finalName =
            if (fileName.endsWith(".pdf", true)) fileName else "$fileName.pdf"

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, finalName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOCUMENTS}/$subFolder"
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = resolver.insert(
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            values
        ) ?: return SavePdfResult.Error("Cannot create external pdf")

        try {
            images.forEachIndexed { index, imageUri ->
                val pageInfo = PdfDocument.PageInfo.Builder(
                    595, 842, index + 1 // A4
                ).create()

                currentPage = pdfDocument.startPage(pageInfo)
                val canvas = currentPage!!.canvas

                val bitmap = loadBitmapFromUri(context, imageUri)
                if (bitmap != null) {
                    val scaled = scaleBitmapToFit(bitmap, 595, 842)
                    val left = (595 - scaled.width) / 2f
                    val top = (842 - scaled.height) / 2f

                    canvas.drawBitmap(scaled, left, top, null)

                    if (scaled != bitmap) scaled.recycle()
                    bitmap.recycle()
                }

                pdfDocument.finishPage(currentPage!!)
                currentPage = null
            }

            resolver.openOutputStream(uri)?.use { output ->
                pdfDocument.writeTo(output)
            } ?: throw IllegalStateException("Cannot open output stream")

            // ✅ đánh dấu ghi xong
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            val realPath = buildExternalFilePath(subFolder, finalName)

            return SavePdfResult.Success(uri, path = realPath)

        } catch (e: Exception) {
            e.printStackTrace()
            resolver.delete(uri, null, null)
            return SavePdfResult.Error(e.message ?: "Create pdf failed")
        } finally {
            currentPage?.let {
                pdfDocument.finishPage(it)
            }
            pdfDocument.close()
        }
    }



    private fun loadBitmapFromUri(
        context: Context,
        uri: Uri
    ): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    // 🔥 QUAN TRỌNG
                    decoder.isMutableRequired = true
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    private fun scaleBitmapToFit(
        bitmap: Bitmap,
        maxWidth: Int,
        maxHeight: Int
    ): Bitmap {
        val ratio = min(
            maxWidth.toFloat() / bitmap.width,
            maxHeight.toFloat() / bitmap.height
        )

        val width = (bitmap.width * ratio).toInt()
        val height = (bitmap.height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }


    fun createEmptyPdfToExternal(
        context: Context,
        fileName: String,
        subFolder: String = "MyPDF"
    ): SavePdfResult {

        val resolver = context.contentResolver
        val finalName =
            if (fileName.endsWith(".pdf", true)) fileName else "$fileName.pdf"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, finalName)
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
        ) ?: return SavePdfResult.Error("Cannot create pdf file")

        val pdfDocument = PdfDocument()
        var page: PdfDocument.Page? = null

        try {
            // ✅ Tạo 1 trang trắng A4
            val pageInfo = PdfDocument.PageInfo.Builder(
                595, 842, 1
            ).create()

            page = pdfDocument.startPage(pageInfo)

            // (không vẽ gì cả → trang trắng)
            pdfDocument.finishPage(page)
            page = null

            resolver.openOutputStream(uri)?.use { output ->
                pdfDocument.writeTo(output)
            } ?: throw IllegalStateException("Cannot open output stream")

            // ✅ Đánh dấu ghi xong
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
            val realPath = buildExternalFilePath(subFolder, finalName)

            return SavePdfResult.Success(uri, path = realPath)

        } catch (e: Exception) {
            e.printStackTrace()
            resolver.delete(uri, null, null)
            return SavePdfResult.Error(e.message ?: "Create empty pdf failed")
        } finally {
            page?.let { pdfDocument.finishPage(it) }
            pdfDocument.close()
        }
    }

    fun buildExternalFilePath(
        subFolder: String,
        fileName: String
    ): String {
        return File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS
            ),
            "$subFolder/$fileName"
        ).absolutePath
    }

    fun checkPdfByPath(path: String): PdfCheckResult {
        val file = File(path)
        if (!file.exists() || file.length() < 10) {
            return PdfCheckResult.INVALID_PDF
        }

        // check header %PDF-
        try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(5)
                if (fis.read(header) != 5) return PdfCheckResult.INVALID_PDF
                if (String(header) != "%PDF-") return PdfCheckResult.INVALID_PDF
            }
        } catch (e: Exception) {
            return PdfCheckResult.INVALID_PDF
        }

        return try {
            PDDocument.load(file).use {
                PdfCheckResult.OK
            }
        } catch (e: com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException) {
            PdfCheckResult.PASSWORD_PROTECTED
        } catch (e: Exception) {
            PdfCheckResult.INVALID_PDF
        }
    }



}