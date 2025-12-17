package com.fmt.tiktokvideo.utils

import android.app.Application

private var mApplication: Application? = null

object AppGlobals {

    fun getApplication(): Application? {
        if (mApplication == null) {
            runCatching {
                mApplication =
                    Class.forName("android.app.ActivityThread").getMethod("currentApplication")
                        .invoke(null, *emptyArray()) as Application
            }.onFailure {
                it.printStackTrace()
            }
        }
        return mApplication
    }
}