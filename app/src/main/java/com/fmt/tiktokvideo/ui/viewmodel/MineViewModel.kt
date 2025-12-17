package com.fmt.tiktokvideo.ui.viewmodel

import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fmt.tiktokvideo.http.ApiService
import com.fmt.tiktokvideo.model.Item
import com.fmt.tiktokvideo.utils.AppGlobals
import kotlinx.coroutines.launch

class MineViewModel : ViewModel() {

    // 为不同 tab 独立维护数据和刷新状态，避免切换时重复请求
    val workItems = mutableStateListOf<Item>()
    val loveItems = mutableStateListOf<Item>()
    val refreshingWorkState = mutableStateOf(false)
    val refreshingLoveState = mutableStateOf(false)
    val loadMoreWorkState = mutableStateOf(true)
    val loadMoreLoveState = mutableStateOf(true)
    var nextPageUrl: String? = null

    fun getTabData(url: String, isRefresh: Boolean = false) {
        val targetList = if (url.contains("strategy=weekly")) workItems else loveItems
        val refreshingState =
            if (url.contains("strategy=weekly")) refreshingWorkState else refreshingLoveState
        val loadMoreState =
            if (url.contains("strategy=weekly")) loadMoreWorkState else loadMoreLoveState
        if (isRefresh) {
            refreshingState.value = true
        }
        viewModelScope.launch {
            runCatching {
                val itemList = ApiService.get().getMineVideoData(url).itemList
                nextPageUrl = ApiService.get().getMineVideoData(url).nextPageUrl
                if (nextPageUrl == null) {
                    loadMoreState.value = false
                }
                if (isRefresh) {
                    targetList.clear()
                    refreshingState.value = false
                }
                targetList.addAll(itemList)
            }.onFailure {
                Toast.makeText(AppGlobals.getApplication(), it.message ?: "", Toast.LENGTH_LONG)
                    .show()
                if (isRefresh) {
                    refreshingState.value = false
                }
            }
        }
    }

    fun loadMoreData() {
        nextPageUrl?.let {
            getTabData(it, false)
        }
    }
}