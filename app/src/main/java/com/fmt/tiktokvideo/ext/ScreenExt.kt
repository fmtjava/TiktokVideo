package com.fmt.tiktokvideo.ext

import android.content.Context
import android.view.WindowManager

fun Context.getScreenW(): Int {
    val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        wm.currentWindowMetrics.bounds.width()
    } else {
        wm.defaultDisplay.width
    }
}

fun Context.getScreenH(): Int {
    val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        wm.currentWindowMetrics.bounds.height()
    } else {
        wm.defaultDisplay.height
    }
}