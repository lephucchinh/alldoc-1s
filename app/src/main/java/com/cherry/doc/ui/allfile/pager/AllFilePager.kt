package com.cherry.doc.ui.allfile.pager

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.cherry.doc.data.DocInfo
import com.cherry.doc.databinding.PageAllFileBinding
import com.cherry.doc.ui.allfile.AllFileViewModel
import com.cherry.doc.ui.allfile.adapter.AllFileAdapter
import kotlinx.coroutines.launch

class AllFilePager : Fragment() {

    private var _binding: PageAllFileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AllFileViewModel by activityViewModels()

    private lateinit var adapter: AllFileAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = PageAllFileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadData()
    }

    private fun setupRecyclerView() {
        adapter = AllFileAdapter(listener = object : AllFileAdapter.Listener {
            override fun onItemClick(item: DocInfo) {
                // TODO: open file
            }

            override fun onShare(item: DocInfo) {
                // TODO: share file
            }

            override fun onRename(item: DocInfo) {
                // TODO: rename file
            }

            override fun onOption(item: DocInfo) {
                // TODO: show bottom sheet / popup
            }
        })

        binding.rcvFiles.adapter = adapter
        binding.rcvFiles.setHasFixedSize(true)
    }



    private fun loadData() {
        viewModel.allFiles.observe(viewLifecycleOwner) { groups ->
            Log.d("AllFilePager", "groups size = ${groups.size}")

            val allFiles = groups.flatMap { it.docList.orEmpty() }
            Log.d("AllFilePager", "files size = ${allFiles.size}")

            adapter.submitList(allFiles)
        }

        viewModel.loadAllFiles()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // =====================================================
    // FAKE DATA – TEST
    // =====================================================
    private fun fakeDocs(): List<DocInfo> {
        return listOf(
            DocInfo().apply {
                fileName = "PDF Scanner.pdf"
                path = "/storage/pdf_scanner.pdf"
                lastModified = System.currentTimeMillis().toString()
                fileSize = "1.2 MB"
            },
            DocInfo().apply {
                fileName = "Report.docx"
                path = "/storage/report.docx"
                lastModified = System.currentTimeMillis().toString()
                fileSize = "850 KB"
            }
        )
    }
}
