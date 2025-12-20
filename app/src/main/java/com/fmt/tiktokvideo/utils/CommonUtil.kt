package com.fmt.tiktokvideo.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import androidx.core.net.toUri

/**
 *  用户点击不在询问后，跳转至设置页面
 */
fun Context.goToSettings() {
    Intent(ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri()).apply {
        this.addCategory(Intent.CATEGORY_DEFAULT)
        this.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }.also { intent ->
        startActivity(intent)
    }
}