package com.cherry.doc.ui.widgets

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.annotation.DrawableRes
import androidx.fragment.app.DialogFragment
import com.cherry.doc.databinding.FragmentDialogDeleteBinding
import java.io.File

class DialogFragmentDelete(
    private val fileName: String,
    private val fileType: String,
    @DrawableRes private val fileImage: Int,
    private val date: String,
    private val time: String,
    private val listener: OnDeleteConfirmListener
) : DialogFragment() {

    private var _binding: FragmentDialogDeleteBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDialogDeleteBinding.inflate(inflater, container, false)
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // bind data
        binding.txtNameFile.text = fileName
        binding.txtDate.text = date
        binding.txtTools.text = fileType
        binding.txtTime.text = time
        binding.imgFile.setImageResource(fileImage)
        // click actions
        binding.txtPositive.setOnClickListener {
            listener.onDelete()
            dismiss()
        }

        binding.txtNegative.setOnClickListener {
            listener.onCancel()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

interface OnDeleteConfirmListener {
    fun onDelete()
    fun onCancel()
}

