package com.cherry.doc.ui.widgets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.cherry.doc.R
import com.cherry.doc.databinding.FragmentDialogSetPasswordBinding

class DialogSetPasswordFragment(
    private val onConfirm: (String) -> Unit
) : DialogFragment() {

    private lateinit var binding: FragmentDialogSetPasswordBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDialogSetPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.txtPositive.setOnClickListener {
            val pass1 = binding.edtSetPassword.text.toString()
            val pass2 = binding.edtConfirmPassword.text.toString()

            when {
                pass1.isBlank() || pass2.isBlank() -> {
                    toast(requireContext().getString(R.string.error_password_empty))
                }
                pass1.length < 4 -> {
                    toast(requireContext().getString(R.string.error_password_too_short))
                }
                pass1 != pass2 -> {
                    toast(requireContext().getString(R.string.error_password_not_match))
                }
                else -> {
                    onConfirm(pass1)
                    dismiss()
                }
            }
        }

        binding.txtNegative.setOnClickListener {
            dismiss()
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
