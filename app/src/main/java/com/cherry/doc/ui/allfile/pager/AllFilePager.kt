package com.cherry.doc.ui.allfile.pager

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.cherry.doc.R
import com.cherry.doc.data.DocInfo
import com.cherry.doc.data.PdfCheckResult
import com.cherry.doc.databinding.PageAllFileBinding
import com.cherry.doc.repository.FilesHelper
import com.cherry.doc.ui.allfile.AllFileViewModel
import com.cherry.doc.ui.allfile.adapter.AllFileAdapter
import com.cherry.doc.ui.widgets.Dialog1EditTextFragment
import com.cherry.doc.ui.widgets.Dialog1EditTextFragment.Companion.RESULT_KEY_ALL_APP
import com.cherry.doc.ui.widgets.Dialog1EditTextFragment.Companion.RESULT_KEY_PASSWORD_ALL_FILE
import com.cherry.doc.ui.widgets.Dialog1EditTextFragment.Companion.RESULT_KEY_UNLOCK_ALL_FILE
import com.cherry.doc.ui.widgets.DialogFragmentDelete
import com.cherry.doc.ui.widgets.DialogSetPasswordFragment
import com.cherry.doc.ui.widgets.OnDeleteConfirmListener
import com.cherry.doc.ui.widgets.OptionPdfBottomSheet
import com.cherry.doc.util.Const.REQUEST_CODE_STORAGE_PERMISSION
import com.cherry.doc.util.Const.REQUEST_CODE_STORAGE_PERMISSION11
import com.cherry.doc.util.FileManager.checkPdfByPath
import com.cherry.doc.util.FileManager.isPdfEncrypted
import com.cherry.doc.util.FileManager.removePdfPasswordByPath
import com.cherry.doc.util.FileManager.unlockPdfToCache
import com.cherry.doc.util.formatDateTime
import com.cherry.doc.util.lockPdf
import com.cherry.doc.util.shareFile
import com.cherry.lib.doc.DocViewerActivity
import com.cherry.lib.doc.bean.DocSourceType
import com.cherry.lib.doc.bean.FileType
import com.cherry.lib.doc.util.FileUtils
import com.cherry.permissions.lib.EasyPermissions
import com.cherry.permissions.lib.annotations.AfterPermissionGranted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File

class AllFilePager : Fragment() {

    private var _binding: PageAllFileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AllFileViewModel by activityViewModels()

    private lateinit var adapter: AllFileAdapter

