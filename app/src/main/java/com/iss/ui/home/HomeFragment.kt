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
import androidx.navigation.fragment.findNavController
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

class HomeFragment : Fragment() {
    private lateinit var propertiesRecyclerView: RecyclerView
    private lateinit var searchEditText: TextInputEditText
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var emptyStateText: TextView
    private lateinit var propertyAdapter: PropertyAdapter
    private val propertyRepository = PropertyRepository()

    // Pagination variables
    private var currentPage = 1
    private var pageSize = 10
    private var isLoading = false
    private var hasMoreData = true
    private var allProperties = mutableListOf<Property>()

    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        propertiesRecyclerView = view.findViewById(R.id.propertiesRecyclerView)
        searchEditText = view.findViewById(R.id.searchEditText)
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)
        emptyStateText = view.findViewById(R.id.emptyStateText)
        
        // Setup RecyclerView
        setupRecyclerView()

        // Setup search functionality
        setupSearch()

        // Load the first page of data
        loadFirstPage()
    }

    private fun setupRecyclerView() {
        propertyAdapter = PropertyAdapter(emptyList()) { property ->
            onPropertyClick(property)
        }
        
        propertiesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = propertyAdapter
            isNestedScrollingEnabled = true

            // Add scroll listener for pagination
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    // No need to proceed if scrolling up or data is already loading/no more data
                    if (dy <= 0 || isLoading || !hasMoreData) return

                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                    // Trigger load more when the user is near the end of the list
                    if (lastVisibleItemPosition >= totalItemCount - 3) {
                        loadNextPage()
                    }
                }
            })
        }
    }

    private fun setupSearch() {
        // 为整个搜索布局添加点击事件
        searchEditText.setOnClickListener {
            android.util.Log.d("HomeFragment", "Search box clicked")
            // Navigate to search page
            findNavController().navigate(R.id.action_home_to_search)
        }
        
        // 防止搜索框获得焦点，避免键盘弹出
        searchEditText.isFocusable = false
        searchEditText.isFocusableInTouchMode = false
    }

    private fun loadFirstPage() {
        // Reset state for a fresh load (e.g., for pull-to-refresh)
        currentPage = 1
        hasMoreData = true
        allProperties.clear()
        propertyAdapter.updateProperties(emptyList()) // Clear adapter immediately
        loadProperties()
            }

    private fun loadNextPage() {
        if (isLoading || !hasMoreData) {
            return
        }
        currentPage++
        loadProperties()
    }

    private fun loadProperties() {
        // Prevent multiple simultaneous loads
        if (isLoading) return

        isLoading = true
        // Only show the main progress bar for the first page
        if (currentPage == 1) {
        showLoading(true)
        } else {
            // For subsequent pages, you might want a smaller footer loading indicator
            // For now, we just rely on the isLoading flag
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = propertyRepository.getPropertyListPaged(currentPage, pageSize)
                result.fold(
                    onSuccess = { pageResponse ->
                        if (pageResponse.records.isNotEmpty()) {
                            if (currentPage == 1) {
                                allProperties.clear()
                            }
                            allProperties.addAll(pageResponse.records)
                            propertyAdapter.updateProperties(allProperties)
                        }

                        hasMoreData = pageResponse.hasNext
                        updateEmptyState()
                    },
                    onFailure = { exception ->
                        Toast.makeText(context, "Failed to load properties: ${exception.message}", Toast.LENGTH_LONG).show()
                        // If a page fails to load, decrement the page number to allow a retry
                        if (currentPage > 1) {
                            currentPage--
                        }
                        updateEmptyState()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                if (currentPage > 1) {
                    currentPage--
                }
                updateEmptyState()
            } finally {
                isLoading = false
                showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        loadingProgressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (currentPage == 1) {
        propertiesRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    private fun updateEmptyState() {
        if (propertyAdapter.itemCount == 0 && !isLoading) {
            emptyStateText.visibility = View.VISIBLE
            propertiesRecyclerView.visibility = View.GONE
        } else {
            emptyStateText.visibility = View.GONE
            propertiesRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun onPropertyClick(property: Property) {
        try {
            val bundle = Bundle().apply {
                putLong("property_id", property.id)
            }
            findNavController().navigate(R.id.action_home_to_detail, bundle)
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "Navigation failed", e)
            Toast.makeText(context, "Failed to open property details", Toast.LENGTH_SHORT).show()
        }
    }

    // The onResume method has been removed as it was causing the issue.
}