package com.cherry.doc.ui.allfile.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.cherry.doc.ui.allfile.pager.AllFilePager
import com.cherry.doc.ui.allfile.pager.ExcelTabPager
import com.cherry.doc.ui.allfile.pager.PdfTabPager
import com.cherry.doc.ui.allfile.pager.WordTabPager

class AllFilePagerAdapter(
    fragment: Fragment,
) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AllFilePager()
            1 -> PdfTabPager()
            2 -> WordTabPager()
            3 -> ExcelTabPager()
            else -> PdfTabPager()
        }
    }
}
