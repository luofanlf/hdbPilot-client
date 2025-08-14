package com.iss.model

import com.google.gson.annotations.SerializedName

data class UserRegisterRequest(
    val username: String,

    val password: String,
    @SerializedName("confirmPassword")
    val confirmPassword: String,
    val email:String
)