package com.iss.ui.activities

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.iss.R
import com.iss.api.AuthApi
import com.iss.model.BaseResponse
import com.iss.model.User
import com.iss.network.NetworkService
import com.iss.utils.UserManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyActivitiesFragment : Fragment() {

    private lateinit var tvUsername: TextView
    private lateinit var tvEmail: TextView

    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_activities, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 初始化视图
        initViews(view)
        
        // 加载用户信息
        loadUserProfile()
        
        // 设置添加房源按钮点击事件
        view.findViewById<View>(R.id.btnAddProperty)?.setOnClickListener {
            // 使用Fragment导航
            findNavController().navigate(R.id.action_myActivitiesFragment_to_addPropertyFragment)
        }
        
        // 设置我的发布按钮点击事件
        view.findViewById<View>(R.id.btnMyListings)?.setOnClickListener {
            findNavController().navigate(R.id.action_myActivitiesFragment_to_myListingsFragment)
        }
        
        // 设置我的收藏按钮点击事件
        view.findViewById<View>(R.id.btnMyFavorites)?.setOnClickListener {
            findNavController().navigate(R.id.action_myActivitiesFragment_to_favoriteListFragment)
        }
        
        // 设置我的评论按钮点击事件
        view.findViewById<View>(R.id.btnMyComments)?.setOnClickListener {
            findNavController().navigate(R.id.action_myActivitiesFragment_to_commentListFragment)
        }
    }

    private fun initViews(view: View) {
        tvUsername = view.findViewById(R.id.tvUsername)
        tvEmail = view.findViewById(R.id.tvEmail)
    }

    private fun loadUserProfile() {
        try {
            // 检查用户是否已登录
            if (!UserManager.isLoggedIn()) {
                Log.w("MyActivitiesFragment", "User not logged in")
                            // 即使未登录，也显示默认信息
            tvUsername.text = "Guest User"
            tvEmail.text = "Please login to view profile"
                return
            }

            // 先显示本地存储的用户信息
            val localUsername = UserManager.getCurrentUsername()
            if (localUsername.isNotEmpty()) {
                tvUsername.text = localUsername
            }
            tvEmail.text = "Loading..."

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val user = getUserProfileFromApi()
                    if (user != null) {
                        updateUserInfo(user)
                    } else {
                        // API调用失败，保持本地信息
                        Log.w("MyActivitiesFragment", "Failed to get user profile from API")
                    }
                } catch (e: Exception) {
                    Log.e("MyActivitiesFragment", "Error loading user profile", e)
                    // 如果API调用失败，保持本地信息
                    if (localUsername.isNotEmpty()) {
                        tvUsername.text = localUsername
                    }
                    tvEmail.text = "Please login first"
                }
            }
        } catch (e: Exception) {
            Log.e("MyActivitiesFragment", "Critical error in loadUserProfile", e)
            // 设置默认值防止崩溃
            tvUsername.text = "Error"
            tvEmail.text = "Error"
        }
    }

    private suspend fun getUserProfileFromApi(): User? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("MyActivitiesFragment", "Starting API call to getUserProfile")
                val response = NetworkService.authApi.getUserProfile()
                Log.d("MyActivitiesFragment", "API response received: ${response.code()}")
                
                if (response.isSuccessful) {
                    val baseResponse = response.body()
                    Log.d("MyActivitiesFragment", "Response body: $baseResponse")
                    
                    if (baseResponse?.code == 0) {
                        Log.d("MyActivitiesFragment", "API call successful, user data: ${baseResponse.data}")
                        baseResponse.data
                    } else {
                        Log.w("MyActivitiesFragment", "API response not successful: code=${baseResponse?.code}, message=${baseResponse?.message}")
                        null
                    }
                } else {
                    Log.w("MyActivitiesFragment", "API call failed: ${response.code()}, error body: ${response.errorBody()?.string()}")
                    null
                }
            } catch (e: Exception) {
                Log.e("MyActivitiesFragment", "Exception in API call", e)
                null
            }
        }
    }

    private fun updateUserInfo(user: User) {
        // 更新用户名显示
        val displayName = user.nickname ?: user.username
        tvUsername.text = displayName
        
        // 更新邮箱显示
        val email = user.email ?: "No email provided"
        tvEmail.text = email
        
        Log.d("MyActivitiesFragment", "Updated user info: username=$displayName, email=$email")
    }
} 