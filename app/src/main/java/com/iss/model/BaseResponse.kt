package com.iss.model

import com.google.gson.annotations.SerializedName

// 对应后端的 BaseResponse<T>
data class BaseResponse<T>(
    val code: Int, // 业务状态码，例如 0 表示成功
    val message: String?, // 响应消息
    val description: String?, // 详细描述，可选
    val data: T? // 实际的数据内容
)