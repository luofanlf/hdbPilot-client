// com.iss.model.UserUpdateRequest.kt
package com.iss.model

data class UserUpdateRequest(
    val newUsername: String? = null,
    val newEmail: String? = null,
    val newNickname: String? = null,
    val newBio: String? = null,
    val oldPassword: String? = null,
    val newPassword: String? = null
)