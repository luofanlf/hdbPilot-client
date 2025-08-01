package com.iss.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.iss.LoginActivity // 确保导入正确的登录 Activity
import com.iss.R

class UserProfileFragment : Fragment() {

    private lateinit var tvUsername: TextView
    private lateinit var btnLogout: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvUsername = view.findViewById(R.id.tvUsername)
        btnLogout = view.findViewById(R.id.btnLogout)

        // 从 SharedPreferences 获取用户名并显示
        val sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val username = sharedPreferences.getString("username", "Guest")
        tvUsername.text = "Welcome, $username"

        btnLogout.setOnClickListener {
            // 执行登出操作
            performLogout()
        }
    }

    private fun performLogout() {
        // 清除 SharedPreferences 中的所有登录信息
        val sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()

        // 跳转回登录界面
        val intent = Intent(requireActivity(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}