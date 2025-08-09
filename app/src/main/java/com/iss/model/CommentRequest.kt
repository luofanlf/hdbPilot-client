package com.iss.model

data class CommentRequest(
    val content: String,
    val rating: Int,
    val propertyId: Long,
    val userId: Long
)
