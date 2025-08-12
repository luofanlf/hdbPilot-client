package com.iss.model

import com.google.android.gms.maps.model.LatLng
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
    val forecastPrice: Float? = null, // 设为可选，因为后端可能不返回
    val status: String,
    val sellerName: String? = null, // 新增字段：卖家用户名
    val sellerEmail: String? = null, // 新增字段：卖家邮箱
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val imageUrl: String? = null,
    val imageList: List<PropertyImage>? = null, // 更新为PropertyImage列表

    //for map
    @Transient
    var latLng: LatLng? = null,

    // --- 新增的用于预测模型的特征字段 ---
    val flatType: String? = null, // flat_type 字段，设为可选
    val storeyRange: String? = null, // storey_range 字段，设为可选
    val remainingLease: String? = null, // remaining_lease 字段，设为可选
    val month: String? = null, // month 字段，设为可选
    val leaseCommenceDate: Int? = null // lease_commence_date 字段，设为可选
    // --- 新增字段结束 ---
) {
    val fullAddress: String
        get() = "$block $streetName, $town $postalCode"

    val formattedResalePrice: String
        get() = "$${String.format("%,.0f", resalePrice)}"

    val formattedForecastPrice: String
        get() = forecastPrice?.let { "$${String.format("%,.0f", it)}" } ?: "N/A"

    val formattedArea: String
        get() = "${floorAreaSqm.toInt()}㎡"

    val floorInfo: String
        get() = "Level $storey" // 这个'storey'是您XML中显示的，与'storeyRange'不同，请注意区分
    
    // 获取第一张图片的URL
    val firstImageUrl: String?
        get() = imageList?.firstOrNull()?.imageUrl
}