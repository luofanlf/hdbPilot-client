package com.iss.model

import com.google.gson.annotations.SerializedName

data class PropertyRequest(
    @SerializedName("listingTitle")
    val listingTitle: String,
    
    @SerializedName("sellerId")
    val sellerId: Long,
    
    @SerializedName("town")
    val town: String,
    
    @SerializedName("postalCode")
    val postalCode: String,
    
    @SerializedName("bedroomNumber")
    val bedroomNumber: Int,
    
    @SerializedName("bathroomNumber")
    val bathroomNumber: Int,
    
    @SerializedName("block")
    val block: String,
    
    @SerializedName("streetName")
    val streetName: String,
    
    @SerializedName("storey")
    val storey: String,
    
    @SerializedName("floorAreaSqm")
    val floorAreaSqm: Float,
    
    @SerializedName("topYear")
    val topYear: Int,
    
    @SerializedName("flatModel")
    val flatModel: String,
    
    @SerializedName("resalePrice")
    val resalePrice: Float,
    
    @SerializedName("status")
    val status: String = "available"
) 