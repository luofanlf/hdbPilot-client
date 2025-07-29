package com.iss.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.iss.R
import com.iss.model.Property
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

    private val propertyRepository = PropertyRepository()
    private var propertyId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            propertyId = it.getLong(ARG_PROPERTY_ID, -1)
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
        
        // 初始化视图
        initViews(view)
        
        // 加载数据
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
        forecastPriceText = view.findViewById(R.id.forecastPriceText)
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
    }

    private fun loadPropertyDetail(id: Long) {
        showLoading(true)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = propertyRepository.getPropertyById(id)
                result.fold(
                    onSuccess = { property ->
                        android.util.Log.d("PropertyDetailFragment", "Property loaded: ${property.listingTitle}")
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
        forecastPriceText.text = property.formattedForecastPrice
        bedroomText.text = "${property.bedroomNumber} Bedroom${if (property.bedroomNumber > 1) "s" else ""}"
        bathroomText.text = "${property.bathroomNumber} Bathroom${if (property.bathroomNumber > 1) "s" else ""}"
        areaText.text = property.formattedArea
        storeyText.text = property.floorInfo
        flatModelText.text = property.flatModel
        topYearText.text = property.topYear.toString()
        statusText.text = property.status
        sellerIdText.text = property.sellerId.toString()

        // 显示所有详情卡片
        hideError()
    }

    private fun showLoading(show: Boolean) {
        loadingProgressBar.visibility = if (show) View.VISIBLE else View.GONE
        // 隐藏其他视图
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

        @JvmStatic
        fun newInstance(propertyId: Long) =
            PropertyDetailFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_PROPERTY_ID, propertyId)
                }
            }
    }
} 