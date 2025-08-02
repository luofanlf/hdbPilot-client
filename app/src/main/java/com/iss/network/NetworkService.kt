package com.iss.network

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.iss.api.AuthApi
import com.iss.api.PropertyApi
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

private lateinit var applicationContext: Context

fun initialize(context: Context) {
    applicationContext = context.applicationContext
}

object NetworkService {

    private const val BASE_URL = "http://10.0.2.2:8080/api/"

    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, JsonDeserializer { json, _, _ ->
            try {
                LocalDateTime.parse(json.asString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            } catch (e: Exception) {
                LocalDateTime.now()
            }
        })
        .setLenient()
        .create()

    private val cookieJar = object : CookieJar {
        private val sharedPreferences by lazy {
            applicationContext.getSharedPreferences("cookie_prefs", Context.MODE_PRIVATE)
        }

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val editor = sharedPreferences.edit()
            for (cookie in cookies) {
                if (cookie.name == "JSESSIONID") {
                    val cookieString = cookie.toString()
                    editor.putString("jsessionid", cookieString)
                    break
                }
            }
            editor.apply()
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val cookieString = sharedPreferences.getString("jsessionid", null)
            return if (cookieString != null) {
                try {
                    val cookie = Cookie.parse(url, cookieString)
                    if (cookie != null) {
                        listOf(cookie)
                    } else {
                        emptyList()
                    }
                } catch (e: IllegalArgumentException) {
                    sharedPreferences.edit().remove("jsessionid").apply()
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val propertyApi: PropertyApi by lazy { retrofit.create(PropertyApi::class.java) }
    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
}