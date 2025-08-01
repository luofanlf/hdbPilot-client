package com.iss.ui.detail

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.iss.PredActivity
import com.iss.R
import com.iss.api.PropertyApi
import com.iss.model.Property
import com.iss.network.NetworkService
import com.iss.repository.PropertyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PropertyDetailFragment : Fragment() {

    private lateinit var titleText: TextView
    private lateinit var addressText: TextView
    private lateinit var propertyImageView: ImageView
    private lateinit var resalePriceText: TextView
    private lateinit var forecastPriceText: TextView
    private lateinit var bedroomText: TextView
    private lateinit var bathroomText: TextView
    private lateinit var areaText: TextView
    private lateinit var storeyText: TextView
    private lateinit var flatModelText: TextView
    private lateinit var topYearText: TextView
    private lateinit var statusText: TextView
    private lateinit var sellerIdText: TextView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var btnGoToPrediction: Button
    private lateinit var btnEdit: Button
    private lateinit var btnDelete: Button

    private val propertyRepository = PropertyRepository()
    private lateinit var propertyApi: PropertyApi
    private var propertyId: Long = -1
    private var isFromMyListings: Boolean = false // 标记是否来自My Listings

    private var loadedProperty: Property? = null // New: To store the loaded property object

    private lateinit var btnViewMap: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            propertyId = it.getLong(ARG_PROPERTY_ID, -1)
            isFromMyListings = it.getBoolean(ARG_FROM_MY_LISTINGS, false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_property_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupGoToPredictionButton() // Set up button listener after views are initialized

        btnViewMap.setOnClickListener {
            loadedProperty?.let { property ->
                val bundle = Bundle().apply {
                    putLong("highlight_property_id", property.id)
                }
                findNavController().navigate(R.id.mapFragment, bundle)
            }
        }

        // 只有从My Listings进入时才设置编辑和删除按钮
        if (isFromMyListings) {
            setupActionButtons() // Set up edit and delete buttons
        } else {
            // 从Home页面进入时隐藏编辑和删除按钮
            btnEdit.visibility = View.GONE
            btnDelete.visibility = View.GONE
        }

        // 初始化API
        propertyApi = NetworkService.propertyApi

        if (propertyId != -1L) {
            loadPropertyDetail(propertyId)
        } else {
            showError("Invalid property ID")
        }
    }

    private fun initViews(view: View) {
        titleText = view.findViewById(R.id.titleText)
        addressText = view.findViewById(R.id.addressText)
        propertyImageView = view.findViewById(R.id.propertyImageView)
        resalePriceText = view.findViewById(R.id.resalePriceText)

        bedroomText = view.findViewById(R.id.bedroomText)
        bathroomText = view.findViewById(R.id.bathroomText)
        areaText = view.findViewById(R.id.areaText)
        storeyText = view.findViewById(R.id.storeyText)
        flatModelText = view.findViewById(R.id.flatModelText)
        topYearText = view.findViewById(R.id.topYearText)
        statusText = view.findViewById(R.id.statusText)
        sellerIdText = view.findViewById(R.id.sellerIdText)
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)
        errorText = view.findViewById(R.id.errorText)
        btnGoToPrediction = view.findViewById(R.id.btnGoToPrediction)
        btnEdit = view.findViewById(R.id.btnEdit)
        btnDelete = view.findViewById(R.id.btnDelete)

        btnViewMap = view.findViewById(R.id.btnViewMap)
    }

    private fun setupGoToPredictionButton() {
        btnGoToPrediction.setOnClickListener {
            loadedProperty?.let { property ->
                val intent = Intent(requireContext(), PredActivity::class.java).apply {
                    // Pass all relevant property data as extras
                    putExtra("PROPERTY_FLOOR_AREA_SQM", property.floorAreaSqm) // Assuming Float
                    putExtra("PROPERTY_TOWN", property.town) // Assuming String
                    putExtra("PROPERTY_FLAT_TYPE", property.flatType) // Assuming String
                    putExtra("PROPERTY_FLAT_MODEL", property.flatModel) // Assuming String
                    putExtra("PROPERTY_STOREY_RANGE", property.storeyRange) // Assuming String, e.g., "07 TO 09"
                    putExtra("PROPERTY_REMAINING_LEASE", property.remainingLease) // Assuming String, e.g., "75 years 00 months"
                    putExtra("PROPERTY_MONTH", property.month) // Assuming String, e.g., "2019-06"
                    putExtra("PROPERTY_LEASE_COMMENCE_DATE", property.leaseCommenceDate) // Assuming Int
                    // You might need to adjust field names based on your 'Property' data class structure
                }
                startActivity(intent)
            } ?: run {
                Toast.makeText(requireContext(), "Property data not loaded yet.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupActionButtons() {
        btnEdit.setOnClickListener {
            loadedProperty?.let { property ->
                navigateToEditProperty(property)
            } ?: run {
                Toast.makeText(requireContext(), "Property data not loaded yet.", Toast.LENGTH_SHORT).show()
            }
        }

        btnDelete.setOnClickListener {
            loadedProperty?.let { property ->
                showDeleteConfirmationDialog(property)
            } ?: run {
                Toast.makeText(requireContext(), "Property data not loaded yet.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToEditProperty(property: Property) {
        try {
            val bundle = Bundle().apply {
                putLong("property_id", property.id)
            }
            findNavController().navigate(R.id.action_propertyDetailFragment_to_editPropertyFragment, bundle)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to open edit property", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmationDialog(property: Property) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Property")
            .setMessage("Are you sure you want to delete '${property.listingTitle}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteProperty(property)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteProperty(property: Property) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = propertyApi.deleteProperty(property.id)
                
                if (response.isSuccessful && response.body()?.data == true) {
                    Toast.makeText(requireContext(), "Property deleted successfully", Toast.LENGTH_LONG).show()
                    // 返回上一页
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                } else {
                    Toast.makeText(requireContext(), "Failed to delete property", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Network error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadPropertyDetail(id: Long) {
        showLoading(true)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = propertyRepository.getPropertyById(id)
                result.fold(
                    onSuccess = { property ->
                        android.util.Log.d("PropertyDetailFragment", "Property loaded: ${property.listingTitle}")
                        loadedProperty = property // Store the loaded property
                        showLoading(false)
                        displayPropertyDetail(property)
                    },
                    onFailure = { exception ->
                        android.util.Log.e("PropertyDetailFragment", "Failed to load property: ${exception.message}")
                        showLoading(false)
                        showError("Failed to load property: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("PropertyDetailFragment", "Exception loading property", e)
                showLoading(false)
                showError("Error: ${e.message}")
            }
        }
    }

    private fun displayPropertyDetail(property: Property) {
        titleText.text = property.listingTitle
        addressText.text = property.fullAddress
        resalePriceText.text = property.formattedResalePrice
        bedroomText.text = "${property.bedroomNumber} Bedroom${if (property.bedroomNumber > 1) "s" else ""}"
        bathroomText.text = "${property.bathroomNumber} Bathroom${if (property.bathroomNumber > 1) "s" else ""}"
        areaText.text = property.formattedArea
        storeyText.text = property.floorInfo
        flatModelText.text = property.flatModel
        topYearText.text = property.topYear.toString()
        statusText.text = property.status
        sellerIdText.text = property.sellerId.toString()

        hideError()
    }

    private fun showLoading(show: Boolean) {
        loadingProgressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            hideError()
        }
    }

    private fun showError(message: String) {
        errorText.visibility = View.VISIBLE
        errorText.text = message
        loadingProgressBar.visibility = View.GONE
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun hideError() {
        errorText.visibility = View.GONE
    }

    companion object {
        private const val ARG_PROPERTY_ID = "property_id"
        private const val ARG_FROM_MY_LISTINGS = "from_my_listings"

        @JvmStatic
        fun newInstance(propertyId: Long, fromMyListings: Boolean = false) =
            PropertyDetailFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_PROPERTY_ID, propertyId)
                    putBoolean(ARG_FROM_MY_LISTINGS, fromMyListings)
                }
            }
    }
}