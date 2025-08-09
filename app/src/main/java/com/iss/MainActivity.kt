package com.iss

import android.content.Intent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import android.view.MenuItem
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.appbar.MaterialToolbar // 导入 MaterialToolbar
import com.iss.ui.activities.UserProfileFragment // 导入你创建的 UserProfileFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 设置Navigation Controller与底部导航栏的连接
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // 设置默认的导航行为
        bottomNav.setupWithNavController(navController)

        // =================================================================
        // 新增代码：获取 MaterialToolbar 并设置菜单项点击监听器
        // =================================================================
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        // 刷新右上角头像
        refreshUserAvatarIcon()
        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_user_profile -> {
                    // 当点击用户头像时，导航到 UserProfileFragment
                    navController.navigate(R.id.userProfileFragment)
                    true
                }
                else -> false
            }
        }

        // 添加选择监听器来处理所有Home按钮的点击
        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.homeFragment -> {
                    Log.d("MainActivity", "Home按钮被点击")

                    val currentDestination = navController.currentDestination
                    Log.d("MainActivity", "当前导航目标: ${currentDestination?.id}")

                    // 如果当前不在HomeFragment，清除导航栈并导航到HomeFragment
                    if (currentDestination?.id != R.id.homeFragment) {
                        Log.d("MainActivity", "当前不在HomeFragment，清除导航栈")
                        navController.popBackStack(R.id.homeFragment, true)
                        navController.navigate(R.id.homeFragment)
                    }
                    true
                }
                R.id.myActivitiesFragment -> {
                    Log.d("MainActivity", "My Activities按钮被点击")
                    navController.navigate(R.id.myActivitiesFragment)
                    true
                }
                R.id.moreFragment -> {
                    Log.d("MainActivity", "More按钮被点击")
                    navController.navigate(R.id.moreFragment)
                    true
                }
                R.id.mapFragment -> {
                    Log.d("MainActivity", "Map按钮被点击")
                    navController.navigate(R.id.mapFragment)
                    true
                }
                else -> {
                    // 其他导航项使用默认行为
                    false
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 返回主界面或从其他页面返回时，刷新头像
        refreshUserAvatarIcon()
    }

    fun refreshUserAvatarIcon() {
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        val menuItem: MenuItem = topAppBar.menu.findItem(R.id.action_user_profile) ?: return
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.reloadIfPossible()
        val avatarUrl = prefs.getString("avatar_url", null)

        if (avatarUrl.isNullOrBlank()) {
            menuItem.setIcon(R.drawable.ic_activities)
            return
        }

        val sizePx = (32f * resources.displayMetrics.density).toInt()
        Glide.with(this)
            .asBitmap()
            .load(avatarUrl)
            .circleCrop()
            .signature(com.bumptech.glide.signature.ObjectKey(System.currentTimeMillis()))
            .into(object : CustomTarget<Bitmap>(sizePx, sizePx) {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val drawable = BitmapDrawable(resources, resource)
                    menuItem.icon = drawable
                    menuItem.icon?.setTintList(null) // 取消统一着色，保留原图
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // 无需处理
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    menuItem.setIcon(R.drawable.ic_activities)
                }
            })
    }
}

private fun Context.getDefaultSharedPreferencesName(): String = "user_prefs"

private fun android.content.SharedPreferences.reloadIfPossible() {
    // 对于 MODE_PRIVATE 的 SP，apply 已经是异步写入磁盘；这里无强制刷新 API。
    // 此方法保留占位，便于将来接入 DataStore 或多进程场景。
}