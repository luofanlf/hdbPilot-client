package com.iss.util

import android.content.Context

object SessionManager {
    fun getCurrentUserId(context: Context): Long {
        val prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        return prefs.getLong("user_id", -1L)
    }
}
