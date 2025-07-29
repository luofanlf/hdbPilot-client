package com.iss.model

import java.time.LocalDateTime

data class Property(
    val id: Long,
    val listingTitle: String,
    val sellerId: Long,
    val town: String,
    val postalCode: String,
    val bedroomNumber: Int,
    val bathroomNumber: Int,
    val block: String,
    val streetName: String,
    val storey: String,
    val floorAreaSqm: Float,
    val topYear: Int,
    val flatModel: String,
    val resalePrice: Float,
    val forecastPrice: Float,
    val status: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val imageUrl: String? = null
) {
    // 计算属性：完整地址
    val fullAddress: String
        get() = "$block $streetName, $town $postalCode"
    
    // 计算属性：格式化价格
    val formattedResalePrice: String
        get() = "$${String.format("%,.0f", resalePrice)}"
    
    val formattedForecastPrice: String
        get() = "$${String.format("%,.0f", forecastPrice)}"
    
    // 计算属性：格式化面积
    val formattedArea: String
        get() = "${floorAreaSqm.toInt()}㎡"
    
    // 计算属性：楼层信息
    val floorInfo: String
        get() = "Level $storey"
} 