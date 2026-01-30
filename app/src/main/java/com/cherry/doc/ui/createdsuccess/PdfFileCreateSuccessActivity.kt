package com.cherry.doc.ui.createdsuccess

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.cherry.doc.R
import com.cherry.doc.data.model.DocInfo
import com.cherry.doc.databinding.ActivityCreateFileSuccessBinding
import com.cherry.doc.repository.FilesHelper
import com.cherry.doc.ui.widgets.Dialog1EditTextFragment
import com.cherry.doc.ui.widgets.Dialog1EditTextFragment.Companion.RESULT_KEY_PASSWORD_ALL_FILE
import com.cherry.doc.util.FileManager.isPdfEncrypted
import com.cherry.doc.util.FileManager.openDoc
import com.cherry.doc.util.FileManager.unlockPdfToCache
import com.cherry.doc.util.formatDateTime
import com.cherry.doc.util.hideSystemBars
import com.cherry.lib.doc.DocViewerActivity
import com.cherry.lib.doc.bean.DocSourceType
import com.cherry.lib.doc.bean.FileType
import com.cherry.lib.doc.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class PdfFileCreateSuccessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateFileSuccessBinding
    private lateinit var docInfo: DocInfo

    companion object {
        private const val EXTRA_DOC_INFO = "extra_doc_info"

        fun start(context: Context, path: String) {
            val intent = Intent(context, PdfFileCreateSuccessActivity::class.java)
            intent.putExtra(EXTRA_DOC_INFO, path) // Parcelable
            context.startActivity(intent)
        }
    }

    // --------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCreateFileSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hideSystemBars()
        val path = getDocInfoFromIntent()

        docInfo = FilesHelper.getDocByPath(path) ?: run {
            finish()
            return
        }


        setupFragmentResultListener()
        setupView()
        registerListener()
    }

    // --------------------------------------------------------------------
    // Init
    // --------------------------------------------------------------------

    private fun getDocInfoFromIntent(): String {
        return intent.getStringExtra(EXTRA_DOC_INFO) ?: ""

    }

    private fun setupView() = with(binding) {
        txtNameFile.text = docInfo.fileName

        val (date, time) = (docInfo.lastModified ?: "").formatDateTime()
        txtDate.text = date
        txtTime.text = time

        txtTools.text = docInfo.getNormalizedFileType() ?: "PDF"
        imgFile.setImageResource(com.cherry.lib.doc.R.drawable.pdf_ic)
    }

    private fun registerListener() = with(binding) {
        btnBack.setOnClickListener { finish() }
        btnOpen.setOnClickListener { openPdf() }
        btnShare.setOnClickListener { sharePdf() }
        btnMail.setOnClickListener { sendMail() }
        btnFavorite.setOnClickListener { addToFavorite() }
    }

    // --------------------------------------------------------------------
    // Open PDF
    // --------------------------------------------------------------------

    private fun openPdf() {
        val path = requirePdfPath()
        val file = File(path)

        if (!checkSupport(path)) return

        if (file.extension.equals("pdf", true) && isPdfEncrypted(file)) {
            showInputPasswordDialog()
        } else {
            openDoc(path, DocSourceType.PATH, activity = this)
        }
    }

    private fun showInputPasswordDialog() {
        Dialog1EditTextFragment.newInstance(
            title = getString(R.string.text_enter_password),
            defaultText = "",
            positiveText = getString(R.string.text_okay),
            negativeText = getString(R.string.text_cancel),
            resultKey = RESULT_KEY_PASSWORD_ALL_FILE
        ).show(supportFragmentManager, RESULT_KEY_PASSWORD_ALL_FILE)
    }

    private fun setupFragmentResultListener() {
        supportFragmentManager.setFragmentResultListener(
            RESULT_KEY_PASSWORD_ALL_FILE,
            this
        ) { _, bundle ->
            val password =
                bundle.getString(Dialog1EditTextFragment.RESULT_TEXT)
                    ?: return@setFragmentResultListener
            unlockAndOpenPdf(password)
        }
    }

    private fun unlockAndOpenPdf(password: String) {
        val file = File(requirePdfPath())

        lifecycleScope.launch(Dispatchers.IO) {
            val unlocked = unlockPdfToCache(
                this@PdfFileCreateSuccessActivity,
                file,
                password
            )

            launch(Dispatchers.Main) {
                if (unlocked != null && unlocked.exists()) {
                    openDoc(
                        unlocked.absolutePath,
                        DocSourceType.PATH,
                        activity = this@PdfFileCreateSuccessActivity
                    )
                } else {
                    Toast.makeText(
                        this@PdfFileCreateSuccessActivity,
                        getString(R.string.text_rename),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun checkSupport(path: String): Boolean {
        val fileType = FileUtils.getFileTypeForUrl(path)
        Log.d(javaClass.simpleName, "fileType = $fileType")
        return fileType != FileType.NOT_SUPPORT
    }

    private fun requirePdfPath(): String {
        return docInfo.path ?: run {
            finish()
            throw IllegalStateException("DocInfo.path is null")
        }
    }

    // --------------------------------------------------------------------
    // Share / Mail / Favorite
    // --------------------------------------------------------------------

    private fun sharePdf() {
        val uri = getPdfUri()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share PDF"))
    }

    private fun sendMail() {
        val uri = getPdfUri()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, docInfo.fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Send mail"))
    }

    private fun addToFavorite() {
        // TODO: save to Room
    }

    private fun getPdfUri(): Uri {
        return FileProvider.getUriForFile(
            this,
            "$packageName.provider",
            File(requirePdfPath())
        )
    }
}
