package com.iss

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.iss.model.LoginRequest
import com.iss.network.NetworkService
import com.iss.utils.UserManager
import com.iss.network.initialize // 确保导入
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var tilPassword: TextInputLayout
    private lateinit var btnLogin: Button
    private lateinit var tvError: TextView
    private lateinit var tvSignUpLink: TextView

    private lateinit var sharedPreferences: SharedPreferences

    // 关键修改：通过 NetworkService 的伴生对象获取 authApi
    private val authApi by lazy { NetworkService.authApi }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 关键修改：在 onCreate() 的最开始调用初始化方法
        initialize(this)

        sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        if (isLoggedIn()) {
            Log.d("LoginActivity", "User is already logged in, navigating to MainActivity.")
            navigateToMainActivity()
            return
        }

        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        tilEmail = findViewById(R.id.tilEmail)
        etPassword = findViewById(R.id.etPassword)
        tilPassword = findViewById(R.id.tilPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvError = findViewById(R.id.tvError)
        tvSignUpLink = findViewById(R.id.tvSignUpLink)

        btnLogin.setOnClickListener {
            performLogin()
        }

        tvSignUpLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun performLogin() {
        val username = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        tilEmail.error = null
        tilPassword.error = null
        tvError.visibility = View.GONE

        if (username.isEmpty()) {
            tilEmail.error = "Username cannot be empty"
            return
        }
        if (password.isEmpty()) {
            tilPassword.error = "Password cannot be empty"
            return
        }

        Log.d("LOGIN_DEBUG", "Attempting login with username: $username")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = LoginRequest(username = username, password = password)
                val response = authApi.loginUser(request)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val baseResponse = response.body()
                        if (baseResponse != null && baseResponse.code == 0 && baseResponse.data != null) {
                            Log.d("LoginActivity", "Login Successful! User ID: ${baseResponse.data}")
                            // 关键修改：登录成功后，调用新的方法获取用户资料
                            fetchUserProfile()
                        } else {
                            val errorMessage = baseResponse?.message ?: "Login failed due to unknown reason."
                            Log.w("LoginActivity", "Login Failed (Backend Business Logic): $errorMessage (Code: ${baseResponse?.code})")
                            tvError.text = errorMessage
                            tvError.visibility = View.VISIBLE
                            Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = try {
                            val jsonError = org.json.JSONObject(errorBody)
                            jsonError.optString("message", "Authentication failed. HTTP ${response.code()}")
                        } catch (e: Exception) {
                            "Authentication failed. HTTP ${response.code()}"
                        }
                        Log.e("LoginActivity", "Login Failed (HTTP): ${response.code()} - $errorBody")
                        tvError.text = errorMessage
                        tvError.visibility = View.VISIBLE
                        Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Login Exception: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    tvError.text = "Network error or server unavailable: ${e.message}"
                    tvError.visibility = View.VISIBLE
                    Toast.makeText(this@LoginActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 关键修改：新增方法，用于获取并保存用户资料
    private fun fetchUserProfile() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profileResponse = authApi.getUserProfile()
                withContext(Dispatchers.Main) {
                    if (profileResponse.isSuccessful) {
                        val baseResponse = profileResponse.body()
                        if (baseResponse?.code == 0 && baseResponse.data != null) {
                            val userProfile = baseResponse.data
                            Log.d("LoginActivity", "Successfully fetched user profile: $userProfile")

                            // 使用UserManager来保存用户信息
                            UserManager.init(this@LoginActivity)
                            UserManager.setCurrentUser(userProfile.id, userProfile.username)
                            
                            // 更新SharedPreferences中的其他用户资料（含头像、角色）
                            sharedPreferences.edit().apply {
                                putString("nickname", userProfile.nickname)
                                putString("email", userProfile.email)
                                putString("bio", userProfile.bio)
                                putString("avatar_url", userProfile.avatarUrl)
                                putString("user_role", userProfile.role)
                                apply()
                            }

                            Toast.makeText(this@LoginActivity, "Login Successful! Welcome, ${userProfile.username}", Toast.LENGTH_SHORT).show()
                            navigateToMainActivity()
                        } else {
                            // 获取资料失败，但已登录，仍然跳转
                            Log.w("LoginActivity", "Failed to fetch user profile after login. Navigating to main.")
                            Toast.makeText(this@LoginActivity, "Login successful, but failed to load profile.", Toast.LENGTH_LONG).show()
                            navigateToMainActivity()
                        }
                    } else {
                        // 获取资料失败，但已登录，仍然跳转
                        Log.w("LoginActivity", "HTTP error while fetching profile. Navigating to main.")
                        Toast.makeText(this@LoginActivity, "Login successful, but failed to load profile.", Toast.LENGTH_LONG).show()
                        navigateToMainActivity()
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Network error occurred while fetching profile: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    // 获取资料失败，但已登录，仍然跳转
                    Toast.makeText(this@LoginActivity, "Login successful, but network error occurred while fetching profile.", Toast.LENGTH_LONG).show()
                    navigateToMainActivity()
                }
            }
        }
    }


    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }


    private fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean("is_logged_in", false)
    }

    private fun saveLoginStatus(userId: String, username: String) {
        // 使用UserManager来保存用户信息
        UserManager.init(this)
        try {
            val userIdLong = userId.toLong()
            UserManager.setCurrentUser(userIdLong, username)
        } catch (e: NumberFormatException) {
            // 如果转换失败，记录错误
            Log.e("LoginActivity", "Failed to convert userId to Long: $userId")
        }
    }
}