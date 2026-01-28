package com.cherry.doc.util
import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.cherry.doc.data.PdfSource
import com.cherry.doc.data.SavePdfResult
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File

object PdfMergeUtil {

    fun mergePdfFilesToExternal(
        context: Context,
        files: List<File>,
        outputName: String,
        subFolder: String = "MyPDF"
    ): SavePdfResult {

        if (files.size < 2) {
            return SavePdfResult.Error("Need at least 2 PDF files")
        }

        // 🔎 CHECK PASSWORD TRƯỚC
        files.forEach { file ->
            if (isPdfPasswordProtected(file)) {
                return SavePdfResult.Error(
                    "Some PDF files are password-protected. Please remove the password before merging."
                )
            }
        }

        val resolver = context.contentResolver
        val finalName =
            if (outputName.endsWith(".pdf", true)) outputName else "$outputName.pdf"

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
        ) ?: return SavePdfResult.Error("Cannot create output pdf")

        return try {
            val merger = PDFMergerUtility()

            resolver.openOutputStream(uri)?.use { output ->
                merger.destinationStream = output

                files.forEach { file ->
                    merger.addSource(file)
                }

                merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())
            } ?: throw IllegalStateException("Cannot open output stream")

            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)

            val realPath = File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS
                ),
                "$subFolder/$finalName"
            ).absolutePath

            SavePdfResult.Success(
                uri = uri,
                path = realPath
            )

        } catch (e: Exception) {
            e.printStackTrace()
            resolver.delete(uri, null, null)
            SavePdfResult.Error(
                e.message ?: "Merge pdf failed"
            )
        }
    }

    fun isPdfPasswordProtected(file: File): Boolean {
        if (!file.exists() || file.length() < 10) return false

        return try {
            PDDocument.load(file).use {
                false // load được → không password
            }
        } catch (e: Exception) {
            true // load fail → có thể là password
        }
    }

}
