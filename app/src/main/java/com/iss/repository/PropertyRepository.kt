package com.iss.repository

import android.util.Log
import com.iss.model.PageResponse
import com.iss.model.Property
import com.iss.model.PropertySearchRequest
import com.iss.network.NetworkService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PropertyRepository {
    private val propertyApi = NetworkService.propertyApi
    
    suspend fun getPropertyList(): Result<List<Property>> = withContext(Dispatchers.IO) {
        try {
            Log.d("PropertyRepository", "Making API call...")

            // 尝试新的API端点
            try {
                Log.d("PropertyRepository", "Trying new API endpoint...")
                val response = propertyApi.getPropertyListAll()
                Log.d("PropertyRepository", "New API - Response code: ${response.code()}")
                
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    Log.d("PropertyRepository", "New API - Response body: $apiResponse")
                    
                    if (apiResponse != null && apiResponse.data != null) {
                        Log.d("PropertyRepository", "New API - Data size: ${apiResponse.data.size}")
                        return@withContext Result.success(apiResponse.data)
                    } else {
                        Log.e("PropertyRepository", "New API - Response or data is null")
                        return@withContext Result.failure(Exception("No data received"))
                    }
                } else {
                    Log.e("PropertyRepository", "New API - API request failed: ${response.code()} - ${response.message()}")
                    return@withContext Result.failure(Exception("API request failed: ${response.code()} - ${response.message()}"))
                }
                
            } catch (e: Exception) {
                Log.e("PropertyRepository", "New API failed", e)
                return@withContext Result.failure(e)
            }
            
        } catch (e: Exception) {
            Log.e("PropertyRepository", "Exception in getPropertyList", e)
            Result.failure(e)
        }
    }

    suspend fun getPropertyListPaged(pageNum: Int = 1, pageSize: Int = 10): Result<PageResponse<Property>> = withContext(Dispatchers.IO) {
        try {
            Log.d("PropertyRepository", "Making paged API call... pageNum: $pageNum, pageSize: $pageSize")
            val response = propertyApi.getPropertyListPaged(pageNum, pageSize)
            Log.d("PropertyRepository", "Paged API - Response code: ${response.code()}")
            
            if (response.isSuccessful) {
                val apiResponse = response.body()
                Log.d("PropertyRepository", "Paged API - Response body: $apiResponse")
                
                if (apiResponse != null && apiResponse.data != null) {
                    Log.d("PropertyRepository", "Paged API - Success: ${apiResponse.data.records.size} records, total: ${apiResponse.data.total}")
                    Result.success(apiResponse.data)
                } else {
                    Log.e("PropertyRepository", "Paged API - Response or data is null")
                    Result.failure(Exception("No data received"))
                }
            } else {
                Log.e("PropertyRepository", "Paged API - API request failed: ${response.code()} - ${response.message()}")
                Result.failure(Exception("API request failed: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("PropertyRepository", "Exception in getPropertyListPaged", e)
            Result.failure(e)
        }
    }

    suspend fun searchProperties(searchRequest: PropertySearchRequest): Result<PageResponse<Property>> = withContext(Dispatchers.IO) {
        try {
            Log.d("PropertyRepository", "Making search API call... request: $searchRequest")
            val response = propertyApi.searchProperties(
                listingTitle = searchRequest.listingTitle,
                postalCode = searchRequest.postalCode,
                bedroomNumberMin = searchRequest.bedroomNumberMin,
                bedroomNumberMax = searchRequest.bedroomNumberMax,
                bathroomNumberMin = searchRequest.bathroomNumberMin,
                bathroomNumberMax = searchRequest.bathroomNumberMax,
                storeyMin = searchRequest.storeyMin,
                storeyMax = searchRequest.storeyMax,
                floorAreaSqmMin = searchRequest.floorAreaSqmMin,
                floorAreaSqmMax = searchRequest.floorAreaSqmMax,
                topYearMin = searchRequest.topYearMin,
                topYearMax = searchRequest.topYearMax,
                resalePriceMin = searchRequest.resalePriceMin,
                resalePriceMax = searchRequest.resalePriceMax,
                town = searchRequest.town,
                pageNum = searchRequest.pageNum,
                pageSize = searchRequest.pageSize
            )
            Log.d("PropertyRepository", "Search API - Response code: ${response.code()}")
            
            if (response.isSuccessful) {
                val apiResponse = response.body()
                Log.d("PropertyRepository", "Search API - Response body: $apiResponse")
                
                if (apiResponse != null && apiResponse.data != null) {
                    Log.d("PropertyRepository", "Search API - Success: ${apiResponse.data.records.size} records, total: ${apiResponse.data.total}")
                    Result.success(apiResponse.data)
                } else {
                    Log.e("PropertyRepository", "Search API - Response or data is null")
                    Result.failure(Exception("No data received"))
                }
            } else {
                Log.e("PropertyRepository", "Search API - API request failed: ${response.code()} - ${response.message()}")
                Result.failure(Exception("API request failed: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("PropertyRepository", "Exception in searchProperties", e)
            Result.failure(e)
        }
    }

    suspend fun getPropertyById(id: Long): Result<Property> = withContext(Dispatchers.IO) {
        try {
            Log.d("PropertyRepository", "Making API call for property detail, id: $id")
            val response = propertyApi.getPropertyById(id)
            Log.d("PropertyRepository", "Property detail - Response code: ${response.code()}")
            
            if (response.isSuccessful) {
                val apiResponse = response.body()
                Log.d("PropertyRepository", "Property detail - Response body: $apiResponse")
                
                if (apiResponse != null && apiResponse.data != null) {
                    Log.d("PropertyRepository", "Property detail - Success: ${apiResponse.data.listingTitle}")
                    Result.success(apiResponse.data)
                } else {
                    Log.e("PropertyRepository", "Property detail - Response or data is null")
                    Result.failure(Exception("Property not found"))
                }
            } else {
                Log.e("PropertyRepository", "Property detail - API request failed: ${response.code()} - ${response.message()}")
                Result.failure(Exception("API request failed: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("PropertyRepository", "Exception in getPropertyById", e)
            Result.failure(e)
        }
    }
} 