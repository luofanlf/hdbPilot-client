package com.iss.network

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class PersistentCookieJar(context: Context) : CookieJar {

    private val sharedPreferences = context.getSharedPreferences("cookie_prefs", Context.MODE_PRIVATE)

    // 存储从响应中收到的所有Cookie
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val editor = sharedPreferences.edit()
        for (cookie in cookies) {
            // 只保存JSESSIONID，因为我们只关心会话
            if (cookie.name == "JSESSIONID") {
                val cookieString = cookie.toString()
                editor.putString("jsessionid", cookieString)
                break
            }
        }
        editor.apply()
    }

    // 为请求加载已保存的Cookie
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookieString = sharedPreferences.getString("jsessionid", null)
        return if (cookieString != null) {
            try {
                // 解析Cookie字符串并返回
                val cookie = Cookie.parse(url, cookieString)
                if (cookie != null) {
                    listOf(cookie)
                } else {
                    emptyList()
                }
            } catch (e: IllegalArgumentException) {
                // 如果Cookie字符串格式错误，则清空
                sharedPreferences.edit().remove("jsessionid").apply()
                emptyList()
            }
        } else {
            emptyList()
        }
    }
}