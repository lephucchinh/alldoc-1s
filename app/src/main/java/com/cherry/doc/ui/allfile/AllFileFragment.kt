package com.cherry.doc.ui.home.all

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import com.cherry.doc.R
import com.cherry.doc.databinding.FragmentHomeAllFilesBinding
import com.cherry.doc.ui.allfile.adapter.AllFilePagerAdapter
import com.google.android.material.tabs.TabLayoutMediator

class AllFileFragment : Fragment() {

    private var _binding: FragmentHomeAllFilesBinding? = null
    private val binding get() = _binding!!
    lateinit var allFilePagerAdapter: AllFilePagerAdapter
    private var currentBgColor: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeAllFilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
    }

    private fun setupViewPager() {
        allFilePagerAdapter = AllFilePagerAdapter(this)
        binding.viewPager.adapter = allFilePagerAdapter
        binding.viewPager.isUserInputEnabled = false

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.text_all)
                1 -> "PDF"
                2 -> "Word"
                3 -> "Excel"
                else -> "PPT"
            }
        }.attach()

        binding.tabLayout.addOnTabSelectedListener(object :
            com.google.android.material.tabs.TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                updateBgTitleByTab(tab.position)
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}

            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
        })

        updateBgTitleByTab(0)
    }

    private fun updateTitleByTab(position: Int) {
        val title = getString(R.string.title_home_screen) // "All PDF Reader"

        if (position == 0) {
            val spannable = android.text.SpannableString(title)

            val allPdfEnd = title.indexOf("Reader") // vị trí bắt đầu "Reader"

            // All PDF → BLACK
            spannable.setSpan(
                android.text.style.ForegroundColorSpan(
                    requireContext().getColor(R.color.black)
                ),
                0,
                allPdfEnd,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Reader → RED
            spannable.setSpan(
                android.text.style.ForegroundColorSpan(
                    requireContext().getColor(R.color.red_pdf)
                ),
                allPdfEnd,
                title.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            binding.txtTitleHomeScreen.text = spannable
        } else {
            // TAB KHÁC → trắng hết
            binding.txtTitleHomeScreen.text = title
            binding.txtTitleHomeScreen.setTextColor(
                requireContext().getColor(R.color.white)
            )
        }
    }


    private fun updateUiForTab(position: Int) {
        val isAllTab = position == 0
        updateTitleByTab(position)


        // Tab text color
        binding.tabLayout.setTabTextColors(
            requireContext().getColor(
                if (isAllTab) R.color.black_50 else R.color.white_70
            ),
            requireContext().getColor(
                if (isAllTab) R.color.black else R.color.white
            )
        )

        binding.btnSetting.imageTintList =
            android.content.res.ColorStateList.valueOf(
                requireContext().getColor(
                    if (isAllTab) R.color.black else R.color.white
                )
            )



        // Indicator color
        binding.tabLayout.setSelectedTabIndicatorColor(
            requireContext().getColor(
                if (isAllTab) R.color.red_pdf else R.color.white
            )
        )
    }


    private fun updateBgTitleByTab(position: Int) {
        val newColor = requireContext().getColor(
            when (position) {
                0 -> android.R.color.white   // ALL = trắng
                1 -> R.color.red_pdf
                2 -> R.color.blue_word
                3 -> R.color.green_excel
                else -> R.color.orange_ppt
            }
        )

        if (currentBgColor == 0) {
            binding.bgTitle.setBackgroundColor(newColor)
        } else {
            animateBgColor(currentBgColor, newColor)
        }

        currentBgColor = newColor

        // 👇 cập nhật text / tab color
        updateUiForTab(position)
    }


    private fun animateBgColor(@ColorInt from: Int, @ColorInt to: Int) {
        val animator = android.animation.ValueAnimator.ofArgb(from, to)
        animator.duration = 250
        animator.addUpdateListener {
            binding.bgTitle.setBackgroundColor(it.animatedValue as Int)
        }
        animator.start()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}
