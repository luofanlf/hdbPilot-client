package com.iss.repository

import com.iss.model.BaseResponse
import com.iss.model.Favorite
import com.iss.model.FavoriteRequest
import com.iss.model.PageResponse
import com.iss.network.NetworkService
import com.iss.utils.UserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FavoriteRepository {
    private val favoriteApi = NetworkService.favoriteApi

    suspend fun addFavorite(propertyId: Long): Result<Favorite> = withContext(Dispatchers.IO) {
        try {
            val userId = UserManager.getCurrentUserId()
            val request = FavoriteRequest(propertyId)
            val response = favoriteApi.addFavorite(userId, request)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.code == 0) {
                    Result.success(body.data!!)
                } else {
                    Result.failure(Exception(body?.message ?: "添加收藏失败"))
                }
            } else {
                Result.failure(Exception("网络请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFavorite(favoriteId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val userId = UserManager.getCurrentUserId()
            val response = favoriteApi.removeFavorite(favoriteId, userId)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.code == 0) {
                    Result.success(body.data!!)
                } else {
                    Result.failure(Exception(body?.message ?: "取消收藏失败"))
                }
            } else {
                Result.failure(Exception("网络请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserFavorites(pageNum: Int = 1, pageSize: Int = 10): Result<PageResponse<Favorite>> = withContext(Dispatchers.IO) {
        try {
            val userId = UserManager.getCurrentUserId()
            val response = favoriteApi.getUserFavorites(userId, pageNum, pageSize)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.code == 0) {
                    Result.success(body.data!!)
                } else {
                    Result.failure(Exception(body?.message ?: "获取收藏列表失败"))
                }
            } else {
                Result.failure(Exception("网络请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isFavorite(propertyId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val userId = UserManager.getCurrentUserId()
            val response = favoriteApi.isFavorite(userId, propertyId)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.code == 0) {
                    Result.success(body.data!!)
                } else {
                    Result.failure(Exception(body?.message ?: "检查收藏状态失败"))
                }
            } else {
                Result.failure(Exception("网络请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserFavoritePropertyIds(): Result<List<Long>> = withContext(Dispatchers.IO) {
        try {
            val userId = UserManager.getCurrentUserId()
            val response = favoriteApi.getUserFavoritePropertyIds(userId)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.code == 0) {
                    Result.success(body.data!!)
                } else {
                    Result.failure(Exception(body?.message ?: "获取收藏房源ID列表失败"))
                }
            } else {
                Result.failure(Exception("网络请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 