package com.iss.api

import com.iss.model.BaseResponse
import com.iss.model.LoginRequest
import com.iss.model.UserRegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("user/login")
    suspend fun loginUser(@Body request: LoginRequest): Response<BaseResponse<Long>>

    @POST("user/register")
    suspend fun registerUser(@Body request: UserRegisterRequest): Response<BaseResponse<Long>>
}