package com.cherry.doc.ui.widgets

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.cherry.doc.R
import com.cherry.doc.data.local.AppDatabase
import com.cherry.doc.data.model.DocInfo
import com.cherry.doc.data.model.PdfCheckResult
import com.cherry.doc.databinding.BottomSheetOptionBinding
import com.cherry.doc.util.FileManager.checkPdfByPath
import com.cherry.doc.util.formatDateTime
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

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
        docInfo.path?.let { path ->
            val hasPassword = checkPdfHavePassword(path)

            btnLockPdf.setText(
                if (hasPassword)
                    R.string.text_unlock_pdf
                else
                    R.string.text_lock_pdf
            )

            btnLockPdf.setCompoundDrawablesWithIntrinsicBounds(
                if (hasPassword)
                    R.drawable.ic_unlock   // drawableStart
                else
                    R.drawable.ic_lock,
                0,
                0,
                0
            )
        }
        viewLifecycleOwner.lifecycleScope.launch {
            docInfo.path?.let {
                btnAddFavourite.isVisible =
                    AppDatabase.getInstance().docFavouriteDao().isFavourite(it).not()
            }
        }
        // icon
        btnLockPdf.isVisible = (docInfo.getFileType() == "PDF")
        btnMergePdf.isVisible = (docInfo.getFileType() == "PDF")
        btnSplitPdf.isVisible = (docInfo.getFileType() == "PDF")
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

    private fun checkPdfHavePassword(filePath: String): Boolean {
        return when (checkPdfByPath(filePath)) {
            PdfCheckResult.OK -> false

            PdfCheckResult.PASSWORD_PROTECTED -> true

            PdfCheckResult.INVALID_PDF -> false
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
