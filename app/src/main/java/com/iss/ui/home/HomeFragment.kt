package com.iss.ui.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.iss.R
import com.iss.adapter.PropertyAdapter
import com.iss.model.Property
import com.iss.repository.PropertyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private lateinit var searchEditText: TextInputEditText
    private lateinit var propertiesRecyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var propertyAdapter: PropertyAdapter
    private val propertyRepository = PropertyRepository()
    private var allProperties = listOf<Property>()

    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 初始化视图
        searchEditText = view.findViewById(R.id.searchEditText)
        propertiesRecyclerView = view.findViewById(R.id.propertiesRecyclerView)
        emptyStateText = view.findViewById(R.id.emptyStateText)
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)
        
        // 设置RecyclerView
        setupRecyclerView()
        
        // 设置搜索功能
        setupSearch()
        
        // 加载数据
        loadProperties()
    }

    private fun setupRecyclerView() {
        propertyAdapter = PropertyAdapter(emptyList()) { property ->
            // 处理房源点击事件
            onPropertyClick(property)
        }
        
        propertiesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = propertyAdapter
            // 确保滚动时内容不被遮挡
            isNestedScrollingEnabled = true
        }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                propertyAdapter.filterProperties(query)
                updateEmptyState()
            }
        })
    }

    private fun loadProperties() {
        showLoading(true)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = propertyRepository.getPropertyList()
                result.fold(
                    onSuccess = { properties ->
                        // 添加调试信息
                        android.util.Log.d("HomeFragment", "API Success: Received ${properties.size} properties")
                        allProperties = properties
                        propertyAdapter.updateProperties(properties)
                        showLoading(false)
                        updateEmptyState()
                        // 添加成功提示
                        Toast.makeText(context, "Successfully loaded ${properties.size} properties", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { exception ->
                        android.util.Log.e("HomeFragment", "API Failure: ${exception.message}")
                        Toast.makeText(context, "Failed to load properties: ${exception.message}", Toast.LENGTH_LONG).show()
                        showLoading(false)
                        updateEmptyState()
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "Exception in loadProperties", e)
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                showLoading(false)
                updateEmptyState()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        loadingProgressBar.visibility = if (show) View.VISIBLE else View.GONE
        propertiesRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun updateEmptyState() {
        val itemCount = propertyAdapter.itemCount
        android.util.Log.d("HomeFragment", "updateEmptyState: itemCount = $itemCount")
        
        if (itemCount == 0) {
            android.util.Log.d("HomeFragment", "Showing empty state")
            emptyStateText.visibility = View.VISIBLE
            propertiesRecyclerView.visibility = View.GONE
        } else {
            android.util.Log.d("HomeFragment", "Showing RecyclerView with $itemCount items")
            emptyStateText.visibility = View.GONE
            propertiesRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun onPropertyClick(property: Property) {
        // TODO: 跳转到房源详情页面
        // 这里可以添加导航到详情页面的逻辑
        // 例如：findNavController().navigate(HomeFragmentDirections.actionHomeToDetail(property))
        Toast.makeText(context, "Clicked: ${property.listingTitle}", Toast.LENGTH_SHORT).show()
    }
}