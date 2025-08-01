package com.iss

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
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
}