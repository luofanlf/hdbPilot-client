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
import com.iss.api.AuthApi
import com.iss.model.UserRegisterRequest
import com.iss.network.NetworkService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {

    private lateinit var etUsernameRegister: TextInputEditText
    private lateinit var tilUsernameRegister: TextInputLayout
    private lateinit var etEmailRegister: TextInputEditText
    private lateinit var tilEmailRegister: TextInputLayout
    private lateinit var etPasswordRegister: TextInputEditText
    private lateinit var tilPasswordRegister: TextInputLayout
    private lateinit var etConfirmPasswordRegister: TextInputEditText
    private lateinit var tilConfirmPasswordRegister: TextInputLayout
    private lateinit var btnRegister: Button
    private lateinit var tvRegisterError: TextView

    private val authApi: AuthApi by lazy { NetworkService.authApi }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize views
        etUsernameRegister = findViewById(R.id.etUsernameRegister)
        tilUsernameRegister = findViewById(R.id.tilUsername)
        etEmailRegister = findViewById(R.id.etEmailRegister)
        tilEmailRegister = findViewById(R.id.tilEmailRegister)
        etPasswordRegister = findViewById(R.id.etPasswordRegister)
        tilPasswordRegister = findViewById(R.id.tilPasswordRegister)
        etConfirmPasswordRegister = findViewById(R.id.etConfirmPasswordRegister)
        tilConfirmPasswordRegister = findViewById(R.id.tilConfirmPasswordRegister)
        btnRegister = findViewById(R.id.btnRegister)
        tvRegisterError = findViewById(R.id.tvRegisterError)

        // Set up register button click listener
        btnRegister.setOnClickListener {
            performRegistration()
        }
    }

    private fun performRegistration() {
        val username = etUsernameRegister.text.toString().trim()
        val email = etEmailRegister.text.toString().trim()
        val password = etPasswordRegister.text.toString().trim()
        val confirmPassword = etConfirmPasswordRegister.text.toString().trim()

        // Clear previous errors
        tilUsernameRegister.error = null
        tilEmailRegister.error = null
        tilPasswordRegister.error = null
        tilConfirmPasswordRegister.error = null
        tvRegisterError.visibility = View.GONE

        // Client-side validation
        if (username.isEmpty()) {
            tilUsernameRegister.error = "Username cannot be empty"
            return
        }
        if (password.isEmpty()) {
            tilPasswordRegister.error = "Password cannot be empty"
            return
        }
        if (confirmPassword.isEmpty()) {
            tilConfirmPasswordRegister.error = "Confirm Password cannot be empty"
            return
        }
        if (password != confirmPassword) {
            tilConfirmPasswordRegister.error = "Passwords do not match"
            tilPasswordRegister.error = "Passwords do not match"
            return
        }
        // Basic email format validation (optional)
        if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmailRegister.error = "Invalid email format"
            return
        }

        // WARNING: DO NOT LOG SENSITIVE INFORMATION IN PRODUCTION
        Log.d("REGISTER_DEBUG", "Attempting registration with:")
        Log.d("REGISTER_DEBUG", "  Username: $username")
        Log.d("REGISTER_DEBUG", "  Email: $email")
        Log.d("REGISTER_DEBUG", "  Password: $password (FOR DEBUGGING ONLY - REMOVE IN PRODUCTION)")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Backend expects username, password, confirmPassword
                val request = UserRegisterRequest(username, password, confirmPassword,email)
                val response = authApi.registerUser(request)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val baseResponse = response.body()
                        if (baseResponse != null && baseResponse.code == 0 && baseResponse.data != null) {
                            val userId = baseResponse.data
                            Log.d("RegisterActivity", "Registration Successful! User ID: $userId")
                            Toast.makeText(this@RegisterActivity, "Registration Successful! Please login.", Toast.LENGTH_LONG).show()

                            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            startActivity(intent)
                            finish()

                        } else {
                            val errorMessage = baseResponse?.message ?: "Registration failed due to unknown reason."
                            Log.w("RegisterActivity", "Registration Failed (Backend Business Logic): $errorMessage (Code: ${baseResponse?.code})")
                            tvRegisterError.text = errorMessage
                            tvRegisterError.visibility = View.VISIBLE
                            Toast.makeText(this@RegisterActivity, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = try {
                            val jsonError = org.json.JSONObject(errorBody)
                            jsonError.optString("message", "Registration failed. HTTP ${response.code()}")
                        } catch (e: Exception) {
                            "Registration failed. HTTP ${response.code()}"
                        }
                        Log.e("RegisterActivity", "Registration Failed (HTTP): ${response.code()} - $errorBody")
                        tvRegisterError.text = errorMessage
                        tvRegisterError.visibility = View.VISIBLE
                        Toast.makeText(this@RegisterActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("RegisterActivity", "Registration Exception: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    tvRegisterError.text = "Network error or server unavailable: ${e.message}"
                    tvRegisterError.visibility = View.VISIBLE
                    Toast.makeText(this@RegisterActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}