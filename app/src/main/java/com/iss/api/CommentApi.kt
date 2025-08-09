package com.iss.api

import com.iss.model.Comment
import com.iss.model.CommentRequest
import com.iss.model.CommentWithUsername
import com.iss.model.BaseResponse
import retrofit2.Response
import retrofit2.http.*

interface CommentApi {
    @GET("/api/comments/property/{propertyId}")
    suspend fun getAllComments(@Path("propertyId") propertyId: Long): Response<List<Comment>>

    @GET("/api/comments/property/{propertyId}/average")
    suspend fun getAverageRating(@Path("propertyId") propertyId: Long): Response<Double>

    @GET("/api/comments/user/{userId}")
    suspend fun getUserComments(@Path("userId") userId: Long): Response<List<Comment>>
    
    @GET("/api/comments/user/current")
    suspend fun getUserComments(): Response<BaseResponse<List<Comment>>>

    @POST("/api/comments")
    suspend fun submitComment(@Body comment: CommentRequest): Response<Map<String, String>>
    
    @DELETE("/api/comments/{commentId}")
    suspend fun deleteComment(@Path("commentId") commentId: Long): Response<Map<String, String>>

    @GET("/api/comments/property/{propertyId}/with-username")
    suspend fun getCommentsWithUsername(@Path("propertyId") propertyId: Long): Response<List<CommentWithUsername>>

}

