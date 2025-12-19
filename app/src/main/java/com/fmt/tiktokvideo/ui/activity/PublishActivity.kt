package com.fmt.tiktokvideo.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.fmt.tiktokvideo.AppContext
import com.fmt.tiktokvideo.R
import com.fmt.tiktokvideo.databinding.ActivityPublishBinding
import com.fmt.tiktokvideo.ext.hideKeyboard
import com.fmt.tiktokvideo.ext.invokeViewBinding
import com.fmt.tiktokvideo.ext.loadUrl
import com.fmt.tiktokvideo.utils.FileUtil
import com.fmt.tiktokvideo.work.UploadFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 *  图片/视频发布页面
 */
class PublishActivity : AppCompatActivity() {

    private val mBinding by invokeViewBinding<ActivityPublishBinding>()

    private var mPreviewUrl: String? = null
    private var mIsVideo: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
        setUpSystemBars()
        mPreviewUrl = intent.getStringExtra(KEY_PREVIEW_URL)
        mIsVideo = intent.getBooleanExtra(KEY_PREVIEW_VIDEO, false)

        showFileThumbnail()

        mBinding.actionClose.setOnClickListener {
            finish()
        }

        mBinding.actionPublish.setOnClickListener {
            publish()
        }
    }

    /**
     *  设置沉浸式状态栏
     */
    private fun setUpSystemBars() {
        // Android 10+ 关闭系统对导航栏的强制对比度优化，Android 10+ 系统会自动调整导航栏背景色以保证与内容的对比度，可能导致自定义主题失效
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // 设置状态栏图标颜色， true→ 深色图标（适合浅色状态栏背景）false→ 浅色图标（适合深色状态栏背景）
        windowInsetsController.isAppearanceLightStatusBars = true

        // 使用 WindowInsets 绘制状态栏背景（符合 Android 15 要求）
        // 根布局已有白色背景，会自动延伸到状态栏区域
        ViewCompat.setOnApplyWindowInsetsListener(mBinding.root) { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(0, statusBarInsets.top, 0, 0)
            insets
        }
    }

    /**
     *  展示封面图
     */
    private fun showFileThumbnail() {
        if (TextUtils.isEmpty(mPreviewUrl)) {
            return
        }
        if (mIsVideo == true) {
            lifecycleScope.launch {
                val cover = withContext(Dispatchers.IO) {
                    FileUtil.generateVideoCover(videoFilePath = mPreviewUrl!!, limit = 200)
                }
                if (!TextUtils.isEmpty(cover)) {
                    mBinding.cover.loadUrl(this@PublishActivity, cover)
                }
            }
        } else {
            mBinding.cover.loadUrl(this, mPreviewUrl)
        }
        mBinding.videoIcon.isVisible = mIsVideo ?: false
    }

    /**
     *  图片/视频上传
     */
    private fun publish() {
        // 隐藏键盘
        hideKeyboard()

        if (TextUtils.isEmpty(mBinding.inputView.text.toString())) {
            Toast.makeText(
                AppContext, R.string.file_upload_desc_illegal,
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        mBinding.actionPublish.isVisible = false
        mBinding.actionPublishProgress.isVisible = true
        mBinding.actionPublishProgress.show()

        lifecycleScope.launch {
            if (!TextUtils.isEmpty(mPreviewUrl) && mIsVideo != null) {
                UploadFileManager.upload(
                    mPreviewUrl!!,
                    mIsVideo!!
                ) { coverFileUploadUrl, originalFileUploadUrl ->
                    if (TextUtils.isEmpty(originalFileUploadUrl) || mIsVideo!! && TextUtils.isEmpty(
                            coverFileUploadUrl
                        )
                    ) {
                        Toast.makeText(AppContext, R.string.file_upload_fail, Toast.LENGTH_SHORT)
                            .show()
                        mBinding.actionPublish.isVisible = true
                        mBinding.actionPublishProgress.isVisible = false
                        mBinding.actionPublishProgress.hide()
                        return@upload
                    }
                    Toast.makeText(AppContext, R.string.file_upload_success, Toast.LENGTH_SHORT)
                        .show()
                    val intent = Intent(this@PublishActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    companion object {
        private const val KEY_PREVIEW_URL = "preview_url"
        private const val KEY_PREVIEW_VIDEO = "preview_video"

        fun start(activity: Activity, previewUrl: String, isVideo: Boolean) {
            val intent = Intent(activity, PublishActivity::class.java)
            intent.putExtra(KEY_PREVIEW_URL, previewUrl)
            intent.putExtra(KEY_PREVIEW_VIDEO, isVideo)
            activity.startActivity(intent)
        }
    }
}