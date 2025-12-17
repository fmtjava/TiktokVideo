package com.fmt.tiktokvideo

import android.app.Application
import android.content.ContextWrapper

internal lateinit var mApp: Application

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        mApp = this
    }
}

object AppContext : ContextWrapper(mApp)