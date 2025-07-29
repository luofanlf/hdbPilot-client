package com.iss.network

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.iss.api.PropertyApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

object NetworkService {
    // 模拟器使用10.0.2.2访问宿主机，真机需要使用实际IP地址
    private const val BASE_URL = "http://10.0.2.2:8080/"
    
    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, JsonDeserializer { json, _, _ ->
            try {
                LocalDateTime.parse(json.asString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            } catch (e: Exception) {
                // 如果解析失败，返回当前时间
                LocalDateTime.now()
            }
        })
        .setLenient()
        .create()
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    
    val propertyApi: PropertyApi = retrofit.create(PropertyApi::class.java)
} 