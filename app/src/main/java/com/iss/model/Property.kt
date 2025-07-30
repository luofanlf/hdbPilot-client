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
    val storey: String, // 现有字段，用于显示楼层信息，但我们还需要 storey_range
    val floorAreaSqm: Float,
    val topYear: Int, // 现有字段，用于显示年份，但我们还需要 lease_commence_date
    val flatModel: String,
    val resalePrice: Float,
    val forecastPrice: Float,
    val status: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val imageUrl: String? = null,

    // --- 新增的用于预测模型的特征字段 ---
    val flatType: String, // flat_type 字段
    val storeyRange: String, // storey_range 字段 (您在XML中也使用了这个)
    val remainingLease: String, // remaining_lease 字段
    val month: String, // month 字段 (例如 "2019-06")
    val leaseCommenceDate: Int // lease_commence_date 字段
    // --- 新增字段结束 ---
) {
    val fullAddress: String
        get() = "$block $streetName, $town $postalCode"

    val formattedResalePrice: String
        get() = "$${String.format("%,.0f", resalePrice)}"

    val formattedForecastPrice: String
        get() = "$${String.format("%,.0f", forecastPrice)}"

    val formattedArea: String
        get() = "${floorAreaSqm.toInt()}㎡"

    val floorInfo: String
        get() = "Level $storey" // 这个'storey'是您XML中显示的，与'storeyRange'不同，请注意区分
}