    private var pendingRenameItem: DocInfo? = null


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
        initListener()
        setupRecyclerView()
        loadData()
    }

    private fun setupRecyclerView() {
        adapter = AllFileAdapter(listener = object : AllFileAdapter.Listener {
            override fun onItemClick(item: DocInfo, position: Int) {
                val path = item.path ?: return
                val file = File(path)

                if (!checkSupport(path)) return

                if (file.extension.lowercase() == "pdf" && isPdfEncrypted(file)) {
                    showInputPasswordDialog(file)
                } else {
                    openDoc(path, DocSourceType.PATH)
                }
            }


            override fun onShare(item: DocInfo) {
                item.path?.let { requireContext().shareFile(it) }
            }

            override fun onRename(item: DocInfo, position: Int) {
                pendingRenameItem = item
                showDialogRename(item.fileName?.substringBeforeLast(".") ?: "")
            }

            override fun onOption(item: DocInfo) {
                showBottomSheetOption(item)
            }
        })

        binding.rcvFiles.adapter = adapter
        binding.rcvFiles.setHasFixedSize(true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION11) {
            if (hasRwPermission()) {
                requestStoragePermission()
            }
        }
    }

    private fun showInputPasswordDialog(file: File) {
        Dialog1EditTextFragment.newInstance(
            title = getString(R.string.text_enter_password),
            defaultText = "",
            positiveText = getString(R.string.text_okay),
            negativeText = getString(R.string.text_cancel),
            resultKey = RESULT_KEY_PASSWORD_ALL_FILE
        ).show(parentFragmentManager, RESULT_KEY_PASSWORD_ALL_FILE)

        parentFragmentManager.setFragmentResultListener(
            RESULT_KEY_PASSWORD_ALL_FILE,
            viewLifecycleOwner
        ) { _, bundle ->
            val password = bundle.getString(Dialog1EditTextFragment.RESULT_TEXT) ?: return@setFragmentResultListener
            unlockAndOpenPdf(file, password)
        }
    }

    private fun showUnLockPasswordDialog(doc: DocInfo) {
        val file = File(doc.path ?: return)

        Dialog1EditTextFragment.newInstance(
            title = getString(R.string.text_enter_password),
            defaultText = "",
            positiveText = getString(R.string.text_okay),
            negativeText = getString(R.string.text_cancel),
            resultKey = RESULT_KEY_UNLOCK_ALL_FILE
        ).show(parentFragmentManager, RESULT_KEY_UNLOCK_ALL_FILE)

        parentFragmentManager.setFragmentResultListener(
            RESULT_KEY_UNLOCK_ALL_FILE,
            viewLifecycleOwner
        ) { _, bundle ->
            val password = bundle.getString(Dialog1EditTextFragment.RESULT_TEXT) ?: return@setFragmentResultListener
            removePdfPasswordByPath(requireContext(),file.path, password)
        }
    }

    private fun unlockAndOpenPdf(file: File, password: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val unlocked = unlockPdfToCache(requireContext(), file, password)

            launch(Dispatchers.Main) {
                if (unlocked != null && unlocked.exists()) {
                    openDoc(unlocked.absolutePath, DocSourceType.PATH)
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


    private fun showDialogRename(nameFile: String) {
        Dialog1EditTextFragment.newInstance(
            title = getString(R.string.text_rename),
            defaultText = nameFile,
            positiveText = getString(R.string.text_save),
            negativeText = getString(R.string.text_cancel),
            resultKey = RESULT_KEY_ALL_APP
        ).show(parentFragmentManager, RESULT_KEY_ALL_APP)
    }

    private fun showBottomSheetOption(item: DocInfo) {
        OptionPdfBottomSheet(
            docInfo = item,
            listener = object : OptionPdfBottomSheet.Listener {

                override fun onAddFavourite(doc: DocInfo) {
                    // TODO: save favourite
                }

                override fun onMergePdf(doc: DocInfo) {
                    // TODO: open merge screen
                }

                override fun onSplitPdf(doc: DocInfo) {
                    // TODO: open split screen
                }

                override fun onLockPdf(doc: DocInfo) {
                    if(checkPdfHavePassword(doc.path ?: "")) {
                        showUnLockPasswordDialog(doc)
                    } else {
                        showDialogLockPdf(doc)
                    }
                }

                override fun onDelete(doc: DocInfo) {
                    showDeleteDialog(doc)
                }

                override fun onShare(doc: DocInfo) {
                    doc.path?.let { requireContext().shareFile(it) }
                }
            }
        ).show(parentFragmentManager, "OptionPdfBottomSheet")
    }

    private fun checkPdfHavePassword(filePath: String): Boolean {
        return when (checkPdfByPath(filePath)) {
            PdfCheckResult.OK -> false

            PdfCheckResult.PASSWORD_PROTECTED -> true

            PdfCheckResult.INVALID_PDF -> false
        }

    }

    private fun showDeleteDialog(doc: DocInfo) {
        val (date, time) = doc.lastModified.formatDateTime()
        val fileImage = doc.getTypeIcon()
        val fileType = doc.getFileType() ?: ""
        doc.fileName?.let {
            DialogFragmentDelete(
                fileName = it,
                fileType = fileType,
                fileImage = fileImage,
                date = date,
                time = time,
                listener = object : OnDeleteConfirmListener {
                    override fun onDelete() {
                        FilesHelper.deleteDoc(doc)
                    }

                    override fun onCancel() {
                        // optional
                    }
                }
            )

        }?.show(parentFragmentManager, "delete_dialog")

    }


    private fun showDialogLockPdf(doc: DocInfo) {
        val file = File(doc.path ?: return)

        DialogSetPasswordFragment { password ->
            lifecycleScope.launch(Dispatchers.IO) {
                val success = file.lockPdf(password = password)

                launch(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.lock_success),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.lock_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }.show(parentFragmentManager, "SET_PASSWORD")
    }

    private fun initListener() {
        parentFragmentManager.setFragmentResultListener(
            Dialog1EditTextFragment.RESULT_KEY_ALL_APP,
            viewLifecycleOwner
        ) { _, bundle ->
            val newName =
                bundle.getString(Dialog1EditTextFragment.RESULT_TEXT)
                    ?: return@setFragmentResultListener

            pendingRenameItem?.let { FilesHelper.renameDoc(it, newName) }
            pendingRenameItem = null

        }
    }


    fun openDoc(path: String, docSourceType: Int, type: Int? = null) {
        DocViewerActivity.Companion.launchDocViewer(
            requireActivity(),
            docSourceType,
            path,
            type
        )
    }

    fun checkSupport(path: String): Boolean {
        var fileType = FileUtils.getFileTypeForUrl(path)
        Log.e(javaClass.simpleName, "fileType = $fileType")
        if (fileType == FileType.NOT_SUPPORT) {
            return false
        }
        return true
    }


    private fun loadData() {
        FilesHelper.allFiles.onEach { groups ->

            val allFiles = groups?.flatMap { it.docList.orEmpty() }

            adapter.submitList(allFiles?.filter { isSupportedDoc(it) })
        }.launchIn(viewLifecycleOwner.lifecycleScope)
        requestStoragePermission()

    }

    fun isSupportedDoc(docInfo: DocInfo): Boolean {
        val name = docInfo.fileName?.lowercase() ?: return false
        return name.endsWith(".pdf")
                || name.endsWith(".doc")
                || name.endsWith(".docx")
                || name.endsWith(".xls")
                || name.endsWith(".xlsx")
                || name.endsWith(".ppt")
                || name.endsWith(".pptx")
                || name.endsWith(".txt")
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    @AfterPermissionGranted(REQUEST_CODE_STORAGE_PERMISSION)
    private fun requestStoragePermission() {
        if (hasRwPermission()) {
            // Have permission, do things!
            CoroutineScope(Dispatchers.Main).launch {
                FilesHelper.loadAllFiles()
            }

        } else {
            // Ask for one permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                get11Permission()
                return
            }
            EasyPermissions.requestPermissions(
                this,
                "This app needs access to your storage to load local doc",
                REQUEST_CODE_STORAGE_PERMISSION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    fun get11Permission() {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.addCategory("android.intent.category.DEFAULT")
            intent.data =
                Uri.parse(java.lang.String.format("package:%s", requireActivity().packageName))
            startActivityForResult(intent,REQUEST_CODE_STORAGE_PERMISSION11)
        } catch (e: Exception) {
            val intent = Intent()
            intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
            startActivityForResult(intent, REQUEST_CODE_STORAGE_PERMISSION11)
        }
    }

    private fun hasRwPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val isExternalStorageManager = Environment.isExternalStorageManager()
            return isExternalStorageManager
        }
        val read = EasyPermissions.hasPermissions(
            requireActivity(),
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        val write = EasyPermissions.hasPermissions(
            requireActivity(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        return read && write
    }
}
