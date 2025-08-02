package com.iss.api

import com.iss.model.BaseResponse
import com.iss.model.PageResponse
import com.iss.model.Property
import com.iss.model.PropertyRequest
import com.iss.model.PropertySearchRequest
import com.iss.model.PropertyImage
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

    @GET("property/search")
    suspend fun searchProperties(
        @Query("listingTitle") listingTitle: String? = null,
        @Query("postalCode") postalCode: String? = null,
        @Query("bedroomNumberMin") bedroomNumberMin: Int? = null,
        @Query("bedroomNumberMax") bedroomNumberMax: Int? = null,
        @Query("bathroomNumberMin") bathroomNumberMin: Int? = null,
        @Query("bathroomNumberMax") bathroomNumberMax: Int? = null,
        @Query("storeyMin") storeyMin: String? = null,
        @Query("storeyMax") storeyMax: String? = null,
        @Query("floorAreaSqmMin") floorAreaSqmMin: Float? = null,
        @Query("floorAreaSqmMax") floorAreaSqmMax: Float? = null,
        @Query("topYearMin") topYearMin: Int? = null,
        @Query("topYearMax") topYearMax: Int? = null,
        @Query("resalePriceMin") resalePriceMin: Float? = null,
        @Query("resalePriceMax") resalePriceMax: Float? = null,
        @Query("town") town: String? = null,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Response<BaseResponse<PageResponse<Property>>>

    @GET("property/{id}")
    suspend fun getPropertyById(@Path("id") id: Long): Response<BaseResponse<Property>>

    @POST("property")
    suspend fun createProperty(@Body propertyRequest: PropertyRequest): Response<BaseResponse<Property>>
    
    @POST("property")
    suspend fun createPropertyWithImages(@Body multipartBody: MultipartBody): Response<BaseResponse<Property>>
    
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
