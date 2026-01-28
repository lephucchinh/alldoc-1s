package com.cherry.doc.ui.mergepdf.adapter

import com.cherry.doc.data.model.DocInfo

data class MergePdfItem(
    val doc: DocInfo,
    var isSelected: Boolean = true
)

enum class MergeMode {
    SELECT,
    MERGE
}
