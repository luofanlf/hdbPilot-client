package com.iss.api

import com.iss.model.BaseResponse // 导入 BaseResponse
import com.iss.model.LoginRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("user/login") // 修正为后端实际路径: /api/user/login (假设BASE_URL是 http://ip:port/api/)
    suspend fun loginUser(@Body request: LoginRequest): Response<BaseResponse<Long>> // 响应类型为 BaseResponse<Long>
}