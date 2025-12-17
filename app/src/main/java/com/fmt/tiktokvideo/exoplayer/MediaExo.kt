package com.fmt.tiktokvideo.exoplayer

import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Surface
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import cn.jzvd.JZMediaInterface
import cn.jzvd.Jzvd
import java.io.File

/**
 *  ExoPlayer 引擎
 */
@UnstableApi
class MediaExo(jzvd: Jzvd?) : JZMediaInterface(jzvd), Player.Listener {

    private var mExoPlayer: ExoPlayer? = null
    private var mPreviousSeek: Long = 0
    private var mCallback: Runnable? = null

    /**
     *  SimpleCache 不允许同一文件夹被多个实例使用，这里使用单例模式管理 SimpleCache
     */
    companion object {
        @Volatile
        private var sCache: SimpleCache? = null
        // 使用引用计数跟踪使用缓存的实例数量，
        private var sCacheRefCount = 0

        @OptIn(UnstableApi::class)
        @Synchronized
        fun getOrCreateCache(context: android.content.Context): SimpleCache {
            if (sCache == null) {
                sCache = SimpleCache(
                    context.cacheDir,
                    LeastRecentlyUsedCacheEvictor(200 * 1024 * 1024),
                    StandaloneDatabaseProvider(context)
                )
            }
            // 增加引用计数
            sCacheRefCount++
            return sCache!!
        }

        @Synchronized
        fun releaseCache() {
            // 减少引用计数
            sCacheRefCount--
            // 当所有实例都释放后，才真正释放 SimpleCache
            if (sCacheRefCount <= 0 && sCache != null) {
                try {
                    sCache?.release()
                } catch (e: Exception) {
                    // 忽略释放异常
                    e.printStackTrace()
                }
                sCache = null
                sCacheRefCount = 0
            }
        }
    }

    /**
     *  开始播放
     */
    override fun start() {
        mExoPlayer?.playWhenReady = true
    }

