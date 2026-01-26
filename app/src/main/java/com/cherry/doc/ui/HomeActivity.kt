package com.cherry.doc.ui

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.DrawableCompat
import com.cherry.doc.R
import com.cherry.doc.data.SaveImagesResult
import com.cherry.doc.data.SavePdfResult
import com.cherry.doc.databinding.ActivityHomeBinding
import com.cherry.doc.repository.FilesHelper.loadAllFiles
import com.cherry.doc.ui.home.all.AllFileFragment
import com.cherry.doc.ui.widgets.BottomSheetCreateFile
import com.cherry.doc.ui.widgets.DialogFragmentCreatePdf
import com.cherry.doc.ui.widgets.OpenSdkScanner.registerDocumentScanner
import com.cherry.doc.ui.widgets.OpenSdkScanner.startScanDocument
import com.cherry.doc.util.FileManager.renameAndSavePdfToExternal
import com.cherry.doc.util.FileManager.saveImagesToExternal
import com.cherry.doc.util.hideSystemBars
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    private companion object {
        const val TAG_ALL = "ALL"
        const val TAG_RECENT = "RECENT"
        const val TAG_FAV = "FAVOURITE"
        const val TAG_TOOLS = "TOOLS"
    }

    private lateinit var currentTag: String
    private lateinit var scannerLauncher: ActivityResultLauncher<IntentSenderRequest>

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("CURRENT_TAG", currentTag)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hideSystemBars()

        initFragments()

        currentTag = savedInstanceState?.getString("CURRENT_TAG") ?: TAG_ALL
        if (savedInstanceState != null) {
            showFragment(currentTag)
        }
        updateBottomUI(
            when (currentTag) {
                TAG_RECENT -> HomeScreen.RECENT
                TAG_FAV -> HomeScreen.FAVOURITE
                TAG_TOOLS -> HomeScreen.TOOLS
                else -> HomeScreen.ALL
            }
        )

        initListener()
    }


    // =====================================================
    // INIT FRAGMENTS – ADD 1 LẦN DUY NHẤT
    // =====================================================
    private fun initFragments() {
        val fm = supportFragmentManager

        // Nếu đã restore (rotation / process death) thì lấy lại fragment cũ
        val all = fm.findFragmentByTag(TAG_ALL) ?: AllFileFragment()

        /*Test Tạm */
        /*TODO*/
        val recent = fm.findFragmentByTag(TAG_RECENT) ?: AllFileFragment()
        val fav = fm.findFragmentByTag(TAG_FAV) ?: AllFileFragment()
        val tools = fm.findFragmentByTag(TAG_TOOLS) ?: AllFileFragment()

        fm.beginTransaction()
            .apply {
                if (!all.isAdded) add(R.id.container, all, TAG_ALL)
                if (!recent.isAdded) add(R.id.container, recent, TAG_RECENT).hide(recent)
                if (!fav.isAdded) add(R.id.container, fav, TAG_FAV).hide(fav)
                if (!tools.isAdded) add(R.id.container, tools, TAG_TOOLS).hide(tools)
            }
            .commit()

    }

    private fun initListener() = with(binding) {
        scannerLauncher = registerDocumentScanner(
            activity = this@HomeActivity,
            onImagesResult = { images ->
                when (
                    val result = saveImagesToExternal(
                        context = this@HomeActivity,
                        images = images,
                        baseName = "scan_${System.currentTimeMillis()}",
                        subFolder = "ScannedImages"
                    )
                ) {
                    is SaveImagesResult.Success -> {
                    }

                    is SaveImagesResult.Error -> {
                    }
                }
            },
            onPdfResult = { pdf ->
                pdf?.let {
                    showDialogCreatePdf(pdf)
                }
            }
        )
        btnAllFile.setOnClickListener {
            showFragment(TAG_ALL)
            updateBottomUI(HomeScreen.ALL)
        }

        btnRecent.setOnClickListener {
            showFragment(TAG_RECENT)
            updateBottomUI(HomeScreen.RECENT)
        }

        btnFavorite.setOnClickListener {
            showFragment(TAG_FAV)
            updateBottomUI(HomeScreen.FAVOURITE)
        }

        btnTools.setOnClickListener {
            showFragment(TAG_TOOLS)
            updateBottomUI(HomeScreen.TOOLS)
        }

        btnScanner.setOnClickListener {

            showBottomSheetCreateFile()

        }
    }

    private fun showFragment(tag: String) {
        val fm = supportFragmentManager

        val current = fm.findFragmentByTag(currentTag)
        val target = fm.findFragmentByTag(tag) ?: return

        if (current != null && current != target) {
            fm.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                .hide(current)
                .show(target)
                .commit()
        } else {
            fm.beginTransaction()
                .show(target)
                .commit()
        }

        currentTag = tag
    }

    private fun showDialogCreatePdf(pdf: File) {
        DialogFragmentCreatePdf { newName ->

            when (
                val result = renameAndSavePdfToExternal(
                    context = this,
                    sourceFile = pdf,
                    newName = newName,
                    subFolder = "ScannedPDF"
                )
            ) {
                is SavePdfResult.Success -> {
                    // ✅ lưu thành công
                    CoroutineScope(Dispatchers.Main).launch {
                        loadAllFiles()
                    }
                }

                is SavePdfResult.Error -> {
                    Toast.makeText(this, result.reason, Toast.LENGTH_SHORT).show()
                }
            }

        }.show(supportFragmentManager, "CreatePdf")


    }


    private fun showBottomSheetCreateFile() {
        BottomSheetCreateFile(object : BottomSheetCreateFile.Listener {
            override fun onImageToPdf() {
                // chọn ảnh -> pdf
            }

            override fun onScanPdf() {
                startScanDocument(
                    activity = this@HomeActivity,
                    launcher = scannerLauncher
                )
            }

            override fun onMergePdf() {
                // merge pdf
            }

            override fun onCreatePdf() {
                // tạo pdf trống
            }
        }).show(supportFragmentManager, "BottomSheetCreateFile")

    }


    private fun updateBottomUI(screen: HomeScreen) {
        resetBottomUI()

        when (screen) {
            HomeScreen.ALL -> select(binding.btnAllFile)
            HomeScreen.RECENT -> select(binding.btnRecent)
            HomeScreen.FAVOURITE -> select(binding.btnFavorite)
            HomeScreen.TOOLS -> select(binding.btnTools)
        }
    }

    private fun resetBottomUI() {
        val normalColor = getColor(R.color.color_80000000)

        listOf(
            binding.btnAllFile,
            binding.btnRecent,
            binding.btnFavorite,
            binding.btnTools
        ).forEach {
            it.setTextColor(normalColor)
            tintTopDrawable(it, normalColor)
        }
    }

    private fun select(tv: TextView) {
        val activeColor = getColor(R.color.color_F8241C)
        tv.setTextColor(activeColor)
        tintTopDrawable(tv, activeColor)
    }

    private fun tintTopDrawable(tv: TextView, color: Int) {
        tv.compoundDrawables[1]?.let { drawableTop ->
            DrawableCompat.setTint(
                DrawableCompat.wrap(drawableTop),
                color
            )
        }
    }
}

enum class HomeScreen {
    ALL,
    RECENT,
    FAVOURITE,
    TOOLS
}
