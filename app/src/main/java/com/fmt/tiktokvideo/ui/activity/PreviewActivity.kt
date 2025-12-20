package com.fmt.tiktokvideo.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.media3.common.util.UnstableApi
import cn.jzvd.Jzvd
import com.fmt.tiktokvideo.R
import com.fmt.tiktokvideo.databinding.ActivityPreviewBinding
import com.fmt.tiktokvideo.ext.invokeViewBinding
import com.fmt.tiktokvideo.ext.loadUrl
import com.fmt.tiktokvideo.utils.goToSettings

/**
 *  拍照/录制视频预览页面
 */
@UnstableApi
class PreviewActivity : AppCompatActivity() {

    private val mBinding: ActivityPreviewBinding by invokeViewBinding()
    private var mIsVideo: Boolean = false
    private lateinit var mRequestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
        mIsVideo = intent.getBooleanExtra(KEY_PREVIEW_VIDEO, false)
        setUpSystemBars()
        registerPermissionLauncher()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (Jzvd.backPress()) {
                    return
                }
                finish()
            }
        })
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

    private fun registerPermissionLauncher() {
        mRequestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                handlePermissionResult(isGranted)
            }
        // 兼容 Android Q 以下的文件访问
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && (ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PERMISSION_GRANTED)
        ) {
            mRequestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            onGrantPermission()
        }
    }

    /**
     *  处理权限申请结构
     */
    private fun handlePermissionResult(granted: Boolean) {
        // 权限已授予，可以直接使用功能
        if (granted) {
            onGrantPermission()
        } else {
            // 想用户解释该权限申请的必要性
            val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            )
            //用户拒绝过但愿意重新考虑，显示权限说明后再请求
            if (showRationale) {
                showNoAccess()
            } else {
                //用户永久拒绝此权限，跳转到应用设置页面
                AlertDialog.Builder(this)
                    .setMessage(getString(R.string.capture_permission_go_settings_message))
                    .setNegativeButton(getString(R.string.capture_permission_no)) { dialog, _ ->
                        dialog.dismiss()
                        finish()
                    }
                    .setPositiveButton(getString(R.string.capture_permission_go_settings)) { dialog, _ ->
                        dialog.dismiss()
                        // 打开应用设置页面
                        goToSettings()
                    }.create().show()
            }
        }
    }

    private fun onGrantPermission() {
        val previewUrl: String = intent.getStringExtra(KEY_PREVIEW_URL) ?: return finish()
        mBinding.actionOk.setOnClickListener {
            PublishActivity.start(this, previewUrl, mIsVideo)
            finish()
        }
        mBinding.actionClose.setOnClickListener {
            finish()
        }

        if (mIsVideo) {
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
    @SuppressLint("ClickableViewAccessibility")
    @OptIn(UnstableApi::class)
    private fun previewVideo(videoUrl: String) {
        mBinding.wrapperPlayerView.isVisible = true
        mBinding.wrapperPlayerView.bindData(videoUrl)
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
            mRequestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }.create().show()
    }

    override fun onDestroy() {
        mBinding.wrapperPlayerView.release()
        super.onDestroy()
    }

    companion object {
        private const val KEY_PREVIEW_URL = "preview_url"
        private const val KEY_PREVIEW_VIDEO = "preview_video"

        fun start(activity: Activity, previewUrl: String, isVideo: Boolean) {
            val intent = Intent(activity, PreviewActivity::class.java)
            intent.putExtra(KEY_PREVIEW_URL, previewUrl)
            intent.putExtra(KEY_PREVIEW_VIDEO, isVideo)
            activity.startActivity(intent)
        }
    }
}