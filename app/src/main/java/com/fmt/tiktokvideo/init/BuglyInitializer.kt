package com.fmt.tiktokvideo.init

import android.content.Context
import androidx.startup.Initializer
import com.tencent.bugly.crashreport.CrashReport

class BuglyInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        CrashReport.initCrashReport(context, "7f7d621247", false);
    }

    override fun dependencies(): List<Class<out Initializer<*>?>?> {
        return emptyList()
    }
}