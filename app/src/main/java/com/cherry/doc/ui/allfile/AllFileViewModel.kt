package com.cherry.doc.ui.allfile

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cherry.doc.data.DocGroupInfo
import com.cherry.doc.data.DocInfo
import com.cherry.doc.util.DocUtil
import com.cherry.doc.util.FileManager.deleteFileSmart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AllFileViewModel(application: Application) : AndroidViewModel(application) {

//    private val _allFiles = MutableLiveData<List<DocGroupInfo>?>()
//    val allFiles: LiveData<List<DocGroupInfo>?> = _allFiles
//
//    fun loadAllFiles() {
//        viewModelScope.launch(Dispatchers.IO) {
//            val data = DocUtil.getDocFile(getApplication())
//            _allFiles.postValue(data)
//        }
//    }

//    fun renameDoc(item: DocInfo, newName: String): DocInfo? {
//        val renamed = item.renameFileAndReturnNew(newName) ?: return null
//
//        val newGroups = _allFiles.value?.map { group ->
//            group.copy(
//                docList = group.docList?.map {
//                    if (it.path == item.path) renamed else it
//                } as ArrayList<DocInfo>?
//            )
//        }
//
//        _allFiles.postValue(newGroups)
//        return renamed
//    }
//
//    fun deleteDoc(item: DocInfo): Boolean {
//       val deleted =  item.path?.let { deleteFileSmart(getApplication() , it) } ?: return false
//        if (deleted.not()) return false
//
//        val updatedGroups = _allFiles.value
//            ?.mapNotNull { group ->
//                val newList = group.docList
//                    ?.filter { it.path != item.path }
//                    ?.toCollection(ArrayList())
//
//                // nếu group còn item → giữ
//                if (!newList.isNullOrEmpty()) {
//                    group.copy(docList = newList)
//                } else {
//                    null // group rỗng → xoá luôn
//                }
//            }
//
//        _allFiles.postValue(updatedGroups)
//        return true
//    }



}
