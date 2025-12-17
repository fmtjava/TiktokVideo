package com.fmt.tiktokvideo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import com.fmt.tiktokvideo.http.ApiService
import com.fmt.tiktokvideo.model.Item

class HomeViewModel : ViewModel() {

    val pageFlow = Pager(
        config = PagingConfig(
            pageSize = 10,
            initialLoadSize = 10,
            enablePlaceholders = false,
            prefetchDistance = 1
        ), pagingSourceFactory = {
            HomePageSource()
        }).flow.cachedIn(viewModelScope)

    inner class HomePageSource : PagingSource<String, Item>() {

        override fun getRefreshKey(state: PagingState<String, Item>): String? {
            return null
        }

        override suspend fun load(params: LoadParams<String>): LoadResult<String, Item> {
            return try {
                val pageKey: String? = params.key
                var nextKey: String? = null
                val nextPageUrl: String?
                val daily = if (pageKey.isNullOrEmpty()) {
                    ApiService.get().getDaily()
                } else {
                    ApiService.get().getDaily(pageKey)
                }
                nextPageUrl = daily.nextPageUrl
                val itemList = daily.issueList[0].itemList
                itemList.removeAll {
                    it.type == "banner2" || it.type == "textHeader"
                }
                if (!nextPageUrl.isNullOrEmpty()) {
                    nextKey = nextPageUrl
                }
                if (itemList.isNotEmpty()) {
                    return LoadResult.Page(itemList, null, nextKey)
                }
                return if (params.key == null) LoadResult.Page(
                    arrayListOf(),
                    null,
                    null
                ) else LoadResult.Error(java.lang.RuntimeException("No more data to fetch"))
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }
    }
}