package com.iss.api

import com.iss.model.BaseResponse
import com.iss.model.LoginRequest
import com.iss.model.User
import com.iss.model.UserRegisterRequest
import com.iss.model.UserUpdateRequest
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface AuthApi {
    @POST("user/login")
    suspend fun loginUser(@Body request: LoginRequest): Response<BaseResponse<Long>>

    @POST("user/register")
    suspend fun registerUser(@Body request: UserRegisterRequest): Response<BaseResponse<Long>>

    @POST("user/update_profile")
    suspend fun updateUserProfile(@Body request: UserUpdateRequest): Response<BaseResponse<Boolean>>

    @POST("user/logout")
    suspend fun logout(): Response<BaseResponse<Boolean>>

    // 获取当前登录用户资料
    @GET("user/profile")
    suspend fun getUserProfile(): Response<BaseResponse<User>>

    // 通过用户ID获取资料（用于确保拿到DB最新avatarUrl）
    @GET("user/{userId}")
    suspend fun getUserById(@Path("userId") userId: Long): Response<BaseResponse<User>>

    // 上传用户头像
    @Multipart
    @PUT("user/{userId}/avatar")
    suspend fun updateAvatar(
        @Path("userId") userId: Long,
        @Part imageFile: MultipartBody.Part
    ): Response<BaseResponse<String>>
}