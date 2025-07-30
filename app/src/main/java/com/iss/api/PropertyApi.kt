package com.iss.api

import com.iss.model.BaseResponse
import com.iss.model.Property
import com.iss.model.PropertyRequest
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Body

interface PropertyApi {
    @GET("property/list") // <-- 修改这里，移除了 'api/'
    suspend fun getPropertyList(): Response<List<Property>>

    @GET("property/list") // <-- 修改这里，移除了 'api/'
    suspend fun getPropertyListWrapped(): Response<BaseResponse<List<Property>>>

    @GET("property/{id}") // <-- 修改这里，移除了 'api/'
    suspend fun getPropertyById(@Path("id") id: Long): Response<BaseResponse<Property>>

    @POST("property") // <-- 添加创建房源接口
    suspend fun createProperty(@Body propertyRequest: PropertyRequest): Response<BaseResponse<Property>>
    
    @GET("property/user/{sellerId}")
    suspend fun getUserProperties(@Path("sellerId") sellerId: Long): Response<BaseResponse<List<Property>>>
    
    @DELETE("property/{id}")
    suspend fun deleteProperty(@Path("id") id: Long): Response<BaseResponse<Boolean>>
    
    @PUT("property/{id}")
    suspend fun updateProperty(@Path("id") id: Long, @Body propertyRequest: PropertyRequest): Response<BaseResponse<Property>>
}