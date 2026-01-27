package com.cherry.doc.ui.mergepdf

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cherry.doc.data.DocInfo
import com.cherry.doc.data.SavePdfResult
import com.cherry.doc.databinding.ActivityMergePdfBinding
import com.cherry.doc.repository.FilesHelper
import com.cherry.doc.repository.FilesHelper.loadAllFiles
import com.cherry.doc.ui.createdsuccess.PdfFileCreateSuccessActivity
import com.cherry.doc.util.PdfMergeUtil.mergePdfFilesToExternal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MergePdfActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMergePdfBinding
    private val selectedFiles = mutableListOf<DocInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMergePdfBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        registerListener()
    }

    private fun setupRecyclerView() {
        binding.rcvFiles.adapter = MergePdfAdapter { list ->
            selectedFiles.clear()
            selectedFiles.addAll(list)
        }
    }

    private fun registerListener() {
        binding.btnMergePdf.setOnClickListener {
            if (selectedFiles.size < 2) {
                Toast.makeText(this, "Select at least 2 PDFs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            mergePdf()
        }
    }

    private fun mergePdf() {
        lifecycleScope.launch {
            val files = selectedFiles.mapNotNull { it.path }.map { File(it) }

            val result = withContext(Dispatchers.IO) {
                mergePdfFilesToExternal(
                    context = this@MergePdfActivity,
                    files = files,
                    outputName = "Merged PDF ${System.currentTimeMillis()}"
                )
            }

            when (result) {
                is SavePdfResult.Success -> {
                    loadAllFiles()
                    FilesHelper.getDocByPath(result.path)
                        ?.let { PdfFileCreateSuccessActivity.start(this@MergePdfActivity, it) }
                }

                is SavePdfResult.Error -> {
                    Toast.makeText(this@MergePdfActivity, result.reason, Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

}
