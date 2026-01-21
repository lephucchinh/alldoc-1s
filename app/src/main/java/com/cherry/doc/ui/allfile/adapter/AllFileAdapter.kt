package com.cherry.doc.ui.allfile.adapter


import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cherry.doc.data.DocInfo
import com.cherry.doc.databinding.ItemFileBinding
import com.cherry.lib.doc.R
import java.text.SimpleDateFormat
import java.util.*

class AllFileAdapter(
    private val items: MutableList<DocInfo> = mutableListOf(),
    private val listener: Listener? = null
) : RecyclerView.Adapter<AllFileAdapter.FileViewHolder>() {

    interface Listener {
        fun onItemClick(item: DocInfo, position: Int)
        fun onShare(item: DocInfo)
        fun onRename(item: DocInfo)
        fun onOption(item: DocInfo)
    }

    inner class FileViewHolder(
        private val binding: ItemFileBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DocInfo, position: Int) = with(binding) {

            // ===== ICON FILE =====
            val iconRes = item.getTypeIcon()
            if (iconRes != -1) {
                imgFile.setImageResource(iconRes)
                txtTools.text = item.getFileType()
            } else {
                imgFile.setImageResource(
                    R.drawable.all_doc_ic
                )
                txtTools.text = ""
            }

            // ===== FILE NAME =====
            txtNameFile.text = item.fileName.orEmpty()

            // ===== DATE / TIME =====
            val (date, time) = formatDateTime(item.lastModified)
            txtDate.text = date
            txtTime.text = time

            // ===== CLICK EVENTS =====
            root.setOnClickListener {
                listener?.onItemClick(item, position)
            }

            txtShare.setOnClickListener {
                listener?.onShare(item)
            }

            txtRename.setOnClickListener {
                listener?.onRename(item)
            }

            imgOption.setOnClickListener {
                listener?.onOption(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemFileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    // =====================================================
    // PUBLIC API
    // =====================================================

    fun submitList(newList: List<DocInfo>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    fun addItem(item: DocInfo) {
        items.add(0, item)
        notifyItemInserted(0)
    }

    fun getItem(position: Int): DocInfo = items[position]


    private fun formatDateTime(time: String?): Pair<String, String> {
        if (time.isNullOrBlank()) return "" to ""

        val millis = parseDateToMillis(time) ?: return "" to ""

        val date = Date(millis)
        val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

        return dateFmt.format(date) to timeFmt.format(date)
    }

    private fun parseDateToMillis(raw: String): Long? {
        val formats = listOf(
            "yyyy/MM/dd HH:mm",
            "yyyy-MM-dd HH:mm",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
        )

        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                sdf.isLenient = false
                val date = sdf.parse(raw)
                if (date != null) return date.time
            } catch (_: Exception) {
            }
        }
        return null
    }


}
