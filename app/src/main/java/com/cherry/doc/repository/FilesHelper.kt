package com.cherry.doc.repository

import com.cherry.doc.App
import com.cherry.doc.data.DocGroupInfo
import com.cherry.doc.data.DocInfo
import com.cherry.doc.util.DocUtil
import com.cherry.doc.util.FileManager.deleteFileSmart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

object FilesHelper {

    private val _allFiles =
        MutableStateFlow<List<DocGroupInfo>>(emptyList())

    val allFiles: StateFlow<List<DocGroupInfo>> = _allFiles.asStateFlow()

    suspend fun loadAllFiles() = withContext(Dispatchers.IO) {
        val data = DocUtil.getDocFile(App.instance)
        _allFiles.value = data ?: emptyList()
    }

    fun renameDoc(item: DocInfo, newName: String): DocInfo? {
        val renamed = item.renameFileAndReturnNew(newName) ?: return null

        _allFiles.update { groups ->
            groups.map { group ->
                val newList = group.docList
                    ?.map { if (it.path == item.path) renamed else it }
                    ?.toCollection(ArrayList())

                group.copy(docList = newList)
            }
        }

        return renamed
    }

    fun deleteDoc(item: DocInfo): Boolean {
        val path = item.path ?: return false
        val deleted = deleteFileSmart(App.instance, path)
        if (!deleted) return false

        _allFiles.update { groups ->
            groups.mapNotNull { group ->
                val newList = group.docList
                    ?.filter { it.path != path }
                    ?.toCollection(ArrayList())

                if (newList.isNullOrEmpty()) null
                else group.copy(docList = newList)
            }
        }

        return true
    }
}
