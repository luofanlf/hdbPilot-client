package com.iss.model

import com.google.gson.annotations.SerializedName

// 对应后端的 UserLoginRequest DTO
data class LoginRequest(
    val username: String, // 对应后端的 username 字段
    val password: String
)