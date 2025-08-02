package com.iss.api

import com.iss.model.BaseResponse
import com.iss.model.PageResponse
import com.iss.model.Property
import com.iss.model.PropertyImage
import com.iss.model.PropertyRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Body
import retrofit2.http.Part
import retrofit2.http.Query

interface PropertyApi {
    @GET("property/list")
    suspend fun getPropertyList(): Response<List<Property>>

    @GET("property/list")
    suspend fun getPropertyListWrapped(): Response<BaseResponse<List<Property>>>

    @GET("property/list/all")
    suspend fun getPropertyListAll(): Response<BaseResponse<List<Property>>>

    @GET("property/list")
    suspend fun getPropertyListPaged(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Response<BaseResponse<PageResponse<Property>>>

    @GET("property/{id}")
    suspend fun getPropertyById(@Path("id") id: Long): Response<BaseResponse<Property>>

    @POST("property")
    suspend fun createProperty(@Body requestBody: RequestBody): Response<BaseResponse<Property>>
    
    @GET("property/user/{sellerId}")
    suspend fun getUserProperties(@Path("sellerId") sellerId: Long): Response<BaseResponse<List<Property>>>
    
    @DELETE("property/{id}")
    suspend fun deleteProperty(@Path("id") id: Long): Response<BaseResponse<Boolean>>
    
    @PUT("property/{id}")
    suspend fun updateProperty(@Path("id") id: Long, @Body propertyRequest: PropertyRequest): Response<BaseResponse<Property>>
    
    @GET("property/{id}/images")
    suspend fun getPropertyImages(@Path("id") propertyId: Long): Response<BaseResponse<List<PropertyImage>>>
    
    @POST("property/{propertyId}/images")
    suspend fun addPropertyImage(
        @Path("propertyId") propertyId: Long,
        @Body requestBody: RequestBody
    ): Response<BaseResponse<PropertyImage>>
    
    @DELETE("property/images/{imageId}")
    suspend fun deletePropertyImage(@Path("imageId") imageId: Long): Response<BaseResponse<Boolean>>
}