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

    private lateinit var sharedPreferences: SharedPreferences // 直接在 Activity 中声明 SharedPreferences

    private val authApi by lazy { NetworkService.authApi }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化 SharedPreferences
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
                            val userId = baseResponse.data
                            Log.d("LoginActivity", "Login Successful! User ID: $userId")


                            saveLoginStatus(userId.toString(), username)

                            Toast.makeText(this@LoginActivity, "Login Successful! Welcome, $username", Toast.LENGTH_SHORT).show()

                            navigateToMainActivity()

                        } else {
                            val errorMessage = baseResponse?.message ?: "Login failed due to unknown reason."
                            Log.w("LoginActivity", "Login Failed (Backend Business Logic): $errorMessage (Code: ${baseResponse?.code})")
                            tvError.text = errorMessage
                            tvError.visibility = View.VISIBLE
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
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Login Exception: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    tvError.text = "Network error or server unavailable: ${e.message}"
                    tvError.visibility = View.VISIBLE
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
        sharedPreferences.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_id", userId)
            putString("username", username)
            apply()
        }
    }
}