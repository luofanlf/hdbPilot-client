package com.iss

import android.app.Application
import com.iss.network.initialize
import com.iss.utils.UserManager

class initApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initialize(this) // 在应用启动时初始化网络服务
        UserManager.init(this) // 初始化用户管理器
    }
}