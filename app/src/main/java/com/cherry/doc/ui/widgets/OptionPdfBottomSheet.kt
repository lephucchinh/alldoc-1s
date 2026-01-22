package com.cherry.doc.ui.widgets

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cherry.doc.R
import com.cherry.doc.data.DocInfo
import com.cherry.doc.databinding.BottomSheetOptionBinding
import com.cherry.doc.util.formatDateTime
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class OptionPdfBottomSheet(
    private val docInfo: DocInfo,
    private val listener: Listener,
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetOptionBinding? = null
    private val binding get() = _binding!!

    interface Listener {
        fun onAddFavourite(doc: DocInfo)
        fun onMergePdf(doc: DocInfo)
        fun onSplitPdf(doc: DocInfo)
        fun onLockPdf(doc: DocInfo)
        fun onDelete(doc: DocInfo)
        fun onShare(doc: DocInfo)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetOptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindData()
        bindActions()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(
            requireContext(),
            R.style.BottomSheetOptionStyle
        )
    }


    private fun bindData() = with(binding) {
        // icon
        val icon = docInfo.getTypeIcon()
        if (icon != -1) imgFile.setImageResource(icon)

        txtTools.text = docInfo.getNormalizedFileType()
        txtNameFile.text = docInfo.fileName.orEmpty()
        val (date, time) = docInfo.lastModified.formatDateTime()
        txtDate.text = date
        txtTime.text = time
    }

    private fun bindActions() = with(binding) {

        btnAddFavourite.setOnClickListener {
            listener.onAddFavourite(docInfo)
            dismiss()
        }

        btnMergePdf.setOnClickListener {
            listener.onMergePdf(docInfo)
            dismiss()
        }

        btnSplitPdf.setOnClickListener {
            listener.onSplitPdf(docInfo)
            dismiss()
        }

        btnLockPdf.setOnClickListener {
            listener.onLockPdf(docInfo)
            dismiss()
        }

        btnDelete.setOnClickListener {
            listener.onDelete(docInfo)
            dismiss()
        }

        btnShare.setOnClickListener {
            listener.onShare(docInfo)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
