package com.cherry.doc.ui.favourite

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cherry.doc.R
import com.cherry.doc.data.local.AppDatabase
import com.cherry.doc.data.model.DocInfo
import com.cherry.doc.databinding.FragmentFavouriteBinding
import com.cherry.doc.repository.FilesHelper
import com.cherry.doc.ui.favourite.adapter.FavouriteAdapter
import com.cherry.doc.ui.widgets.Dialog1EditTextFragment
import com.cherry.doc.ui.widgets.Dialog1EditTextFragment.Companion.RESULT_KEY_ALL_APP
import com.cherry.doc.ui.widgets.Dialog1EditTextFragment.Companion.RESULT_KEY_FAVOURITE
import com.cherry.doc.ui.widgets.Dialog1EditTextFragment.Companion.RESULT_KEY_PASSWORD_ALL_FILE
import com.cherry.doc.ui.widgets.Dialog1EditTextFragment.Companion.RESULT_KEY_PASSWORD_FAVOURITE
import com.cherry.doc.util.FileManager.isPdfEncrypted
import com.cherry.doc.util.FileManager.openDoc
import com.cherry.doc.util.FileManager.unlockPdfToCache
import com.cherry.doc.util.shareFile
import com.cherry.lib.doc.DocViewerActivity
import com.cherry.lib.doc.bean.DocSourceType
import com.cherry.lib.doc.bean.FileType
import com.cherry.lib.doc.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File

class FavouriteFragment : Fragment() {

    private var _binding: FragmentFavouriteBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: FavouriteAdapter
    private var pendingRenameItem: DocInfo? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFavouriteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecycler()
        initListener()
        observeData()
    }

    private fun setupRecycler() {
        adapter = FavouriteAdapter(
            onShare = { doc ->
                doc.path?.let { requireContext().shareFile(it) }
            },
            onRename = { doc ->
                pendingRenameItem = doc
                showDialogRename(doc.fileName?.substringBeforeLast(".") ?: "")
                doc.path?.let { FilesHelper.addFavouriteByPath(it) }
            },
            onUnFavourite = { doc ->
                lifecycleScope.launch {
                    doc.path?.let { AppDatabase.getInstance().docFavouriteDao().deleteByPath(it) }
                    FilesHelper.loadAllFavouriteDocs()
                }
            },
            onClickOpen = { item ->
                val path = item.path ?: return@FavouriteAdapter
                val file = File(path)

                if (!checkSupport(path)) return@FavouriteAdapter

                if (file.extension.lowercase() == "pdf" && isPdfEncrypted(file)) {
                    showInputPasswordDialog(file)
                } else {
                    openDoc(path, DocSourceType.PATH, activity = requireActivity())
                }
            }
        )

        binding.rcvFiles.layoutManager = LinearLayoutManager(requireContext())
        binding.rcvFiles.adapter = adapter
    }

    private fun initListener() {
        parentFragmentManager.setFragmentResultListener(
            RESULT_KEY_FAVOURITE,
            viewLifecycleOwner
        ) { _, bundle ->
            val newName =
                bundle.getString(Dialog1EditTextFragment.RESULT_TEXT)
                    ?: return@setFragmentResultListener

            pendingRenameItem?.let { FilesHelper.renameDoc(it, newName) }
            pendingRenameItem = null

        }
    }

    private fun showDialogRename(nameFile: String) {
        Dialog1EditTextFragment.newInstance(
            title = getString(R.string.text_rename),
            defaultText = nameFile,
            positiveText = getString(R.string.text_save),
            negativeText = getString(R.string.text_cancel),
            resultKey = RESULT_KEY_FAVOURITE
        ).show(parentFragmentManager, RESULT_KEY_FAVOURITE)
    }

    private fun observeData() {
        FilesHelper.allFilesFavourite.onEach { list ->
            Log.d("chinhhllpp", "observeData: $list")
            if (list.isEmpty()) {
                binding.imgNoData.isVisible = true
                binding.rcvFiles.isVisible = false
            } else {
                binding.imgNoData.isVisible = false
                binding.rcvFiles.isVisible = true
                adapter.submitList(list)
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)

    }

    fun checkSupport(path: String): Boolean {
        var fileType = FileUtils.getFileTypeForUrl(path)
        Log.e(javaClass.simpleName, "fileType = $fileType")
        if (fileType == FileType.NOT_SUPPORT) {
            return false
        }
        return true
    }

    private fun showInputPasswordDialog(file: File) {
        Dialog1EditTextFragment.newInstance(
            title = getString(R.string.text_enter_password),
            defaultText = "",
            positiveText = getString(R.string.text_okay),
            negativeText = getString(R.string.text_cancel),
            resultKey = RESULT_KEY_PASSWORD_FAVOURITE
        ).show(parentFragmentManager, RESULT_KEY_PASSWORD_FAVOURITE)

        parentFragmentManager.setFragmentResultListener(
            RESULT_KEY_PASSWORD_FAVOURITE,
            viewLifecycleOwner
        ) { _, bundle ->
            val password = bundle.getString(Dialog1EditTextFragment.RESULT_TEXT)
                ?: return@setFragmentResultListener
            unlockAndOpenPdf(file, password)
        }
    }

    private fun unlockAndOpenPdf(file: File, password: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val unlocked = unlockPdfToCache(requireContext(), file, password)

            launch(Dispatchers.Main) {
                if (unlocked != null && unlocked.exists()) {
                    openDoc(unlocked.absolutePath, DocSourceType.PATH, activity = requireActivity())
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.text_rename),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
