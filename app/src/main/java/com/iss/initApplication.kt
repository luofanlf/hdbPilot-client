package com.iss

import android.app.Application
import com.iss.network.initialize

class initApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initialize(this) // 在应用启动时初始化网络服务
    }
}