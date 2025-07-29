package com.iss.repository

import android.util.Log
import com.iss.model.Property
import com.iss.network.NetworkService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PropertyRepository {
    private val propertyApi = NetworkService.propertyApi
    
    suspend fun getPropertyList(): Result<List<Property>> = withContext(Dispatchers.IO) {
        try {
            Log.d("PropertyRepository", "Making API call...")

            // 如果直接格式失败，尝试包装格式
            try {
                Log.d("PropertyRepository", "Trying wrapped format...")
                val response = propertyApi.getPropertyListWrapped()
                Log.d("PropertyRepository", "Wrapped format - Response code: ${response.code()}")
                
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    Log.d("PropertyRepository", "Wrapped format - Response body: $apiResponse")
                    
                    if (apiResponse != null && apiResponse.data != null) {
                        Log.d("PropertyRepository", "Wrapped format - Data size: ${apiResponse.data.size}")
                        return@withContext Result.success(apiResponse.data)
                    }
                }
                
                Log.e("PropertyRepository", "Both formats failed")
                Result.failure(Exception("API request failed: ${response.code()} - ${response.message()}"))
                
            } catch (e: Exception) {
                Log.e("PropertyRepository", "Wrapped format also failed", e)
                Result.failure(e)
            }
            
        } catch (e: Exception) {
            Log.e("PropertyRepository", "Exception in getPropertyList", e)
            Result.failure(e)
        }
    }
} 