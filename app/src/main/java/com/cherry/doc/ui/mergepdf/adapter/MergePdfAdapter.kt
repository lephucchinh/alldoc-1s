package com.cherry.doc.ui.mergepdf.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.cherry.doc.data.DocInfo
import com.cherry.doc.databinding.ItemMergePdfBinding
import com.cherry.doc.util.setSingleClickListener
import com.cherry.lib.doc.R

class MergePdfAdapter(
    private val onSelectionChanged: (List<DocInfo>) -> Unit,
) : RecyclerView.Adapter<MergePdfAdapter.FileViewHolder>() {

    private val items = mutableListOf<MergePdfItem>()
    private var mode: MergeMode = MergeMode.SELECT


    fun showAll(docs: List<DocInfo>) {
        mode = MergeMode.SELECT
        items.clear()
        items.addAll(
            docs.map {
                MergePdfItem(
                    doc = it,
                    isSelected = false
                )
            }
        )
        notifyDataSetChanged()
        notifySelection()
    }

    fun showMergeList(selected: List<DocInfo>) {
        mode = MergeMode.MERGE
        items.clear()
        items.addAll(selected.map { MergePdfItem(it, true) })
        notifyDataSetChanged()
    }

    fun getSelected(): List<DocInfo> =
        items.filter { it.isSelected }.map { it.doc }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemMergePdfBinding.inflate(
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


    inner class FileViewHolder(
        private val binding: ItemMergePdfBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MergePdfItem, position: Int) {
            val doc = item.doc

            binding.txtTitle.text = doc.fileName
            binding.txtType.text = "PDF"
            binding.imgFile.setImageResource(R.drawable.pdf_ic)

            binding.btnMove.isVisible = mode == MergeMode.MERGE

            binding.imgSelect.setImageResource(
                when (mode) {
                    MergeMode.SELECT ->
                        if (item.isSelected)
                            com.cherry.doc.R.drawable.ic_selected
                        else
                            com.cherry.doc.R.drawable.ic_unselect

                    MergeMode.MERGE ->
                        com.cherry.doc.R.drawable.ic_remove
                }
            )

            binding.root.setOnClickListener {
                if (mode == MergeMode.SELECT) {
                    item.isSelected = !item.isSelected
                    notifyItemChanged(position)
                    notifySelection()
                }
            }

            binding.imgSelect.setSingleClickListener {
                if (mode == MergeMode.MERGE) {

                    val pos = position
                    if (pos == RecyclerView.NO_POSITION) return@setSingleClickListener
                    if (items.size <= 2) {
                        Toast.makeText(
                            itemView.context,
                            "Select at least 2 PDFs",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setSingleClickListener
                    }
                    items.removeAt(pos)
                    notifyItemRemoved(pos)
                    notifyItemRangeChanged(pos, items.size - pos)
                    notifySelection()
                }
                else if (mode == MergeMode.SELECT) {
                    item.isSelected = !item.isSelected
                    notifyItemChanged(position)
                    notifySelection()
                }
            }
        }
    }

    private fun notifySelection() {
        onSelectionChanged(getSelected())
    }
}
