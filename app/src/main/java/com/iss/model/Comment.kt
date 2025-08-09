package com.iss.model

import com.google.gson.annotations.SerializedName

data class Comment private constructor(
    val id: Long?,
    val content: String,
    val rating: Int,
    val createdAt: String?,
    val propertyId: Long,
    val userId: Long // 新增
)

data class CommentWithUsername(
    val id: Long,
    val propertyId: Long,
    val username: String,
    val rating: Int,
    val content: String,
    @SerializedName("createdAt") val createdAt: String // 或 LocalDateTime，如果你有解析器
)


