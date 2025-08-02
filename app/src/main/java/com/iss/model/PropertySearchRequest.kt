package com.iss.model

data class PropertySearchRequest(
    val listingTitle: String? = null,
    val postalCode: String? = null,
    val bedroomNumberMin: Int? = null,
    val bedroomNumberMax: Int? = null,
    val bathroomNumberMin: Int? = null,
    val bathroomNumberMax: Int? = null,
    val storeyMin: String? = null,
    val storeyMax: String? = null,
    val floorAreaSqmMin: Float? = null,
    val floorAreaSqmMax: Float? = null,
    val topYearMin: Int? = null,
    val topYearMax: Int? = null,
    val resalePriceMin: Float? = null,
    val resalePriceMax: Float? = null,
    val town: String? = null,
    val pageNum: Int = 1,
    val pageSize: Int = 10
) 