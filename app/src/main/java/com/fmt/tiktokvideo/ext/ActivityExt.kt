package com.fmt.tiktokvideo.ext

import android.app.Activity
import android.os.Build
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat

fun Activity.hideKeyboard() {
    val view = currentFocus ?: window.decorView.rootView
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // Android 11+ (API 30+) 推荐使用 WindowInsetsController
        window.insetsController?.hide(WindowInsetsCompat.Type.ime())
    } else {
        // Android 11 以下使用 InputMethodManager
        val imm = ContextCompat.getSystemService(this, InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }
}