package com.iss.network

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.iss.api.AuthApi
import com.iss.api.PropertyApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

object NetworkService {
    // IMPORTANT: Make sure BASE_URL matches your Spring Boot backend's root path for APIs.
    // It should include the context path (e.g., /api/) if your controllers are mapped under it.
    private const val BASE_URL = "http://10.0.2.2:8080/api/" // <-- 关键修改：加上了 /api/

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

    val authApi: AuthApi = retrofit.create(AuthApi::class.java)
}