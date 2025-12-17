package com.fmt.tiktokvideo.ui.activity

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import cn.jzvd.Jzvd
import com.fmt.tiktokvideo.R
import com.fmt.tiktokvideo.databinding.ActivityPreviewBinding
import com.fmt.tiktokvideo.ext.invokeViewBinding
import com.fmt.tiktokvideo.ext.loadUrl

/**
 *  拍照/录制视频预览页面
 */
class PreviewActivity : AppCompatActivity() {

    private val mBinding: ActivityPreviewBinding by invokeViewBinding()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
        setUpSystemBars()
        requestPermission()
    }

    private fun setUpSystemBars() {
        // Android 10+ 关闭系统对导航栏的强制对比度优化，Android 10+ 系统会自动调整导航栏背景色以保证与内容的对比度，可能导致自定义主题失效
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // 设置状态栏图标颜色， true→ 深色图标（适合浅色状态栏背景）false→ 浅色图标（适合深色状态栏背景）
        windowInsetsController.isAppearanceLightStatusBars = false

        // 防止状态栏与顶部内容重叠
        ViewCompat.setOnApplyWindowInsetsListener(mBinding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val topInset = systemBars.top
            mBinding.root.updatePadding(top = topInset)
            insets
        }
    }

    private fun requestPermission() {
        // 兼容 Android Q 以下的文件访问
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PERMISSION_GRANTED)
        ) {
            val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(this, permissions, REQ_PREVIEW)
        } else {
            onGrantPermission()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_PREVIEW) {
            if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                onGrantPermission()
            } else {
                // 想用户解释该权限申请的必要性
                val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE
                )
                if (showRationale) {
                    showNoAccess()
                } else {
                    goToSettings()
                }
            }
        }
    }

    private fun onGrantPermission() {
        val previewUrl: String = intent.getStringExtra(KEY_PREVIEW_URL)
            ?: return finish()
        val isVideo: Boolean = intent.getBooleanExtra(KEY_PREVIEW_VIDEO, false)
        mBinding.actionOk.setOnClickListener {
            PublishActivity.start(this, previewUrl, isVideo)
            finish()
        }
        mBinding.actionClose.setOnClickListener {
            finish()
        }

        if (isVideo) {
            previewVideo(previewUrl)
        } else {
            previewImage(previewUrl)
        }
    }

    /***
     *  预览图片
     */
    private fun previewImage(imageUrl: String) {
        mBinding.photoView.isVisible = true
        mBinding.photoView.loadUrl(this, imageUrl)
    }

    /**
     *  预览视频
     */
    private fun previewVideo(videoUrl: String) {
        mBinding.playerView.isVisible = true
        mBinding.playerView.setUp(videoUrl, "")
        mBinding.playerView.startVideo()
    }

    /**
     *  用户点击不在询问后，跳转至设置页面
     */
    private fun goToSettings() {
        Intent(ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri()).apply {
            this.addCategory(Intent.CATEGORY_DEFAULT)
            this.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.also { intent ->
            startActivity(intent)
        }
    }

    /**
     * 显示解释对话框
     */
    private fun showNoAccess() {
        AlertDialog.Builder(this).setTitle(R.string.preview_permission_message).setPositiveButton(
            R.string.capture_permission_no
        ) { _, _ ->
            finish()
        }.setNegativeButton(R.string.capture_permission_ok) { _, _ ->
            requestPermission()
        }.create().show()
    }

    override fun onPause() {
        super.onPause()
        Jzvd.releaseAllVideos()
    }

    override fun onBackPressed() {
        if (Jzvd.backPress()) {
            return
        }
        super.onBackPressed()
    }

    companion object {
        private const val KEY_PREVIEW_URL = "preview_url"
        private const val KEY_PREVIEW_VIDEO = "preview_video"
        const val REQ_PREVIEW = 1000

        fun start(activity: Activity, previewUrl: String, isVideo: Boolean) {
            val intent = Intent(activity, PreviewActivity::class.java)
            intent.putExtra(KEY_PREVIEW_URL, previewUrl)
            intent.putExtra(KEY_PREVIEW_VIDEO, isVideo)
            activity.startActivity(intent)
        }
    }
}