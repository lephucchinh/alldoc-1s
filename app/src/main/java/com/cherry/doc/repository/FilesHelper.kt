package com.cherry.doc.repository

import android.util.Log
import com.cherry.doc.App
import com.cherry.doc.data.local.AppDatabase
import com.cherry.doc.data.model.DocGroupInfo
import com.cherry.doc.data.model.DocInfo
import com.cherry.doc.util.DocUtil
import com.cherry.doc.util.FileManager.deleteFileSmart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object FilesHelper {

    private val _allFiles =
        MutableStateFlow<List<DocGroupInfo>>(emptyList())

    val allFiles: StateFlow<List<DocGroupInfo>> = _allFiles.asStateFlow()

    val allFilesFavourite = MutableStateFlow<List<DocInfo>>(emptyList())

    suspend fun loadAllFiles() = withContext(Dispatchers.IO) {
        val data = DocUtil.getDocFile(App.instance)
        _allFiles.value = data ?: emptyList()
        loadAllFavouriteDocs()
    }

    fun renameDoc(item: DocInfo, newName: String): DocInfo? {
        val oldPath = item.path ?: return null
        val renamed = item.renameFileAndReturnNew(newName) ?: return null
        val newPath = renamed.path ?: return renamed

        // 1. Update allFiles
        _allFiles.update { groups ->
            groups.map { group ->
                group.copy(
                    docList = group.docList
                        ?.map { if (it.path == oldPath) renamed else it }
                        ?.toCollection(ArrayList())
                )
            }
        }

        // 2. Update favourite memory
        allFilesFavourite.update { list ->
            list.map { if (it.path == oldPath) renamed else it }
        }

        // 3. 🔥 UPDATE DB PATH
        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.getInstance()
                .docFavouriteDao()
                .updatePath(oldPath, newPath)
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

    fun getDocByPath(path: String): DocInfo? {
        return _allFiles.value
            .asSequence()
            .flatMap { it.docList.orEmpty().asSequence() }
            .firstOrNull { it.path == path }
    }

    suspend fun loadAllFavouriteDocs() = withContext(Dispatchers.IO) {

        // 1. Lấy list path từ DB
        val favouritePaths = AppDatabase
            .getInstance()
            .docFavouriteDao()
            .getAll()
            .first()
            .map { it.path }
            .toSet()

        if (favouritePaths.isEmpty()) {
            Log.d("chinhlllp", "loadAllFavouriteDocs: ")
            allFilesFavourite.value = emptyList()
            return@withContext
        }

        // 2. Lọc từ allFiles
        val favDocs = _allFiles.value
            .asSequence()
            .flatMap { it.docList.orEmpty().asSequence() }
            .filter { it.path != null && favouritePaths.contains(it.path) }
            .toList()

        // 3. Push vào StateFlow
        allFilesFavourite.value = favDocs
    }

    fun removeFavouriteByPath(path: String) {
        allFilesFavourite.update { list ->
            list.filter { it.path != path }
        }
    }

    fun addFavouriteByPath(path: String) {
        val doc = getDocByPath(path) ?: return

        allFilesFavourite.update { list ->
            list
                .filter { it.path != path } // xoá bản cũ nếu có
                .plus(doc)                  // add bản mới
        }
    }



}
