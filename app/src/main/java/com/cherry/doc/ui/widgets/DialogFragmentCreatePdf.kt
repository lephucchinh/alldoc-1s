package com.cherry.doc.ui.widgets

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.cherry.doc.databinding.FragmentDialogCreatePdfBinding
import com.cherry.doc.util.setSingleClickListener

class DialogFragmentCreatePdf(
    private val onConfirm: (fileName: String) -> Unit
) : DialogFragment() {

    private lateinit var binding: FragmentDialogCreatePdfBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Không có title mặc định
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDialogCreatePdfBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Nền trong suốt để XML black_50 có tác dụng
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        registerListener()
    }

    private fun registerListener() = with(binding) {

        txtNegative.setSingleClickListener {
            dismiss()
        }

        txtPositive.setSingleClickListener {
            val name = edtDialog.text.toString().trim()
            if (name.isNotEmpty()) {
                dismiss()
                onConfirm(name)
            } else {
                edtDialog.error = "File name cannot be empty"
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}
