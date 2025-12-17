package com.fmt.tiktokvideo.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fmt.tiktokvideo.R
import com.fmt.tiktokvideo.databinding.LayoutListLoadingFooterBinding

class FooterLoadStateAdapter(val retry: () -> Unit) :
    LoadStateAdapter<FooterLoadStateAdapter.LoadStateViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoadStateViewHolder {
        val binding = LayoutListLoadingFooterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        binding.retryAction.setOnClickListener {
            retry()
        }
        return LoadStateViewHolder(binding)
    }


    override fun onBindViewHolder(holder: LoadStateViewHolder, loadState: LoadState) {
        val loading = holder.binding.loading
        val loadingText = holder.binding.text
        val retryAction = holder.binding.retryAction

        when (loadState) {
            is LoadState.Loading -> {
                loading.isVisible = true
                loadingText.isVisible = true
                retryAction.isVisible = false
                loadingText.setText(R.string.abs_list_loading_footer_loading)
                loading.show()
                return
            }

            is LoadState.NotLoading -> {
                loadingText.isVisible = true
                retryAction.isVisible = false
                loadingText.setText(R.string.abs_list_loading_footer_no_data)
            }

            is LoadState.Error -> {
                loadingText.isVisible = false
                retryAction.isVisible = true
            }

            else -> {

            }
        }
        loading.hide()
        loading.postOnAnimation { loading.isVisible = false }
    }

    inner class LoadStateViewHolder(val binding: LayoutListLoadingFooterBinding) :
        RecyclerView.ViewHolder(binding.root)
}