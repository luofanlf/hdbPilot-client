package com.iss.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.iss.LoginActivity
import com.iss.R
import com.iss.model.UserUpdateRequest
import com.iss.network.NetworkService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.delay

class UserProfileFragment : Fragment() {

    private lateinit var tvUsername: TextView
    private lateinit var tvUserRole: TextView
    private lateinit var etNickname: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etBio: TextInputEditText
    private lateinit var etOldPassword: TextInputEditText
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnSave: Button
    private lateinit var btnLogout: Button
    private var ivAvatarView: de.hdodenhof.circleimageview.CircleImageView? = null

    private val authApi by lazy { NetworkService.authApi }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            uploadAvatar(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("UserProfileFragment", "onViewCreated() called. Initializing UI components.")
        // 1. 绑定UI组件
        tvUsername = view.findViewById(R.id.tvUsername)
        tvUserRole = view.findViewById(R.id.tvUserRole)
        etNickname = view.findViewById(R.id.etNickname)
        etEmail = view.findViewById(R.id.etEmail)
        etBio = view.findViewById(R.id.etBio)
        etOldPassword = view.findViewById(R.id.etOldPassword)
        etNewPassword = view.findViewById(R.id.etNewPassword)
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword)
        btnSave = view.findViewById(R.id.btnSave)
        btnLogout = view.findViewById(R.id.btnLogout)
        ivAvatarView = view.findViewById(R.id.ivAvatar)

        // 2. 加载用户信息并填充到UI
        loadUserProfile()

        // 3. 设置按钮点击监听器
        btnSave.setOnClickListener {
            Log.d("UserProfileFragment", "Save button clicked. Starting update process.")
            updateUserProfile()
        }

        btnLogout.setOnClickListener {
            Log.d("UserProfileFragment", "Logout button clicked. Performing logout.")
            performLogout()
        }

