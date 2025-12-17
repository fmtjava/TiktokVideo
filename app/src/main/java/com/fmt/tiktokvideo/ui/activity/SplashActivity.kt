package com.fmt.tiktokvideo.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.fmt.tiktokvideo.cache.UserManager
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            UserManager.loadCache()
            if (UserManager.isLogin()) {
                MainActivity.start(this@SplashActivity)
            } else {
                LoginActivity.start(this@SplashActivity)
            }
        }
    }
}