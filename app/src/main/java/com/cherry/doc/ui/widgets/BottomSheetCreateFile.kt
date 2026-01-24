package com.cherry.doc.ui.widgets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cherry.doc.databinding.BottomSheetCreateFileBinding
import com.cherry.doc.util.setSingleClickListener
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BottomSheetCreateFile(
    private val listener: Listener
) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetCreateFileBinding

    interface Listener {
        fun onImageToPdf()
        fun onScanPdf()
        fun onMergePdf()
        fun onCreatePdf()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetCreateFileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerListener()
    }

    private fun registerListener() = with(binding) {

        btnImageToPdf.setSingleClickListener {
            dismiss()
            listener.onImageToPdf()
        }

        btnScannerPdf.setSingleClickListener {
            dismiss()
            listener.onScanPdf()
        }

        btnMergeFile.setSingleClickListener {
            dismiss()
            listener.onMergePdf()
        }

        btnCreatePdf.setSingleClickListener {
            dismiss()
            listener.onCreatePdf()
        }
    }
}