        // 4. 点击头像选择图片
        ivAvatarView?.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun loadUserProfile() {
        val sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        val storedUserId: Long = try {
            sharedPreferences.getLong("user_id", -1L)
        } catch (e: ClassCastException) {
            sharedPreferences.getString("user_id", null)?.toLongOrNull() ?: -1L
        }
        val storedUsername = sharedPreferences.getString("username", "Guest")
        val storedNickname = sharedPreferences.getString("nickname", "")
        val storedEmail = sharedPreferences.getString("email", "")
        val storedBio = sharedPreferences.getString("bio", "")
        val storedUserRole = sharedPreferences.getString("user_role", "User")
        val storedAvatarUrl = sharedPreferences.getString("avatar_url", null)

        Log.d("UserProfileFragment", "Loading user profile from SharedPreferences:")
        Log.d("UserProfileFragment", "User ID: $storedUserId")
        Log.d("UserProfileFragment", "Username: $storedUsername")
        Log.d("UserProfileFragment", "Nickname: $storedNickname")
        Log.d("UserProfileFragment", "Email: $storedEmail")
        Log.d("UserProfileFragment", "Bio: $storedBio")
        Log.d("UserProfileFragment", "User Role: $storedUserRole")

        if (storedUserId == -1L || storedUsername.isNullOrBlank() || storedUsername == "Guest") {
            Log.w("UserProfileFragment", "Invalid user in SharedPreferences. Forcing logout.")
            Toast.makeText(requireContext(), "User not found in local storage. Please log in again.", Toast.LENGTH_LONG).show()
            performLogout()
            return
        }

        tvUsername.text = "Welcome, $storedUsername"
        tvUserRole.text = "Role: $storedUserRole"
        etNickname.setText(storedNickname)
        etEmail.setText(storedEmail)
        etBio.setText(storedBio)

        try {
            val avatarView = view?.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.ivAvatar)
            if (!storedAvatarUrl.isNullOrBlank() && avatarView != null) {
                com.bumptech.glide.Glide.with(this)
                    .load(storedAvatarUrl)
                    .placeholder(R.drawable.ic_activities)
                    .error(R.drawable.ic_activities)
                    .signature(com.bumptech.glide.signature.ObjectKey(System.currentTimeMillis()))
                    .into(avatarView)
            }
        } catch (e: Exception) {
            Log.w("UserProfileFragment", "Failed to load avatar: ${e.message}")
        }
    }

    private fun uploadAvatar(uri: Uri) {
        val sp = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId: Long = try {
            sp.getLong("user_id", -1L)
        } catch (e: ClassCastException) {
            sp.getString("user_id", null)?.toLongOrNull() ?: -1L
        }
        if (userId == -1L) {
            Toast.makeText(requireContext(), "Invalid user. Please re-login.", Toast.LENGTH_LONG).show()
            return
        }

        val tempFile = createTempFileFromUri(uri) ?: run {
            Toast.makeText(requireContext(), "Cannot read selected image.", Toast.LENGTH_LONG).show()
            return
        }
        val reqBody = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("imageFile", tempFile.name, reqBody)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = authApi.updateAvatar(userId, part)
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful && resp.body()?.code == 0 && !resp.body()?.data.isNullOrBlank()) {
                        val newUrl = resp.body()!!.data!!
                        val sp = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        val committed = sp.edit().putString("avatar_url", newUrl).commit()
                        Log.d("UserProfileFragment", "Avatar updated via API, newUrl=$newUrl, committed=$committed")
                        // 立即刷新本页与顶部头像
                        loadUserProfile()
                        (activity as? com.iss.MainActivity)?.refreshUserAvatarIcon()
                        Toast.makeText(requireContext(), "Avatar updated successfully.", Toast.LENGTH_SHORT).show()
                    } else {
                        val msg = resp.body()?.message ?: ("Upload failed: " + resp.code())
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("UserProfileFragment", "Upload avatar failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                tempFile.delete()
            }
        }
    }

    // 不再轮询
    private fun refreshProfileAfterAvatarUpdated() { }

    private fun createTempFileFromUri(uri: Uri): File? {
        return try {
            val input = requireContext().contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("avatar_", ".jpg", requireContext().cacheDir)
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
            tempFile
        } catch (e: Exception) {
            Log.e("UserProfileFragment", "createTempFileFromUri error: ${e.message}")
            null
        }
    }

    private fun updateUserProfile() {
        val oldPassword = etOldPassword.text.toString().trim()
        val newPassword = etNewPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        if (newPassword.isNotEmpty() || oldPassword.isNotEmpty() || confirmPassword.isNotEmpty()) {
            if (newPassword.length < 8) {
                Toast.makeText(requireContext(), "New password must be at least 8 characters.", Toast.LENGTH_SHORT).show()
                Log.w("UserProfileFragment", "New password is too short.")
                return
            }
            if (newPassword != confirmPassword) {
                Toast.makeText(requireContext(), "New passwords do not match.", Toast.LENGTH_SHORT).show()
                Log.w("UserProfileFragment", "New passwords do not match.")
                return
            }
        }

        val request = UserUpdateRequest(
            newNickname = etNickname.text.toString().trim().takeIf { it.isNotBlank() },
            newEmail = etEmail.text.toString().trim().takeIf { it.isNotBlank() },
            newBio = etBio.text.toString().trim().takeIf { it.isNotBlank() },
            oldPassword = oldPassword.takeIf { it.isNotBlank() },
            newPassword = newPassword.takeIf { it.isNotBlank() }
        )

        Log.d("UserProfileFragment", "Sending update profile request with:")
        Log.d("UserProfileFragment", "New Nickname: ${request.newNickname}")
        Log.d("UserProfileFragment", "New Email: ${request.newEmail}")
        Log.d("UserProfileFragment", "New Bio: ${request.newBio}")
        Log.d("UserProfileFragment", "Old Password: ${request.oldPassword?.let { "[HIDDEN]" } ?: "null"}")
        Log.d("UserProfileFragment", "New Password: ${request.newPassword?.let { "[HIDDEN]" } ?: "null"}")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("UserProfileFragment", "Starting network request...")
                val response = authApi.updateUserProfile(request)
                Log.d("UserProfileFragment", "Network request finished with status code: ${response.code()}")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val baseResponse = response.body()
                        Log.d("UserProfileFragment", "Response body received: ${baseResponse.toString()}")

                        if (baseResponse?.code == 0) {
                            Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                            Log.i("UserProfileFragment", "Profile updated successfully! Updating SharedPreferences.")
                            val sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                            sharedPreferences.edit().apply {
                                putString("nickname", etNickname.text.toString().trim())
                                putString("email", etEmail.text.toString().trim())
                                putString("bio", etBio.text.toString().trim())
                                apply()
                            }
                            etOldPassword.text?.clear()
                            etNewPassword.text?.clear()
                            etConfirmPassword.text?.clear()
                        } else if (baseResponse?.message?.contains("User not found", ignoreCase = true) == true || response.code() == 401) {
                            showSessionExpiredDialog()
                        }
                        else {
                            val errorMessage = baseResponse?.message ?: "Failed to update profile."
                            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                            Log.w("UserProfileFragment", "Update failed due to backend logic: $errorMessage (Code: ${baseResponse?.code})")
                        }
                    } else if (response.code() == 401) {
                        showSessionExpiredDialog()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = "Failed to update profile: ${response.code()} - $errorBody"
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                        Log.e("UserProfileFragment", "Update failed due to HTTP error: $errorMessage")
                    }
                }
            } catch (e: Exception) {
                Log.e("UserProfileFragment", "Network error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showSessionExpiredDialog() {
        if (!isAdded) return

        AlertDialog.Builder(requireContext())
            .setTitle("Account Security Notification")
            .setMessage("You have been logged out for security reasons. Please log in again to verify your identity.")
            .setPositiveButton("Confirm") { dialog, _ ->
                dialog.dismiss()
                performLogout()
            }
            .setCancelable(false)
            .show()
    }

    private fun performLogout() {
        val sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
        Log.i("UserProfileFragment", "SharedPreferences cleared. Logging out.")

        val intent = Intent(requireActivity(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}