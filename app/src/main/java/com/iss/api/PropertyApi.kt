package com.iss.api

import com.iss.model.ApiResponse
import com.iss.model.Property
import retrofit2.Response
import retrofit2.http.GET

interface PropertyApi {
    @GET("api/property/list")
    suspend fun getPropertyList(): Response<List<Property>>
    
    @GET("api/property/list")
    suspend fun getPropertyListWrapped(): Response<ApiResponse<List<Property>>>
} 