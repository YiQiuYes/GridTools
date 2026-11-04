package com.yiqiu.toolkit

import android.app.Application
import com.yiqiu.toolkit.core.di.AppContainer
import com.yiqiu.toolkit.core.di.DefaultAppContainer

/** Single process-wide entry point that owns the dependency graph. */
class ToolKitApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
