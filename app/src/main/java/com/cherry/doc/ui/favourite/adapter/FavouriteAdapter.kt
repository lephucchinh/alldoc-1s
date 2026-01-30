package com.cherry.doc.ui.favourite.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cherry.doc.data.model.DocInfo
import com.cherry.doc.databinding.ItemFileFavouriteBinding
import com.cherry.doc.util.formatDateTime
import com.cherry.doc.util.setSingleClickListener

class FavouriteAdapter(
    private val onShare: (DocInfo) -> Unit,
    private val onRename: (DocInfo) -> Unit,
    private val onUnFavourite: (DocInfo) -> Unit,
    private val onClickOpen: (DocInfo) -> Unit
) : ListAdapter<DocInfo, FavouriteAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFileFavouriteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemFileFavouriteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DocInfo) = with(binding) {

            txtNameFile.text = item.fileName
            txtTools.text = item.getFileType()
            val (date, time) = item.lastModified.formatDateTime()
            txtDate.text = date
            txtTime.text = time
            imgFile.setImageResource(item.getTypeIcon())
            txtShare.setSingleClickListener { onShare(item) }
            txtRename.setSingleClickListener { onRename(item) }
            root.setSingleClickListener {
                onClickOpen(item)
            }
            imgFavourite.setSingleClickListener {
                onUnFavourite(item)
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<DocInfo>() {
        override fun areItemsTheSame(old: DocInfo, new: DocInfo): Boolean =
            old.path == new.path

        override fun areContentsTheSame(old: DocInfo, new: DocInfo): Boolean =
            old == new
    }
}
