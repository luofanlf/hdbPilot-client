package com.iss.ui.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.iss.R

class MyActivitiesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_activities, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
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
    }
} 