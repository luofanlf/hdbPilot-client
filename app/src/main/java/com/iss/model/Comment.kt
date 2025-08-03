package com.iss.model

data class Comment private constructor(
    val id: Long?,
    val content: String,
    val rating: Int,
    val createdAt: String?,
    val propertyId: Long
)

