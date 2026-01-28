package com.cherry.doc.ui.allfile.adapter

import androidx.recyclerview.widget.DiffUtil
import com.cherry.doc.data.model.DocInfo

object DocDiffCallback : DiffUtil.ItemCallback<DocInfo>() {

    override fun areItemsTheSame(oldItem: DocInfo, newItem: DocInfo): Boolean {
        return oldItem.path == newItem.path
    }

    override fun areContentsTheSame(oldItem: DocInfo, newItem: DocInfo): Boolean {
        return oldItem.fileName == newItem.fileName &&
                oldItem.lastModified == newItem.lastModified
    }
}
