package com.iss.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.iss.R
import com.iss.api.PropertyApi
import com.iss.databinding.FragmentMyListingsBinding
import com.iss.model.Property
import com.iss.network.NetworkService
import com.iss.ui.adapters.MyListingsAdapter
import kotlinx.coroutines.launch
import retrofit2.Response

class MyListingsFragment : Fragment() {

    private var _binding: FragmentMyListingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var propertyApi: PropertyApi
    private lateinit var adapter: MyListingsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyListingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化API
        propertyApi = NetworkService.propertyApi

        // 设置工具栏
        setupToolbar()

        // 设置RecyclerView
        setupRecyclerView()

        // 设置下拉刷新
        setupSwipeRefresh()

        // 加载数据
        loadMyListings()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = MyListingsAdapter(
            onItemClick = { property ->
                navigateToPropertyDetail(property)
            }
        )

        binding.rvMyListings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MyListingsFragment.adapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadMyListings()
        }
    }

    private fun loadMyListings() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateText.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // 调用获取用户发布房源的API
                val response = propertyApi.getUserProperties(101L) // 使用固定的sellerId，实际应用中应该从用户登录信息获取
                
                if (response.isSuccessful) {
                    val properties = response.body()?.data ?: emptyList()
                    if (properties.isEmpty()) {
                        showEmptyState()
                    } else {
                        adapter.submitList(properties)
                        binding.rvMyListings.visibility = View.VISIBLE
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to load listings: ${response.message()}", Toast.LENGTH_LONG).show()
                    showEmptyState()
                }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                showEmptyState()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun showEmptyState() {
        binding.rvMyListings.visibility = View.GONE
        binding.emptyStateText.visibility = View.VISIBLE
    }

    private fun navigateToPropertyDetail(property: Property) {
        try {
            val bundle = Bundle().apply {
                putLong("property_id", property.id)
                putBoolean("from_my_listings", true)
            }
            findNavController().navigate(R.id.action_myListingsFragment_to_propertyDetailFragment, bundle)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to open property details", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 