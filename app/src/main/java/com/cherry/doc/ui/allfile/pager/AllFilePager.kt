package com.cherry.doc.ui.allfile.pager

import android.Manifest
import android.app.Activity.RESULT_OK
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.cherry.doc.R
import com.cherry.doc.data.DocInfo
import com.cherry.doc.databinding.PageAllFileBinding
import com.cherry.doc.ui.allfile.AllFileViewModel
import com.cherry.doc.ui.allfile.adapter.AllFileAdapter
import com.cherry.doc.ui.main.MainActivity
import com.cherry.doc.ui.main.MainActivity.Companion.TAG
import com.cherry.doc.ui.widgets.Dialog1EditTextFragment
import com.cherry.doc.ui.widgets.Dialog1EditTextFragment.Companion.RESULT_KEY_ALL_APP
import com.cherry.doc.ui.widgets.OptionPdfBottomSheet
import com.cherry.doc.util.DocUtil
import com.cherry.doc.util.FileManager.deleteFileSmart
import com.cherry.doc.util.shareFile
import com.cherry.lib.doc.DocViewerActivity
import com.cherry.lib.doc.bean.DocSourceType
import com.cherry.lib.doc.bean.FileType
import com.cherry.lib.doc.util.FileUtils
import com.cherry.permissions.lib.EasyPermissions
import com.cherry.permissions.lib.annotations.AfterPermissionGranted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
                var path = item.path ?: ""
                if (checkSupport(path)) {
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

        if (requestCode == MainActivity.Companion.REQUEST_CODE_STORAGE_PERMISSION11) {
            if (hasRwPermission()) {
                requestStoragePermission()
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
                    // TODO: lock pdf
                }

                override fun onDelete(doc: DocInfo) {
                    doc.path?.let { deleteFileSmart(requireContext(),it) }
                }

                override fun onShare(doc: DocInfo) {
                    doc.path?.let { requireContext().shareFile(it) }
                }
            }
        ).show(parentFragmentManager, "OptionPdfBottomSheet")
    }

    private fun initListener() {
        parentFragmentManager.setFragmentResultListener(
            Dialog1EditTextFragment.RESULT_KEY_ALL_APP,
            viewLifecycleOwner
        ) { _, bundle ->
            val newName =
                bundle.getString(Dialog1EditTextFragment.RESULT_TEXT)
                    ?: return@setFragmentResultListener

            pendingRenameItem?.let { viewModel.renameDoc(it, newName) }
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
        viewModel.allFiles.observe(viewLifecycleOwner) { groups ->

            val allFiles = groups?.flatMap { it.docList.orEmpty() }

            adapter.submitList(allFiles?.filter { isSupportedDoc(it) })
        }
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


    @AfterPermissionGranted(MainActivity.Companion.REQUEST_CODE_STORAGE_PERMISSION)
    private fun requestStoragePermission() {
        if (hasRwPermission()) {
            // Have permission, do things!
            CoroutineScope(Dispatchers.Main).launch {
                viewModel.loadAllFiles()
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
                MainActivity.Companion.REQUEST_CODE_STORAGE_PERMISSION,
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
            startActivityForResult(intent, MainActivity.Companion.REQUEST_CODE_STORAGE_PERMISSION11)
        } catch (e: Exception) {
            val intent = Intent()
            intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
            startActivityForResult(intent, MainActivity.Companion.REQUEST_CODE_STORAGE_PERMISSION11)
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
