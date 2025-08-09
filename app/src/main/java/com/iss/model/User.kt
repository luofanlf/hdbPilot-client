package com.iss.model

import java.time.LocalDateTime
import com.google.gson.annotations.SerializedName

data class User(
    val id: Long,
    val username: String,
    val nickname: String?, // 可能是空的
    val email: String?,    // 可能是空的
    val bio: String?,      // 可能是空的
    val role: String,
    @SerializedName(value = "avatarUrl", alternate = ["avatar_url"]) val avatarUrl: String?, // 兼容后端不同命名
    @SerializedName(value = "createdAt", alternate = ["created_at"]) val createdAt: LocalDateTime? // 兼容后端不同命名
)