    /**
     * 准备资源
     */
    override fun prepare() {
        release()
        mMediaHandlerThread = HandlerThread("JZVD")
        mMediaHandler = Handler(Looper.getMainLooper())
        handler = Handler()
        mMediaHandler.post {
            // 使用单例 SimpleCache，避免重复创建导致异常
            val cache = getOrCreateCache(jzvd.context)
            // 配置带缓存的数据源
            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
                .setFlags(CacheDataSource.FLAG_BLOCK_ON_CACHE)

            mExoPlayer = ExoPlayer.Builder(jzvd.context)
                .setMediaSourceFactory(ProgressiveMediaSource.Factory(cacheDataSourceFactory))
                .build()
            mExoPlayer?.repeatMode = Player.REPEAT_MODE_OFF
            val currUrl = jzvd.jzDataSource.currentUrl.toString()
            val mediaItem: MediaItem
            val file = File(currUrl)
            mediaItem = if (file.exists()) {
                MediaItem.fromUri(file.absolutePath)
            } else {
                MediaItem.fromUri(currUrl)
            }
            // 监听播放状态（缓冲、播放、暂停、结束）、错误、进度变化等，更新 UI 或处理逻辑
            mExoPlayer?.addListener(this)
            // // 播放时自动缓存（下次播放优先读缓存）
            mExoPlayer?.setMediaItem(mediaItem)
            mExoPlayer?.prepare()
            mExoPlayer?.playWhenReady = true
            mCallback = OnBufferingUpdate()

            // 为 ExoPlayer 设置 Surface，提供视频显示画面
            jzvd.textureView?.surfaceTexture?.let {
                mExoPlayer?.setVideoSurface(Surface(it))
            }
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        if (SAVED_SURFACE == null) {
            SAVED_SURFACE = surface
            prepare()
        } else {
            jzvd.textureView?.setSurfaceTexture(SAVED_SURFACE)
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {

    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        // 这里返回 false：SurfaceTexture 保留，ExoPlayer 可以安全完成操作，在 release() 中统一清理
        // 如果返回 true：SurfaceTexture 立即被释放，但 ExoPlayer 可能还在使用 → 崩溃
        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
    }

    /**
     *  暂停
     */
    override fun pause() {
        mExoPlayer?.playWhenReady = false
    }

    /**
     *  是否播放中
     */
    override fun isPlaying(): Boolean {
        return mExoPlayer?.playWhenReady ?: false
    }

    /**
     * 跳转到指定时间点播放
     */
    override fun seekTo(time: Long) {
        if (mExoPlayer == null) return
        if (time != mPreviousSeek) {
            if (time >= mExoPlayer!!.bufferedPosition) {
                jzvd.onStatePreparingPlaying()
            }
            mExoPlayer?.seekTo(time)
            mPreviousSeek = time
            jzvd.seekToInAdvance = time
        }
    }

    /**
     *  是否资源
     */
    override fun release() {
        if (mMediaHandler != null && mMediaHandlerThread != null && mExoPlayer != null) {
            val tmpHandlerThread = mMediaHandlerThread
            val tmpMediaPlayer: ExoPlayer? = mExoPlayer
            SAVED_SURFACE = null

            mMediaHandler.post {
                // 1、先清除 Surface，再释放 ExoPlayer
                tmpMediaPlayer?.setVideoSurface(null)
                tmpMediaPlayer?.release()
                // 2、释放 SimpleCache 引用
                releaseCache()
                tmpHandlerThread.quit()
            }
            // 3、mExoPlayer 置空
            mExoPlayer = null
        }
    }

    /**
     *  获取当前的播放位置
     */
    override fun getCurrentPosition(): Long {
        if (mExoPlayer != null) {
            return mExoPlayer!!.currentPosition
        }
        return 0
    }

    /**
     *  获取播放时间
     */
    override fun getDuration(): Long {
        if (mExoPlayer != null) {
            return mExoPlayer!!.duration
        }
        return 0
    }

    /**
     *  设置音量
     */
    override fun setVolume(leftVolume: Float, rightVolume: Float) {
        mExoPlayer?.volume = leftVolume
        mExoPlayer?.volume = rightVolume
    }

    /**
     *  设置倍速播放
     */
    override fun setSpeed(speed: Float) {
        // 倍速播放（0.5x~2.0x）
        val playbackParameters = PlaybackParameters(speed, 1.0f)
        mExoPlayer?.playbackParameters = playbackParameters
    }

    /**
     *  设置播放 Surface
     */
    override fun setSurface(surface: Surface) {
        mExoPlayer?.setVideoSurface(surface)
    }

    /**
     *  视频大小改变回调
     */
    override fun onVideoSizeChanged(videoSize: VideoSize) {
        handler.post {
            jzvd.onVideoSizeChanged(
                (videoSize.width * videoSize.pixelWidthHeightRatio).toInt(),
                videoSize.height
            )
        }
    }

    /**
     *  视频播放状态改变回调
     */
    @Deprecated("Deprecated in Java")
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        handler.post {
            when (playbackState) {
                Player.STATE_IDLE -> {}
                Player.STATE_BUFFERING -> {
                    jzvd.onStatePreparingPlaying()
                    mCallback?.let { handler.post(it) }
                }

                Player.STATE_READY -> {
                    if (playWhenReady) {
                        jzvd.onStatePlaying()
                    }
                }

                Player.STATE_ENDED -> {
                    jzvd.onCompletion()
                }
            }
        }
    }

    /**
     * 缓冲进度更新
     */
    private inner class OnBufferingUpdate : Runnable {
        override fun run() {
            if (mExoPlayer != null && mCallback != null) {
                val percent: Int = mExoPlayer!!.bufferedPercentage
                handler.post { jzvd.setBufferProgress(percent) }
                if (percent < 100) {
                    handler.postDelayed(mCallback!!, 300)
                } else {
                    handler.removeCallbacks(mCallback!!)
                }
            }
        }
    }

    /**
     *  播放错误回调
     */
    override fun onPlayerError(error: PlaybackException) {
        handler.post { jzvd.onError(1000, 1000) }
    }
}