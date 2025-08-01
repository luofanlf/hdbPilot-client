package com.iss.model

import com.google.gson.annotations.SerializedName

data class PropertyImage(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("propertyId")
    val propertyId: Long,
    
    @SerializedName("imageUrl")
    val imageUrl: String,
    
    @SerializedName("createdAt")
    val createdAt: String?,
    
    @SerializedName("updatedAt")
    val updatedAt: String?
) 