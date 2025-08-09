package com.iss.model

data class Comment private constructor(
    val id: Long?,
    val content: String,
    val rating: Int,
    val createdAt: String?,
    val propertyId: Long,
    val userId: Long
) {
    companion object {
        fun create(
            id: Long? = null,
            content: String,
            rating: Int,
            createdAt: String? = null,
            propertyId: Long,
            userId: Long
        ): Comment {
            return Comment(id, content, rating, createdAt, propertyId, userId)
        }
    }
}

