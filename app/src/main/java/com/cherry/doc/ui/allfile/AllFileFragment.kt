package com.cherry.doc.ui.home.all

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cherry.doc.R
import com.cherry.doc.databinding.FragmentHomeAllFilesBinding
import com.cherry.doc.ui.allfile.adapter.AllFilePagerAdapter
import com.google.android.material.tabs.TabLayoutMediator

class AllFileFragment : Fragment() {

    private var _binding: FragmentHomeAllFilesBinding? = null
    private val binding get() = _binding!!
    lateinit var allFilePagerAdapter: AllFilePagerAdapter

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
                0 -> this.getString(R.string.text_all)
                1 -> "PDF"
                2 -> "Word"
                3 -> "Excel"
                else -> "PPT"
            }
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
