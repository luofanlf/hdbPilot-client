package com.iss

import android.content.Intent
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

    private val authApi by lazy { NetworkService.authApi }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        tilEmail = findViewById(R.id.tilEmail)
        etPassword = findViewById(R.id.etPassword)
        tilPassword = findViewById(R.id.tilPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvError = findViewById(R.id.tvError)

        btnLogin.setOnClickListener {
            performLogin()
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

        // --- WARNING: DO NOT LOG SENSITIVE INFORMATION IN PRODUCTION ---
        // These logs are for debugging purposes only.
        // Remove them before deploying your app to users.
        Log.d("LOGIN_DEBUG", "Attempting login with:")
        Log.d("LOGIN_DEBUG", "  Username: $username")
        Log.d("LOGIN_DEBUG", "  Password: $password (FOR DEBUGGING ONLY - REMOVE IN PRODUCTION)")
        // --- END OF WARNING ---

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
                            Toast.makeText(this@LoginActivity, "Login Successful! Welcome, $username", Toast.LENGTH_SHORT).show()

                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()

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
}