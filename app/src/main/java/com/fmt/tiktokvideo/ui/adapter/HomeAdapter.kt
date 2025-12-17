package com.fmt.tiktokvideo.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cn.jzvd.Jzvd
import com.fmt.tiktokvideo.R
import com.fmt.tiktokvideo.databinding.ItemTiktokBinding
import com.fmt.tiktokvideo.exoplayer.MediaExo
import com.fmt.tiktokvideo.ext.loadUrl
import com.fmt.tiktokvideo.model.Item

class HomeAdapter(private val lifecycleOwner: LifecycleOwner) :
    PagingDataAdapter<Item, HomeAdapter.HomeViewHolder>(object : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem.data?.id == newItem.data?.id
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem == newItem
        }
    }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        return HomeViewHolder(
            lifecycleOwner,
            LayoutInflater.from(parent.context).inflate(R.layout.item_tiktok, parent, false)
        )
    }

    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        val video = getItem(position) ?: return
        holder.bindVideo(video)
    }

    inner class HomeViewHolder(val lifecycleOwner: LifecycleOwner, itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        private val itemTiktokBinding = ItemTiktokBinding.bind(itemView)

        @OptIn(UnstableApi::class)
        @SuppressLint("SetTextI18n")
        fun bindVideo(video: Item) {
            itemTiktokBinding.tvAlias.text = "@${video.data?.author?.name}"
            itemTiktokBinding.tvTitle.text = video.data?.title ?: ""
            itemTiktokBinding.ivAvatar.loadUrl(
                lifecycleOwner,
                video.data?.author?.icon ?: "",
                isCircle = true
            )
            itemTiktokBinding.videoView.setUp(
                video.data?.playUrl ?: "",
                video.data?.title ?: "",
                Jzvd.SCREEN_NORMAL,
                MediaExo::class.java
            )
            video.data?.cover?.let { cover ->
                itemTiktokBinding.videoView.posterImageView.loadUrl(
                    lifecycleOwner,
                    cover.feed
                )
            }
            video.data?.consumption?.let { consumption ->
                itemTiktokBinding.mbCollect.text = formatCount(consumption.collectionCount)
                itemTiktokBinding.mbComment.text = formatCount(consumption.replyCount)
                itemTiktokBinding.mbRealCollect.text = formatCount(consumption.realCollectionCount)
                itemTiktokBinding.mbShare.text = formatCount(consumption.shareCount)
            }
        }
    }
}

@SuppressLint("DefaultLocale")
fun formatCount(n: Int): String {
    return if (n < 10_000) {
        n.toString()
    } else {
        val w = (n / 100).toDouble() / 100.0  // 保留 1 位小数
        val s = String.format("%.1f", w)
        if (s.endsWith(".0")) s.dropLast(2) + "W" else "${s}W"
    }
}