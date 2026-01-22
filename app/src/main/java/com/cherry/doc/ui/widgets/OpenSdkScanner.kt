package com.cherry.doc.ui.widgets

import android.app.Activity
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream

object OpenSdkScanner {
    fun registerDocumentScanner(
        activity: AppCompatActivity,
        onImagesResult: (List<Uri>) -> Unit,
        onPdfResult: (File?) -> Unit,
    ): ActivityResultLauncher<IntentSenderRequest> {

        return activity.registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->

            if (result.resultCode != Activity.RESULT_OK) {
                onImagesResult(emptyList())
                onPdfResult(null)
                return@registerForActivityResult
            }

            val scanResult =
                GmsDocumentScanningResult.fromActivityResultIntent(result.data)

            // Images
            val images = scanResult?.pages?.map { it.imageUri } ?: emptyList()
            onImagesResult(images)

            // PDF
            val pdfFile = scanResult?.pdf?.let { pdf ->
                val file = File(activity.filesDir, "scan_${System.currentTimeMillis()}.pdf")
                activity.contentResolver.openInputStream(pdf.uri)?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                file
            }

            onPdfResult(pdfFile)
        }
    }

    fun startScanDocument(
        activity: Activity,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        pageLimit: Int = 5,
    ) {
        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setPageLimit(pageLimit)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .build()

        val scanner = GmsDocumentScanning.getClient(options)

        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                launcher.launch(
                    IntentSenderRequest.Builder(intentSender).build()
                )
            }
    }
}