package com.iss.utils

import android.content.Context
import android.content.SharedPreferences

object UserManager {
    private const val PREF_NAME = "user_prefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    
    private lateinit var sharedPreferences: SharedPreferences
    
    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    fun setCurrentUser(userId: Long, username: String) {
        sharedPreferences.edit().apply {
            putLong(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }
    
    fun getCurrentUserId(): Long {
        // 先尝试读取Long类型
        val longUserId = sharedPreferences.getLong(KEY_USER_ID, -1L)
        if (longUserId != -1L) {
            return longUserId
        }
        
        // 如果Long类型读取失败，尝试读取String类型并转换
        val stringUserId = sharedPreferences.getString(KEY_USER_ID, null)
        return if (stringUserId != null) {
            try {
                stringUserId.toLong()
            } catch (e: NumberFormatException) {
                -1L
            }
        } else {
            -1L
        }
    }
    
    fun getCurrentUsername(): String {
        return sharedPreferences.getString(KEY_USERNAME, "") ?: ""
    }
    
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false) && getCurrentUserId() != -1L
    }
    
    fun logout() {
        sharedPreferences.edit().apply {
            remove(KEY_USER_ID)
            remove(KEY_USERNAME)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
    }
    
    fun clearUserData() {
        sharedPreferences.edit().clear().apply()
    }
} 