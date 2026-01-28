package com.cherry.doc.ui.allfile.adapter


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cherry.doc.data.model.DocInfo
import com.cherry.doc.databinding.ItemFileBinding
import com.cherry.doc.util.formatDateTime
import com.cherry.doc.util.setSingleClickListener
import com.cherry.lib.doc.R

class AllFileAdapter(
    private val listener: Listener? = null,
) : ListAdapter<DocInfo, AllFileAdapter.FileViewHolder>(DocDiffCallback) {

    interface Listener {
        fun onItemClick(item: DocInfo, position: Int)
        fun onShare(item: DocInfo)
        fun onRename(item: DocInfo, position: Int)
        fun onOption(item: DocInfo)
    }

    inner class FileViewHolder(
        private val binding: ItemFileBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DocInfo, position: Int) = with(binding) {

            // ===== ICON FILE =====
            val iconRes = item.getTypeIcon()
            if (iconRes != -1) {
                imgFile.setImageResource(iconRes)
                txtTools.text = item.getNormalizedFileType()
            } else {
                imgFile.setImageResource(R.drawable.all_doc_ic)
                txtTools.text = ""
            }

            // ===== FILE NAME =====
            txtNameFile.text = item.fileName.orEmpty()

            // ===== DATE / TIME =====
            val (date, time) = item.lastModified.formatDateTime()
            txtDate.text = date
            txtTime.text = time


            // ===== CLICK EVENTS =====
            root.setSingleClickListener {
                listener?.onItemClick(item, position)
            }

            txtShare.setSingleClickListener { listener?.onShare(item) }
            txtRename.setSingleClickListener { listener?.onRename(item, position) }
            imgOption.setSingleClickListener { listener?.onOption(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        return FileViewHolder(
            ItemFileBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }
}

