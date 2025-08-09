package com.iss.api

import com.iss.model.BaseResponse
import com.iss.model.Favorite
import com.iss.model.FavoriteRequest
import com.iss.model.PageResponse
import retrofit2.Response
import retrofit2.http.*

interface FavoriteApi {
    @POST("favorite")
    suspend fun addFavorite(
        @Query("userId") userId: Long,
        @Body request: FavoriteRequest
    ): Response<BaseResponse<Favorite>>

    @DELETE("favorite/{favoriteId}")
    suspend fun removeFavorite(
        @Path("favoriteId") favoriteId: Long,
        @Query("userId") userId: Long
    ): Response<BaseResponse<Boolean>>

    @DELETE("favorite/property/{propertyId}")
    suspend fun removeFavoriteByPropertyId(
        @Path("propertyId") propertyId: Long,
        @Query("userId") userId: Long
    ): Response<BaseResponse<Boolean>>

    @GET("favorite/user/{userId}")
    suspend fun getUserFavorites(
        @Path("userId") userId: Long,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Response<BaseResponse<PageResponse<Favorite>>>

    @GET("favorite/check")
    suspend fun isFavorite(
        @Query("userId") userId: Long,
        @Query("propertyId") propertyId: Long
    ): Response<BaseResponse<Favorite>>

    @GET("favorite/user/{userId}/property-ids")
    suspend fun getUserFavoritePropertyIds(
        @Path("userId") userId: Long
    ): Response<BaseResponse<List<Long>>>
} 