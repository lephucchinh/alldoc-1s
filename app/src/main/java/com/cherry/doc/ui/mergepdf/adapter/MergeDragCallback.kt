package com.cherry.doc.ui.mergepdf.adapter

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class MergeDragCallback(
    private val adapter: MergePdfAdapter
) : ItemTouchHelper.Callback() {

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        if (!adapter.isMergeMode()) return makeMovementFlags(0, 0)
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {

        val from = viewHolder.adapterPosition
        val to = target.adapterPosition

        if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) {
            return false
        }

        adapter.onItemMove(from, to)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

    override fun isLongPressDragEnabled(): Boolean {
        return adapter.isMergeMode()
    }
}
