package com.iss.model

data class User(
    val id: Long,
    val username: String,
    val nickname: String?, // 可能是空的
    val email: String?,    // 可能是空的
    val bio: String?,      // 可能是空的
    val role: String
)