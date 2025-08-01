package com.iss.model

data class PageResponse<T>(
    val records: List<T>,
    val total: Long,
    val size: Long,
    val current: Long,
    val pages: Long,
    val orders: List<Any>? = null,
    val optimizeCountSql: Boolean? = null,
    val searchCount: Boolean? = null,
    val countId: String? = null,
    val maxLimit: Long? = null
) {
    val hasNext: Boolean
        get() = current < pages
    
    val hasPrevious: Boolean
        get() = current > 1
} 