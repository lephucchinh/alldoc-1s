package com.cherry.doc.ui.widgets

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import com.cherry.doc.databinding.FragmentDialogLoadingBinding

class DialogLoading(context: Context) : Dialog(context) {

    private lateinit var binding: FragmentDialogLoadingBinding

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(false)
        setCanceledOnTouchOutside(false)

        binding = FragmentDialogLoadingBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }
    
    override fun show() {
        if (!isShowing) {
            super.show()
            binding.animLoading.playAnimation()
        }
    }

    override fun dismiss() {
        if (isShowing) {
            binding.animLoading.cancelAnimation()
            super.dismiss()
        }
    }
}
