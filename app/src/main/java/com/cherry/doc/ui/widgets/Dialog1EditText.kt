package com.cherry.doc.ui.widgets

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.cherry.doc.databinding.Dialog1EditTextBinding

class Dialog1EditTextFragment : DialogFragment() {

    private var _binding: Dialog1EditTextBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Translucent_NoTitleBar)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = Dialog1EditTextBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.setCancelable(true)

        val args = requireArguments()
        val resultKey = args.getString(KEY_RESULT_KEY)
            ?: throw IllegalStateException("Dialog1EditTextFragment requires RESULT_KEY")

        binding.txtTitle.text = args.getString(KEY_TITLE)
        binding.edtDialog.setText(args.getString(KEY_DEFAULT_TEXT))
        binding.txtPositive.text = args.getString(KEY_POSITIVE_TEXT)
        binding.txtNegative.text = args.getString(KEY_NEGATIVE_TEXT)

        binding.txtPositive.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                resultKey,
                bundleOf(RESULT_TEXT to binding.edtDialog.text.toString())
            )
            dismiss()
        }

        binding.txtNegative.setOnClickListener { dismiss() }

        // focus + keyboard
        binding.edtDialog.requestFocus()
        binding.edtDialog.post {
            val imm = requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.edtDialog, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        // ===== ARGUMENT KEYS =====
        const val KEY_TITLE = "key_title"
        const val KEY_DEFAULT_TEXT = "key_default_text"
        const val KEY_POSITIVE_TEXT = "key_positive_text"
        const val KEY_NEGATIVE_TEXT = "key_negative_text"
        const val KEY_RESULT_KEY = "key_result_key"
        const val RESULT_TEXT = "result_text"
        // ===== RESULT KEYS =====
        const val RESULT_KEY_ALL_APP = "DialogAllFileRename"
        const val RESULT_KEY_FAVOURITE = "DialogFavouriteRename"
        const val RESULT_KEY_EXCEL = "DialogExcelRename"
        const val RESULT_KEY_PDF = "DialogPdfRename"
        const val RESULT_KEY_PPT = "DialogPptRename"
        const val RESULT_KEY_WORD = "DialogWordRename"
        const val RESULT_KEY_PASSWORD_ALL_FILE = "RESULT_KEY_PASSWORD_ALL_FILE"
        const val RESULT_KEY_PASSWORD_FAVOURITE = "RESULT_KEY_PASSWORD_FAVOURITE"
        const val RESULT_KEY_UNLOCK_ALL_FILE = "RESULT_KEY_UNLOCK_ALL_FILE"
        const val RESULT_KEY_PASSWORD_EXCEL = "RESULT_KEY_PASSWORD_EXCEL"
        const val RESULT_KEY_PASSWORD_PDF = "RESULT_KEY_PASSWORD_PDF"
        const val RESULT_KEY_UNLOCK_PDF = "RESULT_KEY_UNLOCK_PDF"
        const val RESULT_KEY_PASSWORD_PPT = "RESULT_KEY_PASSWORD_PPT"
        const val RESULT_KEY_PASSWORD_WORD = "RESULT_KEY_PASSWORD_WORD"



        fun newInstance(
            title: String,
            defaultText: String?,
            positiveText: String,
            negativeText: String,
            resultKey: String,
        ) = Dialog1EditTextFragment().apply {
            arguments = bundleOf(
                KEY_TITLE to title,
                KEY_DEFAULT_TEXT to defaultText,
                KEY_POSITIVE_TEXT to positiveText,
                KEY_NEGATIVE_TEXT to negativeText,
                KEY_RESULT_KEY to resultKey

            )
        }
    }
}



