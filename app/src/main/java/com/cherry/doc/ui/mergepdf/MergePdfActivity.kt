package com.cherry.doc.ui.mergepdf

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import com.cherry.doc.R
import com.cherry.doc.data.DocInfo
import com.cherry.doc.data.SavePdfResult
import com.cherry.doc.databinding.ActivityMergePdfBinding
import com.cherry.doc.repository.FilesHelper
import com.cherry.doc.repository.FilesHelper.loadAllFiles
import com.cherry.doc.ui.createdsuccess.PdfFileCreateSuccessActivity
import com.cherry.doc.ui.mergepdf.adapter.MergeDragCallback
import com.cherry.doc.ui.mergepdf.adapter.MergePdfAdapter
import com.cherry.doc.util.PdfMergeUtil.mergePdfFilesToExternal
import com.cherry.doc.util.hideSystemBars
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MergePdfActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMergePdfBinding
    private val selectedFiles = mutableListOf<DocInfo>()
    private var isMergeEnabled = false
    lateinit var mergePdfAdapter: MergePdfAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMergePdfBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hideSystemBars()
        initView()
        registerListener()
    }

    private fun initView() {

        mergePdfAdapter = MergePdfAdapter { list ->
            selectedFiles.clear()
            selectedFiles.addAll(list)
        }
        mergePdfAdapter.showAll(FilesHelper.allFiles.value.flatMap { it.docList.orEmpty() })
        binding.rcvFiles.adapter = mergePdfAdapter
        val touchHelper = ItemTouchHelper(
            MergeDragCallback(mergePdfAdapter)
        )
        touchHelper.attachToRecyclerView(binding.rcvFiles)

    }

    private fun registerListener() {
        binding.btnMergePdf.setOnClickListener {

            if (isMergeEnabled.not()) {
                if (selectedFiles.size < 2) {
                    Toast.makeText(this, "Select at least 2 PDFs", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                isMergeEnabled = true
                mergePdfAdapter.showMergeList(selectedFiles)
                binding.icAds.isVisible = isMergeEnabled
                binding.txtMerge.setText(if (isMergeEnabled) R.string.text_button_merge_file else R.string.text_button_import)
            } else {
                mergePdf()
            }
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
                        ?.let {
                            it.path?.let { path ->
                                PdfFileCreateSuccessActivity.start(
                                    this@MergePdfActivity,
                                    path
                                )
                            }
                        }
                }

                is SavePdfResult.Error -> {
                    Toast.makeText(this@MergePdfActivity, result.reason, Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

}
