package com.iss.ui.search

import android.os.Bundle
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.iss.R
import com.iss.adapter.PropertyAdapter
import com.iss.model.Property
import com.iss.model.PropertySearchRequest
import com.iss.repository.PropertyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PropertySearchFragment : Fragment() {
    private lateinit var searchLayout: TextInputLayout
    private lateinit var searchEditText: TextInputEditText
    private lateinit var filtersButton: MaterialButton
    private lateinit var filterFormCard: View
    private lateinit var propertiesRecyclerView: RecyclerView
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
    private var currentSearchRequest: PropertySearchRequest? = null
    
    // Filter form fields
    private lateinit var postalCodeEditText: TextInputEditText
    private lateinit var townEditText: TextInputEditText
    private lateinit var bedroomMinEditText: TextInputEditText
    private lateinit var bedroomMaxEditText: TextInputEditText
    private lateinit var bathroomMinEditText: TextInputEditText
    private lateinit var bathroomMaxEditText: TextInputEditText
    private lateinit var priceMinEditText: TextInputEditText
    private lateinit var priceMaxEditText: TextInputEditText
    private lateinit var areaMinEditText: TextInputEditText
    private lateinit var areaMaxEditText: TextInputEditText
    private lateinit var searchButton: MaterialButton
    private lateinit var clearFiltersButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_property_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupRecyclerView()
        setupClickListeners()
    }

    private fun initializeViews(view: View) {
        // Main views
        searchLayout = view.findViewById(R.id.searchLayout)
        searchEditText = view.findViewById(R.id.searchEditText)
        filtersButton = view.findViewById(R.id.filtersButton)
        filterFormCard = view.findViewById(R.id.filterFormCard)
        propertiesRecyclerView = view.findViewById(R.id.propertiesRecyclerView)
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)
        emptyStateText = view.findViewById(R.id.emptyStateText)
        
        // Filter form fields
        postalCodeEditText = view.findViewById(R.id.postalCodeEditText)
        townEditText = view.findViewById(R.id.townEditText)
        bedroomMinEditText = view.findViewById(R.id.bedroomMinEditText)
        bedroomMaxEditText = view.findViewById(R.id.bedroomMaxEditText)
        bathroomMinEditText = view.findViewById(R.id.bathroomMinEditText)
        bathroomMaxEditText = view.findViewById(R.id.bathroomMaxEditText)
        priceMinEditText = view.findViewById(R.id.priceMinEditText)
        priceMaxEditText = view.findViewById(R.id.priceMaxEditText)
        areaMinEditText = view.findViewById(R.id.areaMinEditText)
        areaMaxEditText = view.findViewById(R.id.areaMaxEditText)
        searchButton = view.findViewById(R.id.searchButton)
        clearFiltersButton = view.findViewById(R.id.clearFiltersButton)
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

    private fun setupClickListeners() {
        // Filters button - toggle filter form
        filtersButton.setOnClickListener {
            android.util.Log.d("PropertySearchFragment", "Filters button clicked")
            toggleFilterForm()
        }
        
        // Search button in filter form
        searchButton.setOnClickListener {
            android.util.Log.d("PropertySearchFragment", "Search button clicked")
            performSearch()
        }
        
        // Clear filters button
        clearFiltersButton.setOnClickListener {
            android.util.Log.d("PropertySearchFragment", "Clear filters button clicked")
            clearFilters()
        }
        
        // Search bar - perform quick search when search icon is clicked
        searchLayout.setStartIconOnClickListener {
            android.util.Log.d("PropertySearchFragment", "Search icon clicked - starting quick search")
            performQuickSearch()
        }
        
        // Search bar - also perform quick search on editor action (Enter key)
        searchEditText.setOnEditorActionListener { _, _, _ ->
            android.util.Log.d("PropertySearchFragment", "Search bar editor action")
            performQuickSearch()
            true
        }
    }

    private fun toggleFilterForm() {
        if (filterFormCard.visibility == View.VISIBLE) {
            filterFormCard.visibility = View.GONE
        } else {
            filterFormCard.visibility = View.VISIBLE
        }
    }

    private fun performQuickSearch() {
//        val query = searchEditText.text?.toString()?.trim()
//        android.util.Log.d("PropertySearchFragment", "performQuickSearch called with query: '$query'")
//
//        // 如果搜索框为空，搜索所有属性；否则按标题搜索
//        val searchRequest = if (query.isNullOrEmpty()) {
//            android.util.Log.d("PropertySearchFragment", "Searching all properties (empty query)")
//            PropertySearchRequest() // 空的搜索请求，应该返回所有属性
//        } else {
//            android.util.Log.d("PropertySearchFragment", "Searching by title: $query")
//            PropertySearchRequest(listingTitle = query)
//        }
        
        performSearch()
    }

    private fun loadNextPage() {
        if (isLoading || !hasMoreData || currentSearchRequest == null) {
            android.util.Log.d("PropertySearchFragment", "loadNextPage skipped: isLoading=$isLoading, hasMoreData=$hasMoreData, currentSearchRequest=${currentSearchRequest != null}")
            return
        }
        
        currentPage++
        android.util.Log.d("PropertySearchFragment", "Loading next page: $currentPage")
        performSearch(currentSearchRequest!!)
    }

    private fun resetPagination() {
        currentPage = 1
        hasMoreData = true
        allProperties.clear()
        propertyAdapter.updateProperties(emptyList())
    }

    private fun performSearch() {
        android.util.Log.d("PropertySearchFragment", "performSearch() called - Search button clicked")
        
        // 重置分页状态
        resetPagination()
        
        // 构建搜索请求
        val searchRequest = buildSearchRequest()
        android.util.Log.d("PropertySearchFragment", "Built search request: $searchRequest")
        
        // 保存当前搜索请求用于分页
        currentSearchRequest = searchRequest
        
        // 执行搜索
        performSearch(searchRequest)
    }

    private fun performSearch(searchRequest: PropertySearchRequest) {
        android.util.Log.d("PropertySearchFragment", "performSearch(searchRequest) called with page: $currentPage")
        
        // 更新页码
        val pagedSearchRequest = searchRequest.copy(pageNum = currentPage, pageSize = pageSize)
        android.util.Log.d("PropertySearchFragment", "Final search request: $pagedSearchRequest")
        
        // 防止重复加载
        if (isLoading) return
        
        isLoading = true
        showLoading(true)
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                android.util.Log.d("PropertySearchFragment", "Calling search API...")
                val result = propertyRepository.searchProperties(pagedSearchRequest)
                result.fold(
                    onSuccess = { pageResponse ->
                        android.util.Log.d("PropertySearchFragment", "Search API success: ${pageResponse.records.size} properties found")
                        
                        if (pageResponse.records.isNotEmpty()) {
                            if (currentPage == 1) {
                                allProperties.clear()
                            }
                            allProperties.addAll(pageResponse.records)
                            propertyAdapter.updateProperties(allProperties)
                        }
                        
                        hasMoreData = pageResponse.hasNext
                        updateEmptyState()
                        showLoading(false)
                        
                        if (pageResponse.records.isNotEmpty()) {
                            Toast.makeText(context, "Found ${pageResponse.records.size} properties", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "No properties found", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFailure = { exception ->
                        android.util.Log.e("PropertySearchFragment", "Search API failed: ${exception.message}")
                        Toast.makeText(context, "Search failed: ${exception.message}", Toast.LENGTH_LONG).show()
                        showLoading(false)
                        updateEmptyState()
                        if (currentPage > 1) {
                            currentPage--
                        }
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("PropertySearchFragment", "Search exception: ${e.message}")
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                showLoading(false)
                updateEmptyState()
                if (currentPage > 1) {
                    currentPage--
                }
            } finally {
                isLoading = false
            }
        }
    }

    private fun buildSearchRequest(): PropertySearchRequest {
        android.util.Log.d("PropertySearchFragment", "buildSearchRequest() called")
        
        // 检查UI元素是否正确绑定
        android.util.Log.d("PropertySearchFragment", "UI elements check:")
        android.util.Log.d("PropertySearchFragment", "  searchEditText: ${searchEditText != null}")
        android.util.Log.d("PropertySearchFragment", "  postalCodeEditText: ${postalCodeEditText != null}")
        android.util.Log.d("PropertySearchFragment", "  townEditText: ${townEditText != null}")
        
        // 使用搜索框的输入作为listingTitle
        val listingTitle = searchEditText.text?.toString()?.trim().takeIf { !it.isNullOrEmpty() }
        val postalCode = postalCodeEditText.text?.toString()?.trim().takeIf { !it.isNullOrEmpty() }
        val town = townEditText.text?.toString()?.trim().takeIf { !it.isNullOrEmpty() }
        val bedroomNumberMin = bedroomMinEditText.text?.toString()?.toIntOrNull()
        val bedroomNumberMax = bedroomMaxEditText.text?.toString()?.toIntOrNull()
        val bathroomNumberMin = bathroomMinEditText.text?.toString()?.toIntOrNull()
        val bathroomNumberMax = bathroomMaxEditText.text?.toString()?.toIntOrNull()
        val resalePriceMin = priceMinEditText.text?.toString()?.toFloatOrNull()
        val resalePriceMax = priceMaxEditText.text?.toString()?.toFloatOrNull()
        val floorAreaSqmMin = areaMinEditText.text?.toString()?.toFloatOrNull()
        val floorAreaSqmMax = areaMaxEditText.text?.toString()?.toFloatOrNull()
        
        android.util.Log.d("PropertySearchFragment", "Building search request with filters:")
        android.util.Log.d("PropertySearchFragment", "  listingTitle (from search box): '$listingTitle'")
        android.util.Log.d("PropertySearchFragment", "  postalCode: '$postalCode'")
        android.util.Log.d("PropertySearchFragment", "  town: '$town'")
        android.util.Log.d("PropertySearchFragment", "  bedroomNumberMin: $bedroomNumberMin")
        android.util.Log.d("PropertySearchFragment", "  bedroomNumberMax: $bedroomNumberMax")
        android.util.Log.d("PropertySearchFragment", "  bathroomNumberMin: $bathroomNumberMin")
        android.util.Log.d("PropertySearchFragment", "  bathroomNumberMax: $bathroomNumberMax")
        android.util.Log.d("PropertySearchFragment", "  resalePriceMin: $resalePriceMin")
        android.util.Log.d("PropertySearchFragment", "  resalePriceMax: $resalePriceMax")
        android.util.Log.d("PropertySearchFragment", "  floorAreaSqmMin: $floorAreaSqmMin")
        android.util.Log.d("PropertySearchFragment", "  floorAreaSqmMax: $floorAreaSqmMax")
        
        val request = PropertySearchRequest(
            listingTitle = listingTitle,
            postalCode = postalCode,
            town = town,
            bedroomNumberMin = bedroomNumberMin,
            bedroomNumberMax = bedroomNumberMax,
            bathroomNumberMin = bathroomNumberMin,
            bathroomNumberMax = bathroomNumberMax,
            resalePriceMin = resalePriceMin,
            resalePriceMax = resalePriceMax,
            floorAreaSqmMin = floorAreaSqmMin,
            floorAreaSqmMax = floorAreaSqmMax
        )
        
        android.util.Log.d("PropertySearchFragment", "Final request object: $request")
        return request
    }

    private fun clearFilters() {
        searchEditText.text?.clear()
        postalCodeEditText.text?.clear()
        townEditText.text?.clear()
        bedroomMinEditText.text?.clear()
        bedroomMaxEditText.text?.clear()
        bathroomMinEditText.text?.clear()
        bathroomMaxEditText.text?.clear()
        priceMinEditText.text?.clear()
        priceMaxEditText.text?.clear()
        areaMinEditText.text?.clear()
        areaMaxEditText.text?.clear()
    }

    private fun showLoading(show: Boolean) {
        loadingProgressBar.visibility = if (show) View.VISIBLE else View.GONE
        propertiesRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun updateEmptyState() {
        if (propertyAdapter.itemCount == 0 && loadingProgressBar.visibility != View.VISIBLE) {
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
            findNavController().navigate(R.id.action_search_to_detail, bundle)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to open property details", Toast.LENGTH_SHORT).show()
        }
    }
} 