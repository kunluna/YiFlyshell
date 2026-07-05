package com.yishell.app

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class YiShellApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 全局未捕获异常兜底 — 记录日志后交给系统默认处理
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("YiShell", "=== Uncaught Exception ===", throwable)
            Log.e("YiShell", "Thread: ${thread.name}")
            Log.e("YiShell", "Exception: ${throwable.javaClass.simpleName}: ${throwable.message}")

            // 交给系统默认处理（显示 crash dialog / 写入 dropbox）
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
