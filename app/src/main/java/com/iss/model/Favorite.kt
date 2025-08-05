package com.iss.model

import com.google.gson.annotations.SerializedName

data class Favorite(
    val id: Long,
    val userId: Long,
    val propertyId: Long,
    val property: Property? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
) 