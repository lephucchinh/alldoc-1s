package com.cherry.doc.util
import com.cherry.doc.data.PdfSource
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File

object PdfMergeUtil {

    fun mergePdfs(
        inputs: List<PdfSource>,
        outputPath: String
    ): Boolean {
        return try {
            val outputDoc = PDDocument(MemoryUsageSetting.setupTempFileOnly())

            inputs.forEach { input ->
                val file = File(input.pdfInfo.path)

                val doc = if (input.password.isNullOrEmpty()) {
                    PDDocument.load(file)
                } else {
                    PDDocument.load(file, input.password)
                }

                doc.use {
                    it.pages.forEach { page ->
                        outputDoc.importPage(page)
                    }
                }
            }

            outputDoc.save(outputPath)
            outputDoc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
