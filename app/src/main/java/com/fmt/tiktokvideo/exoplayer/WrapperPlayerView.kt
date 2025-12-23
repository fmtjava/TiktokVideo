package com.fmt.tiktokvideo.exoplayer

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerControlView
import com.fmt.tiktokvideo.R
import com.fmt.tiktokvideo.databinding.LayoutExoPlayerControllerViewBinding
import com.fmt.tiktokvideo.databinding.LayoutWrapperPlayerViewBinding
import com.fmt.tiktokvideo.utils.PixelConverter

@UnstableApi
class WrapperPlayerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs), Player.Listener,
    PlayerControlView.VisibilityListener {

    private val mViewBinding =
        LayoutWrapperPlayerViewBinding.inflate(LayoutInflater.from(context), this)
    private var mPlayer: ExoPlayer? = null
    private var mIsPlaying: Boolean = false
    private var mControllerView: PlayerControlView? = null

    @SuppressLint("ClickableViewAccessibility")
    @OptIn(UnstableApi::class)
    fun bindData(videoUrl: String) {
        mViewBinding.playBtn.isVisible = true
        mViewBinding.playerView.isVisible = true
        mViewBinding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        // 先释放之前的 controllerView（如果存在）
        mControllerView?.removeVisibilityListener(this)
        mControllerView =  LayoutExoPlayerControllerViewBinding.inflate(LayoutInflater.from(context))?.exoController
        mControllerView?.addVisibilityListener(this)
        // 注意：要先移除掉 PlayerControlView 的父容器
        val ctrlParent = mControllerView?.parent
        if (ctrlParent != mViewBinding.root) {
            if (ctrlParent != null) {
                (ctrlParent as ViewGroup).removeView(mControllerView)
            }
        }
        val ctrlParams = LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
        )
        ctrlParams.bottomMargin = PixelConverter.dpToPx(context, 20f)
        ctrlParams.gravity = Gravity.BOTTOM
        // 将 PlayerControlView 添加到底部
        addView(mControllerView, ctrlParams)
        // 创建 ExoPlayer
        mPlayer = ExoPlayer.Builder(context).build().also { exoPlayer ->
            exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
            // 重点：关联 播放器实例 和 playerView 和 controllerView
            mViewBinding.playerView.player = exoPlayer
            mControllerView?.player = exoPlayer
            exoPlayer.setMediaItem(MediaItem.fromUri(videoUrl))
            exoPlayer.addListener(this)
            exoPlayer.playWhenReady = true
            exoPlayer.prepare()
        }
        mViewBinding.playerView.setOnTouchListener { _, _ ->
            mControllerView?.show()
            true
        }
        mViewBinding.playBtn.setOnClickListener {
            if (mIsPlaying) {
                mPlayer?.playWhenReady = false
                mViewBinding.playBtn.setImageResource(R.mipmap.icon_video_play)
            } else {
                mPlayer?.playWhenReady = true
                mViewBinding.playBtn.setImageResource(R.mipmap.icon_video_pause)
                mControllerView?.show()
                if (mPlayer?.playbackState == Player.STATE_ENDED) {
                    mPlayer?.seekTo(0)
                }
            }
        }
    }

    /**
     * 视频播放状态回调
     */
    @Deprecated("Deprecated in Java")
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        super.onPlayerStateChanged(playWhenReady, playbackState)
        // 判断视频是否正在播放中
        mIsPlaying = playbackState == Player.STATE_READY && playWhenReady
        // 根据播放状态控制播放按钮的显隐藏
        if (mIsPlaying) {
            mViewBinding.playBtn.isVisible = true
            mViewBinding.playBtn.setImageResource(R.mipmap.icon_video_pause)
        }
        if (playbackState == Player.STATE_ENDED) {
            mViewBinding.playBtn.isVisible = true
            mViewBinding.playBtn.setImageResource(R.mipmap.icon_video_play)
        }
    }

    /**
     * 控制器可见状态回调
     */
    override fun onVisibilityChange(visibility: Int) {
        // 播放按钮的显隐藏与控制器保持一致
        mViewBinding.playBtn.isVisible =
            if (mPlayer?.playbackState == Player.STATE_ENDED) true else visibility == VISIBLE
    }

    fun release() {
        // 移除监听器，防止内存泄漏
        mPlayer?.removeListener(this)
        // 移除控制器可见性监听器
        mControllerView?.removeVisibilityListener(this)
        mControllerView = null
        // 释放播放器
        mPlayer?.playWhenReady = false
        mPlayer?.release()
        mPlayer = null
    }
}