package com.iss

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var tilPassword: TextInputLayout
    private lateinit var btnLogin: Button
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login) // Set the login layout

        // Initialize views
        etEmail = findViewById(R.id.etEmail)
        tilEmail = findViewById(R.id.tilEmail)
        etPassword = findViewById(R.id.etPassword)
        tilPassword = findViewById(R.id.tilPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvError = findViewById(R.id.tvError)

        // Set up login button click listener
        btnLogin.setOnClickListener {
            performLogin()
        }
    }

    private fun performLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Clear previous errors
        tilEmail.error = null
        tilPassword.error = null
        tvError.visibility = View.GONE

        // Input validation
        if (email.isEmpty()) {
            tilEmail.error = "Email/Username cannot be empty"
            return
        }
        if (password.isEmpty()) {
            tilPassword.error = "Password cannot be empty"
            return
        }

        // --- Simple hardcoded authentication for demonstration ---
        // In a real app, you would make an API call here.
        if (email == "test@example.com" && password == "password123") {
            Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()

            // Navigate to MainActivity after successful login
            val intent = Intent(this, MainActivity::class.java)
            // Clear back stack so user cannot go back to login screen with back button
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish() // Finish LoginActivity so user cannot return to it

        } else {
            tvError.text = "Invalid email/username or password"
            tvError.visibility = View.VISIBLE
            Toast.makeText(this, "Login Failed!", Toast.LENGTH_SHORT).show()
        }
    }
}