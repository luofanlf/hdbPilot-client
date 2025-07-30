package com.iss.api

import com.iss.model.BaseResponse
import com.iss.model.Property
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface PropertyApi {
    @GET("property/list") // <-- 修改这里，移除了 'api/'
    suspend fun getPropertyList(): Response<List<Property>>

    @GET("property/list") // <-- 修改这里，移除了 'api/'
    suspend fun getPropertyListWrapped(): Response<BaseResponse<List<Property>>>

    @GET("property/{id}") // <-- 修改这里，移除了 'api/'
    suspend fun getPropertyById(@Path("id") id: Long): Response<BaseResponse<Property>>
}