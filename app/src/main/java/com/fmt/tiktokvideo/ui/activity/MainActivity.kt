package com.fmt.tiktokvideo.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import cn.jzvd.Jzvd
import com.fmt.tiktokvideo.R
import com.fmt.tiktokvideo.cache.UserManager
import com.fmt.tiktokvideo.cache.encryptedPrefs
import com.fmt.tiktokvideo.dao.CacheManager
import com.fmt.tiktokvideo.databinding.ActivityMainBinding
import com.fmt.tiktokvideo.ext.invokeViewBinding
import com.fmt.tiktokvideo.ext.navigateTo
import com.fmt.tiktokvideo.nav.NavGraphBuilder
import com.fmt.tiktokvideo.nav.Router
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val mBinding: ActivityMainBinding by invokeViewBinding<ActivityMainBinding>()
    private val mSelectTextColor by lazy {
        ContextCompat.getColor(this, R.color.white)
    }
    private val mUnSelectTextColor by lazy {
        ContextCompat.getColor(this, R.color.color_translucent_white)
    }

    private val mNavController by lazy {
        findNavController(R.id.nav_host_fragment)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
        setUpSystemBars()
        val hostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        NavGraphBuilder.build(
            mNavController,
            this,
            hostFragment!!.childFragmentManager,
            R.id.nav_host_fragment
        )
        initListener()
    }

    private fun initListener() {
        mBinding.tvHome.setOnClickListener(this)
        mBinding.ivVideo.setOnClickListener(this)
        mBinding.tvMine.setOnClickListener(this)

        // 设置退出登录监听
        lifecycleScope.launch {
            UserManager.isNeedLoginOut().collectLatest {
                if (it) {
                    withContext(Dispatchers.IO) {
                        encryptedPrefs.edit { clear() }
                        val cacheUser = CacheManager.get().userDao.getUser()
                        cacheUser?.run {
                            CacheManager.get().userDao.delete(cacheUser)
                        }
                    }
                    val intent = Intent(this@MainActivity, LoginActivity::class.java).apply {
                        flags =
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (Jzvd.backPress()) {
                    return
                }
               finish()
            }
        })
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.tv_home -> changeBottomTab(Router.HOME, mSelectTextColor, mUnSelectTextColor)

            R.id.iv_video -> {
                mNavController.navigateTo(Router.CAPTURE)
            }

            R.id.tv_mine -> {
                changeBottomTab(Router.MINE, mUnSelectTextColor, mSelectTextColor)
            }

            else -> {

            }
        }
    }

    /**
     *  底部 Tab 切换
     */
    private fun changeBottomTab(
        router: String,
        @ColorInt homeTxtColor: Int,
        @ColorInt mineTxtColor: Int
    ) {
        mNavController.navigateTo(router)
        mBinding.tvHome.setTextColor(homeTxtColor)
        mBinding.tvMine.setTextColor(mineTxtColor)
    }

    private fun setUpSystemBars() {
        // Android 10+ 关闭系统对导航栏的强制对比度优化，Android 10+ 系统会自动调整导航栏背景色以保证与内容的对比度，可能导致自定义主题失效
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // 设置状态栏图标颜色， true→ 深色图标（适合浅色状态栏背景）false→ 浅色图标（适合深色状态栏背景）
        windowInsetsController.isAppearanceLightStatusBars = false

        // 防止导航栏与内容重叠
        ViewCompat.setOnApplyWindowInsetsListener(mBinding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // 底部系统栏高度（虚拟导航栏高度，无则为0）
            val bottomInset = systemBars.bottom

            mBinding.lyBottom.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                // 调整高度：dimen定义的高度 + 底部插入量
                height = resources.getDimensionPixelSize(R.dimen.bottom_nav_height) + bottomInset
            }
            // 调整内边距：底部内边距设为插入量
            mBinding.lyBottom.updatePadding(bottom = bottomInset)

            insets
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }
    }
}