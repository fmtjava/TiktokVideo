package com.fmt.tiktokvideo.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.util.Size
import android.view.MotionEvent
import android.view.Surface
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraProvider
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import com.fmt.nav_annotation.NavDestination
import com.fmt.tiktokvideo.R
import com.fmt.tiktokvideo.databinding.ActivityCaptureBinding
import com.fmt.tiktokvideo.ext.invokeViewBinding
import com.fmt.tiktokvideo.utils.goToSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 *  图片/视频拍摄页面
 */
@NavDestination(route = "activity_capture", type = NavDestination.NavType.Activity)
class CaptureActivity : AppCompatActivity() {

    private val mBinding by invokeViewBinding<ActivityCaptureBinding>()
    private var mCamera: Camera? = null // 相机
    private var mImageCapture: ImageCapture? = null // 图片拍摄
    private var mVideoCapture: VideoCapture<Recorder>? = null // 视频拍摄
    private var mVideoRecording: Recording? = null // 视频录制
    private var mCameraProvider: ProcessCameraProvider? = null // 摄像头提供者
    private var mCurrentLensFacing: Int = CameraSelector.LENS_FACING_BACK // 默认后置
    private var mFlashMode: Int = ImageCapture.FLASH_MODE_OFF // 闪光灯状态，默认关闭
    private lateinit var mPickMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var mRequestMultiplePermissionsLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
        setUpSystemBars()
        registerPickMediaLauncher()
        registerMultiplePermissionsLauncher()
        initListener()
    }

    private fun registerPickMediaLauncher() {
        mPickMediaLauncher =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    onFileSaved(uri)
                }
            }
    }

    private fun registerMultiplePermissionsLauncher() {
        mRequestMultiplePermissionsLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                handleMultiplePermissionsResult(permissions)
            }
        val neededPermissions = PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(
                this, permission
            ) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        if (neededPermissions.isNotEmpty()) {
            mRequestMultiplePermissionsLauncher.launch(neededPermissions)
        } else {
            startCamera()
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
        windowInsetsController.isAppearanceLightStatusBars = false

        // 防止状态栏与顶部内容重叠
        ViewCompat.setOnApplyWindowInsetsListener(mBinding.flController) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val topInset = systemBars.top
            mBinding.flController.updatePadding(top = topInset)
            insets
        }
    }

    private fun initListener() {
        mBinding.actionClose.setOnClickListener {
            finish()
        }
        mBinding.actionFlip.setOnClickListener {
            switchCamera()
        }
        mBinding.actionFlash.setOnClickListener {
            toggleFlash()
        }
        mBinding.actionAlbum.setOnClickListener {
            mPickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        }
    }

    /**
     *  摄像头切换
     */
    private fun switchCamera() {
        val cameraProvider = mCameraProvider ?: return
        mCurrentLensFacing = when (mCurrentLensFacing) {
            CameraSelector.LENS_FACING_BACK -> {
                if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
            }

            CameraSelector.LENS_FACING_FRONT -> {
                if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    CameraSelector.LENS_FACING_BACK
                } else {
                    CameraSelector.LENS_FACING_FRONT
                }
            }

            else -> throw IllegalStateException("Back and Front camera are unavailable")
        }
        // 重新绑定摄像头
        bindCamera()
    }

    /**
     * 打开/关闭闪光灯
     */
    private fun toggleFlash() {
        // 前置摄像头不支持闪光灯
        if (mCurrentLensFacing == CameraSelector.LENS_FACING_FRONT) {
            Toast.makeText(applicationContext, "前置摄像头不支持闪光灯", Toast.LENGTH_SHORT).show()
            return
        }
        // 切换闪光灯状态
        mFlashMode = when (mFlashMode) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_OFF
            else -> ImageCapture.FLASH_MODE_OFF
        }
        // 更新 ImageCapture 的闪光灯设置
        mImageCapture?.flashMode = mFlashMode

        // 更新按钮UI
        updateFlashButtonUI()
    }

    /**
     *  权限申请处理
     */
    private fun handleMultiplePermissionsResult(permissions: Map<String, Boolean>) {
        val deniedPermissions = permissions.filter { !it.value }.keys
        if (deniedPermissions.isEmpty()) {
            startCamera()
        } else {
            // 检查是否有权限被永久拒绝（不再询问）
            val permanentlyDeniedPermissions = deniedPermissions.filter { permission ->
                !shouldShowRequestPermissionRationale(permission)
            }
            if (permanentlyDeniedPermissions.isNotEmpty()) {
                // 有权限被永久拒绝，需要引导用户去设置页面
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
            } else {
                // 权限没有被永久拒绝，可以再次申请
                AlertDialog.Builder(this).setMessage(getString(R.string.capture_permission_message))
                    .setNegativeButton(getString(R.string.capture_permission_no)) { dialog, _ ->
                        dialog.dismiss()
                        this@CaptureActivity.finish()
                    }.setPositiveButton(getString(R.string.capture_permission_ok)) { dialog, _ ->
                        mRequestMultiplePermissionsLauncher.launch(deniedPermissions.toTypedArray())
                        dialog.dismiss()
                    }.create().show()
            }
        }
    }

    /**
     *  开启预览
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            mCameraProvider = cameraProvider
            // 初始化摄像头方向
            mCurrentLensFacing = when {
                cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.LENS_FACING_BACK
                cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("Back and Front camera are unavailable")
            }
            bindCamera()
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     *  绑定摄像头
     */
    private fun bindCamera() {
        val cameraProvider = mCameraProvider ?: return
        // 根据当前方向创建 CameraSelector
        val cameraSelector = when (mCurrentLensFacing) {
            CameraSelector.LENS_FACING_BACK -> CameraSelector.DEFAULT_BACK_CAMERA
            CameraSelector.LENS_FACING_FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
            else -> throw IllegalStateException("Back and Front camera are unavailable")
        }
        // 前置摄像头不支持闪光灯，自动关闭
        if (mCurrentLensFacing == CameraSelector.LENS_FACING_FRONT) {
            mFlashMode = ImageCapture.FLASH_MODE_OFF
        }
        // 更新闪光灯按钮UI
        updateFlashButtonUI()
        // preview 画面预览
        val displayRotation = mBinding.previewView.display?.rotation ?: Surface.ROTATION_0
        val preview = Preview.Builder().setTargetRotation(displayRotation).build().also {
            it.surfaceProvider = mBinding.previewView.surfaceProvider
        }

        // imageCapture 图片拍摄，设置图片拍摄质量参数
        this.mImageCapture = ImageCapture.Builder().setTargetRotation(displayRotation)
            .setCaptureMode(CAPTURE_MODE_MINIMIZE_LATENCY) // 压缩图片的质量
            .setJpegQuality(100).setFlashMode(mFlashMode) // 设置闪光灯模式
            .setResolutionSelector(
                ResolutionSelector.Builder().setResolutionStrategy(
                    ResolutionStrategy(
                        Size(1920, 1080), // 期望的最大分辨率
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER // 降级规则
                    )
                ).build()
            ).build()
        val useCases = mutableListOf(preview, mImageCapture)
        if (isSupportCombinedUsages(cameraSelector, cameraProvider)) {
            // 设置视频录制参数
            val recorder = Recorder.Builder()
                .setQualitySelector(getQualitySelector(cameraSelector, cameraProvider)).build()
            mVideoCapture = VideoCapture.withOutput(recorder)
            useCases.add(mVideoCapture)
        }
        // 注意：这里要 try - catch，bindToLifecycle 极端情况下会报错
        try {
            // 注意：要先解绑
            cameraProvider.unbindAll()
            this.mCamera = cameraProvider.bindToLifecycle(
                this, cameraSelector, *useCases.toTypedArray()
            )
            bindUI()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 更新闪光灯按钮UI
     */
    private fun updateFlashButtonUI() {
        val iconRes = when (mFlashMode) {
            ImageCapture.FLASH_MODE_ON -> R.mipmap.icon_flash_on
            ImageCapture.FLASH_MODE_OFF -> R.mipmap.icon_flash_close
            else -> R.mipmap.icon_flash_close
        }
        mBinding.actionFlash.setIconResource(iconRes)

        // 如果前置摄像头，禁用按钮
        mBinding.actionFlash.isEnabled = mCurrentLensFacing == CameraSelector.LENS_FACING_BACK
    }

    /**
     *  获取支持的视频质量等级
     */
    @SuppressLint("RestrictedApi")
    private fun getQualitySelector(
        cameraSelector: CameraSelector, cameraProvider: ProcessCameraProvider
    ): QualitySelector {
        val cameraInfo = cameraProvider.availableCameraInfos.filter {
            it.lensFacing == cameraSelector.lensFacing
        }
        if (cameraInfo.isEmpty()) {
            return QualitySelector.from(Quality.SD)
        }
        val supportQualities = Recorder.getVideoCapabilities(cameraInfo.first())
            .getSupportedQualities(DynamicRange.SDR).filter {
                // 超清、高清、标清
                listOf(Quality.FHD, Quality.HD, Quality.SD).contains(it)
            }
        return QualitySelector.from(supportQualities[0])
    }

    @SuppressLint("ClickableViewAccessibility", "RestrictedApi")
    private fun bindUI() {
        mBinding.captureTips.setText(R.string.capture_tips_take_picture)
        // 点击拍照
        mBinding.recordView.setOnClickListener {
            takePicture()
        }
        // 长按录制视频
        mBinding.recordView.setOnLongClickListener {
            captureVideo()
            true
        }
        mVideoCapture?.run {
            mBinding.captureTips.setText(R.string.capture_tips)
            mBinding.recordView.setOnTouchListener { _, event ->
                // 手指抬起时停止录制
                if (event.action == MotionEvent.ACTION_UP && mVideoRecording?.isClosed == false) {
                    mVideoRecording?.stop()
                }
                false
            }
        }
        // 预览画面点击聚焦
        mBinding.previewView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                // 创建聚焦点
                val meteringPointFactory = mBinding.previewView.meteringPointFactory
                val point = meteringPointFactory.createPoint(event.x, event.y)
                val focusAction =
                    FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF).build()
                // 开启聚焦
                this@CaptureActivity.mCamera?.cameraControl?.startFocusAndMetering(focusAction)
                // 显示聚焦的控件，增强体验
                showFocusPoint(event.x, event.y)
            }
            true
        }
        // 初始化闪光灯按钮状态
        updateFlashButtonUI()
    }

    /**
     * 显示聚焦的控件
     */
    private fun showFocusPoint(x: Float, y: Float) {
        val focusView = mBinding.focusPoint
        val alphaAnim = SpringAnimation(focusView, DynamicAnimation.ALPHA, 1f).apply {
            // stiffness：弹簧刚度（硬度）
            // STIFFNESS_LOW(300f) --> 软弹簧，回弹慢，振荡明显
            // STIFFNESS_MEDIUM(1500f) --> 中等硬度，平衡回弹速度与振荡
            // STIFFNESS_HIGH(3000f) --> 硬弹簧，回弹快，几乎无振荡
            spring.stiffness = SPRING_STIFFNESS_ALPHA_OUT
            // dampingRatio：阻尼比（振荡衰减系数）
            // 0 < dampingRatio < 1：欠阻尼（Under-damped），弹簧会振荡衰减（多次回弹后停止），值越小振荡越剧烈
            // dampingRatio = 1：临界阻尼（Critically-damped），无振荡，以最快速度平稳停止（无回弹）。
            // dampingRatio > 1：过阻尼（Over-damped），无振荡，缓慢停止（比临界阻尼更慢）。
            spring.dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
            addEndListener { _, _, _, _ ->
                SpringAnimation(focusView, DynamicAnimation.ALPHA, 0f).apply {
                    spring.stiffness = SPRING_STIFFNESS_ALPHA_OUT
                    spring.dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
                }.start()
            }
        }

        val scaleXAnim = SpringAnimation(focusView, DynamicAnimation.SCALE_X, 1f).apply {
            spring.stiffness = SPRING_STIFFNESS
            spring.dampingRatio = SPRING_DAMPING_RATIO
        }

        val scaleYAnim = SpringAnimation(focusView, DynamicAnimation.SCALE_Y, 1f).apply {
            spring.stiffness = SPRING_STIFFNESS
            spring.dampingRatio = SPRING_DAMPING_RATIO
        }

        focusView.bringToFront()
        focusView.isVisible = true
        focusView.translationX = x - focusView.width / 2
        focusView.translationY = y - focusView.width / 2
        focusView.alpha = 0f
        focusView.scaleX = 1.5f
        focusView.scaleY = 1.5f

        alphaAnim.start()
        scaleXAnim.start()
        scaleYAnim.start()
    }

    /**
     *  拍照
     */
    private fun takePicture() {
        // 开启震动
        val vibrator = getSystemService(Vibrator::class.java) as Vibrator
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION") vibrator.vibrate(200)
            }
        }
        val fileName = SimpleDateFormat(FILENAME, Locale.US).format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)  // 图片的名称
            put(MediaStore.MediaColumns.MIME_TYPE, PHOTO_TYPE)   // 图片的类型
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH_PICTURE
                ) // 实现图片保存到公共 Pictures 目录,替代绝对路径实现 Scoped Storage 兼容
            }
        }

        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
        ).build()

        mImageCapture?.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri ?: return
                    onFileSaved(savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(applicationContext, exception.message, Toast.LENGTH_SHORT).show()
                }
            })
    }

    /**
     * 录制视频
     */
    private fun captureVideo() {
        // 开启震动
        val vibrator = getSystemService(Vibrator::class.java) as Vibrator
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION") vibrator.vibrate(200)
            }
        }
        // 放大录制按钮，增强视觉体验
        mBinding.recordView.scaleX = 1.2f
        mBinding.recordView.scaleY = 1.2f

        val fileName =
            SimpleDateFormat(FILENAME, Locale.US).format(System.currentTimeMillis()) + ".mp4"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName) // 视频的名称
            put(MediaStore.MediaColumns.MIME_TYPE, VIDEO_TYPE)  // 视频的类型
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH_VIDEO
                ) // 实现图片保存到公共 Movies 目录,替代绝对路径实现 Scoped Storage 兼容
            }
        }

        val outputOptions = MediaStoreOutputOptions.Builder(
            contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).setDurationLimitMillis(11_000) // 录制视频的的最大限制
            .build()
        mVideoRecording = mVideoCapture?.output?.prepareRecording(this, outputOptions).apply {
            if (PermissionChecker.checkSelfPermission(
                    this@CaptureActivity, Manifest.permission.RECORD_AUDIO
                ) == PermissionChecker.PERMISSION_GRANTED
            ) {
                this?.withAudioEnabled()
            }
        }?.start(ContextCompat.getMainExecutor(this)) {
            when (it) {
                // 开始视频录制
                is VideoRecordEvent.Start -> {
                    mBinding.captureTips.setText(R.string.capture_tips_stop_recording)
                }

                is VideoRecordEvent.Status -> {
                    // 录制中，录制时长，文件体积等信息
                    val recordedMills =
                        TimeUnit.NANOSECONDS.toMillis(it.recordingStats.recordedDurationNanos)
                    val progress = (recordedMills * 1.0f / (10 * 1000) * 100).roundToInt()
                    mBinding.recordView.progress = progress
                }
                // 录制结束
                is VideoRecordEvent.Finalize -> {
                    mBinding.captureTips.setText(R.string.capture_tips)
                    // 录制视频成功
                    if (!it.hasError()) {
                        val saveUri = it.outputResults.outputUri
                        onFileSaved(saveUri)
                    } else {
                        // 录制视频失败
                        mVideoRecording?.close()
                        mVideoRecording = null
                    }
                    // 恢复录制按钮的状态
                    mBinding.recordView.scaleX = 1.0f
                    mBinding.recordView.scaleY = 1.0f
                    mBinding.recordView.progress = 0
                    mBinding.captureTips.setText(R.string.capture_tips)
                }
            }
        }
    }

    /**
     *  是否支持组合模式
     */
    @OptIn(ExperimentalCamera2Interop::class)
    private fun isSupportCombinedUsages(
        cameraSelector: CameraSelector, cameraProvider: CameraProvider
    ): Boolean {
        val level = cameraSelector.filter(cameraProvider.availableCameraInfos).firstOrNull()
            ?.let { Camera2CameraInfo.from(it) }
            ?.getCameraCharacteristic(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
            ?: return false
        return level >= CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
    }

    /**
     * 跳转图片/视频预览页面
     */
    @OptIn(UnstableApi::class)
    private fun onFileSaved(savedUri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cursor = contentResolver.query(
                    savedUri,
                    arrayOf(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.MIME_TYPE),
                    null,
                    null,
                    null
                ) ?: return@launch
                // 先尝试获取出录制图片/视频本地路径
                var outputFilePath: String? = null
                var outputFileMimeType: String? = null
                var isVideo = false
                cursor.use {
                    if (cursor.moveToFirst()) {
                        val dataIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                        if (dataIndex >= 0) {
                            val path = cursor.getString(dataIndex)
                            if (!path.isNullOrEmpty() && File(path).exists()) {
                                outputFilePath = path
                            }
                        }
                        // 取出录制图片/视频的文件类型
                        outputFileMimeType =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE))
                        isVideo = MimeTypes.isVideo(outputFileMimeType)
                    }
                }
                // 如果无法获取路径（Android 11 等场景），则复制到应用私有目录
                if (outputFilePath == null) {
                    outputFilePath = copyUriToFile(savedUri, isVideo)
                }
                if (outputFilePath == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            applicationContext, R.string.file_deal_error, Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }
                // 主动扫描一次相册（仅对原始文件路径，复制的文件不需要）
                if (!outputFilePath.startsWith(cacheDir.absolutePath)) {
                    MediaScannerConnection.scanFile(
                        this@CaptureActivity,
                        arrayOf(outputFilePath),
                        arrayOf(outputFileMimeType),
                        null
                    )
                }

                withContext(Dispatchers.Main) {
                    PreviewActivity.start(this@CaptureActivity, outputFilePath, isVideo)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        applicationContext,
                        "${getString(R.string.file_deal_error)}:${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // 复制 URI 内容到应用私有目录的工具方法
    private suspend fun copyUriToFile(uri: Uri, isVideo: Boolean): String? =
        withContext(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri) ?: return@withContext null
                // 生成文件名
                val extension = if (isVideo) ".mp4" else ".jpg"
                val fileName = "temp_${System.currentTimeMillis()}$extension"

                // 创建输出目录
                val outputDir = if (isVideo) {
                    File(cacheDir, "videos").apply { mkdirs() }
                } else {
                    File(cacheDir, "images").apply { mkdirs() }
                }
                val outputFile = File(outputDir, fileName)
                // 复制文件
                outputFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
                inputStream.close()
                outputFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    companion object {
        // 动态权限申请
        private val PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
        ).apply {
            // 注意：Build.VERSION_CODES.P 以下，还是需要 Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        // spring 动画参数配置
        private const val SPRING_STIFFNESS_ALPHA_OUT = 100f
        private const val SPRING_STIFFNESS = 800f
        private const val SPRING_DAMPING_RATIO = 0.35f

        // 图片/视频文件名称，存放位置
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-sss"
        private const val PHOTO_TYPE = "image/jpeg"
        private const val VIDEO_TYPE = "video/mp4"
        private const val RELATIVE_PATH_PICTURE = "Pictures/Jetpack"
        private const val RELATIVE_PATH_VIDEO = "Movies/Jetpack"
    }
}