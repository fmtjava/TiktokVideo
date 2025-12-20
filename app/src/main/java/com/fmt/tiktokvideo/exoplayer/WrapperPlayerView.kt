package com.fmt.tiktokvideo.exoplayer

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
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

    @SuppressLint("ClickableViewAccessibility")
    @OptIn(UnstableApi::class)
    fun bindData(videoUrl: String) {
        mViewBinding.playBtn.isVisible = true
        mViewBinding.playerView.isVisible = true
        mViewBinding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        val controllerView =
            LayoutExoPlayerControllerViewBinding.inflate(LayoutInflater.from(context)).exoController
        controllerView.addVisibilityListener(this)
        val ctrlParent = controllerView.parent
        if (ctrlParent != mViewBinding.root) {
            if (ctrlParent != null) {
                (ctrlParent as ViewGroup).removeView(controllerView)
            }
        }
        val ctrlParams = LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
        )
        ctrlParams.bottomMargin = PixelConverter.dpToPx(context, 20f)
        ctrlParams.gravity = Gravity.BOTTOM
        addView(controllerView, ctrlParams)
        mPlayer = ExoPlayer.Builder(context).build().also { exoPlayer ->
            exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
            // 关联 播放器实例 和 playerView 和 controllerView
            mViewBinding.playerView.player = exoPlayer
            controllerView.player = exoPlayer
            exoPlayer.setMediaItem(MediaItem.fromUri(videoUrl))
            exoPlayer.addListener(this)
            exoPlayer.playWhenReady = true
            exoPlayer.prepare()
        }
        mViewBinding.playerView.setOnTouchListener { _, _ ->
            controllerView.show()
            true
        }
        mViewBinding.playBtn.setOnClickListener {
            if (mIsPlaying) {
                mPlayer?.playWhenReady = false
                mViewBinding.playBtn.setImageResource(R.mipmap.icon_video_play)
            } else {
                mPlayer?.playWhenReady = true
                mViewBinding.playBtn.setImageResource(R.mipmap.icon_video_pause)
                controllerView.show()
                if (mPlayer?.playbackState == Player.STATE_ENDED) {
                    mPlayer?.seekTo(0)
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        super.onPlayerStateChanged(playWhenReady, playbackState)
        mIsPlaying = playbackState == Player.STATE_READY && playWhenReady
        if (mIsPlaying) {
            mViewBinding.playBtn.isVisible = true
            mViewBinding.playBtn.setImageResource(R.mipmap.icon_video_pause)
        }
        if (playbackState == Player.STATE_ENDED) {
            mViewBinding.playBtn.isVisible = true
            mViewBinding.playBtn.setImageResource(R.mipmap.icon_video_play)
        }
    }

    override fun onVisibilityChange(visibility: Int) {
        mViewBinding.playBtn.isVisible =
            if (mPlayer?.playbackState == Player.STATE_ENDED) true else visibility == View.VISIBLE
    }

    fun release() {
        mPlayer?.playWhenReady = false
        mPlayer?.release()
        mPlayer = null
    }
}