package com.iss.repository

import com.iss.api.CommentApi
import com.iss.api.PropertyApi
import com.iss.model.Comment
import com.iss.model.Property
import com.iss.network.NetworkService
import com.iss.utils.UserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

class CommentRepository {
    private val commentApi: CommentApi = NetworkService.commentApi
    private val propertyApi: PropertyApi = NetworkService.propertyApi

    suspend fun getUserComments(): Result<List<Comment>> = withContext(Dispatchers.IO) {
        try {
            val userId = UserManager.getCurrentUserId()
            Log.d("CommentRepository", "Getting user comments for user: $userId")
            
            if (userId == -1L) {
                Log.w("CommentRepository", "User not logged in")
                return@withContext Result.failure(Exception("User not logged in"))
            }
            
            // 使用带用户ID的端点，避免session依赖
            val response = commentApi.getUserComments(userId)
            Log.d("CommentRepository", "API response: ${response.code()}")
            
            if (response.isSuccessful) {
                val comments = response.body()
                if (comments != null) {
                    Log.d("CommentRepository", "Successfully loaded ${comments.size} comments")
                    Result.success(comments)
                } else {
                    Log.w("CommentRepository", "Comments data is null")
                    Result.failure(Exception("Failed to get comments"))
                }
            } else {
                Log.w("CommentRepository", "API call failed: ${response.code()}")
                Result.failure(Exception("Failed to get comments: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("CommentRepository", "Exception in getUserComments", e)
            Result.failure(e)
        }
    }
    
    private fun getCurrentUserId(): Long? {
        val userId = UserManager.getCurrentUserId()
        return if (userId != -1L) userId else null
    }

    suspend fun getPropertyById(propertyId: Long): Result<Property> = withContext(Dispatchers.IO) {
        try {
            val response = propertyApi.getPropertyById(propertyId)
            if (response.isSuccessful) {
                val baseResponse = response.body()
                if (baseResponse?.code == 0 && baseResponse.data != null) {
                    Result.success(baseResponse.data)
                } else {
                    Result.failure(Exception(baseResponse?.message ?: "Property not found"))
                }
            } else {
                Result.failure(Exception("Failed to get property: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPropertiesByIds(propertyIds: List<Long>): Result<List<Property>> = withContext(Dispatchers.IO) {
        try {
            Log.d("CommentRepository", "Getting properties for IDs: $propertyIds")
            if (propertyIds.isEmpty()) {
                Log.d("CommentRepository", "No property IDs provided")
                return@withContext Result.success(emptyList())
            }
            
            val response = propertyApi.getPropertiesByIds(propertyIds)
            Log.d("CommentRepository", "Properties API response: ${response.code()}")
            
            if (response.isSuccessful) {
                val baseResponse = response.body()
                Log.d("CommentRepository", "Properties response body: $baseResponse")
                
                if (baseResponse?.code == 0) {
                    val properties = baseResponse.data
                    if (properties != null) {
                        Log.d("CommentRepository", "Successfully loaded ${properties.size} properties")
                        Result.success(properties)
                    } else {
                        Log.w("CommentRepository", "Properties data is null")
                        Result.failure(Exception("Failed to get properties"))
                    }
                } else {
                    Log.w("CommentRepository", "Properties API response not successful: code=${baseResponse?.code}, message=${baseResponse?.message}")
                    Result.failure(Exception(baseResponse?.message ?: "Failed to get properties"))
                }
            } else {
                Log.w("CommentRepository", "Properties API call failed: ${response.code()}")
                Result.failure(Exception("Failed to get properties: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("CommentRepository", "Exception in getPropertiesByIds", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteComment(commentId: Long?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (commentId == null) {
                return@withContext Result.failure(Exception("Comment ID is null"))
            }
            
            Log.d("CommentRepository", "Deleting comment with ID: $commentId")
            val response = commentApi.deleteComment(commentId)
            Log.d("CommentRepository", "Delete comment API response: ${response.code()}")
            
            if (response.isSuccessful) {
                Log.d("CommentRepository", "Comment deleted successfully")
                Result.success(Unit)
            } else {
                Log.w("CommentRepository", "Delete comment API call failed: ${response.code()}")
                Result.failure(Exception("Failed to delete comment: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("CommentRepository", "Exception in deleteComment", e)
            Result.failure(e)
        }
    }
}
