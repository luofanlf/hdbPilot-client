package com.iss.model

// 用于包装API响应的通用类
data class ApiResponse<T>(
    val code: Int? = null,
    val message: String? = null,
    val data: T? = null
) 