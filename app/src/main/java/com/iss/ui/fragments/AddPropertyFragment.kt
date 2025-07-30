package com.iss.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.iss.api.PropertyApi
import com.iss.databinding.FragmentAddPropertyBinding
import com.iss.model.PropertyRequest
import com.iss.network.NetworkService
import kotlinx.coroutines.launch
import retrofit2.Response

class AddPropertyFragment : Fragment() {

    private var _binding: FragmentAddPropertyBinding? = null
    private val binding get() = _binding!!
    private lateinit var propertyApi: PropertyApi
    
    // 下拉框数据
    private val towns = arrayOf(
        "Ang Mo Kio", "Bedok", "Bishan", "Boon Lay", "Bukit Batok", 
        "Bukit Merah", "Bukit Panjang", "Bukit Timah", "Central Water Catchment",
        "Choa Chu Kang", "Clementi", "Downtown Core", "Geylang", "Hougang",
        "Jurong East", "Jurong West", "Kallang", "Lim Chu Kang", "Mandai",
        "Marina East", "Marina South", "Marine Parade", "Museum", "Newton",
        "North-Eastern Islands", "Novena", "Orchard", "Outram", "Pasir Ris",
        "Paya Lebar", "Pioneer", "Punggol", "Queenstown", "River Valley",
        "Rochor", "Sembawang", "Sengkang", "Serangoon", "Simpang", "Singapore River",
        "Southern Islands", "Straits View", "Sungei Kadut", "Tampines", "Tanglin",
        "Tengah", "Toa Payoh", "Tuas", "Western Islands", "Western Water Catchment",
        "Woodlands", "Yishun"
    )
    
    private val flatModels = arrayOf(
        "1-Room", "2-Room", "3-Room", "4-Room", "5-Room", 
        "3-Room Executive", "4-Room Executive", "5-Room Executive",
        "Multi-Generation", "Studio Apartment", "Type S1", "Type S2"
    )
    
    private val years = (1960..2024).toList().map { it.toString() }.toTypedArray()
    
    // 选中的值
    private var selectedTown = ""
    private var selectedFlatModel = ""
    private var selectedYear = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddPropertyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化API
        propertyApi = NetworkService.propertyApi

        // 设置下拉框点击事件
        setupDropdowns()

        // 设置表单验证
        setupFormValidation()

        // 设置提交按钮
        setupSubmitButton()
    }

    private fun setupDropdowns() {
        // 设置城镇下拉框
        binding.etTown.setOnClickListener {
            showTownDialog()
        }

        // 设置公寓模型下拉框
        binding.etFlatModel.setOnClickListener {
            showFlatModelDialog()
        }

        // 设置建成年份下拉框
        binding.etTopYear.setOnClickListener {
            showYearDialog()
        }
    }

    private fun showTownDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Select Town")
        builder.setItems(towns) { _, which ->
            selectedTown = towns[which]
            binding.etTown.setText(selectedTown)
        }
        builder.show()
    }

    private fun showFlatModelDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Select Flat Model")
        builder.setItems(flatModels) { _, which ->
            selectedFlatModel = flatModels[which]
            binding.etFlatModel.setText(selectedFlatModel)
        }
        builder.show()
    }

    private fun showYearDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Select Year")
        builder.setItems(years) { _, which ->
            selectedYear = years[which]
            binding.etTopYear.setText(selectedYear)
        }
        builder.show()
    }

    private fun setupFormValidation() {
        // 设置邮政编码验证
        binding.tilPostalCode.setEndIconOnClickListener {
            validatePostalCode()
        }

        // 设置价格验证
        binding.tilResalePrice.setEndIconOnClickListener {
            validatePrice()
        }
    }

    private fun setupSubmitButton() {
        binding.btnSubmit.setOnClickListener {
            if (validateForm()) {
                submitProperty()
            }
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        // 验证房源标题
        if (binding.etListingTitle.text.isNullOrBlank()) {
            binding.tilListingTitle.error = "Please enter listing title"
            isValid = false
        } else {
            binding.tilListingTitle.error = null
        }

        // 验证城镇
        if (selectedTown.isBlank()) {
            binding.tilTown.error = "Please select a town"
            isValid = false
        } else {
            binding.tilTown.error = null
        }

        // 验证邮政编码
        if (!validatePostalCode()) {
            isValid = false
        }

        // 验证卧室数量
        val bedroomNumber = binding.etBedroomNumber.text.toString().toIntOrNull()
        if (bedroomNumber == null || bedroomNumber < 1 || bedroomNumber > 10) {
            binding.tilBedroomNumber.error = "Please enter valid bedroom number (1-10)"
            isValid = false
        } else {
            binding.tilBedroomNumber.error = null
        }

        // 验证浴室数量
        val bathroomNumber = binding.etBathroomNumber.text.toString().toIntOrNull()
        if (bathroomNumber == null || bathroomNumber < 1 || bathroomNumber > 5) {
            binding.tilBathroomNumber.error = "Please enter valid bathroom number (1-5)"
            isValid = false
        } else {
            binding.tilBathroomNumber.error = null
        }

        // 验证楼栋
        if (binding.etBlock.text.isNullOrBlank()) {
            binding.tilBlock.error = "Please enter block number"
            isValid = false
        } else {
            binding.tilBlock.error = null
        }

        // 验证街道名称
        if (binding.etStreetName.text.isNullOrBlank()) {
            binding.tilStreetName.error = "Please enter street name"
            isValid = false
        } else {
            binding.tilStreetName.error = null
        }

        // 验证楼层
        if (binding.etStorey.text.isNullOrBlank()) {
            binding.tilStorey.error = "Please enter storey information"
            isValid = false
        } else {
            binding.tilStorey.error = null
        }

        // 验证建筑面积
        val floorArea = binding.etFloorAreaSqm.text.toString().toFloatOrNull()
        if (floorArea == null || floorArea < 20 || floorArea > 200) {
            binding.tilFloorAreaSqm.error = "Please enter valid floor area (20-200 sqm)"
            isValid = false
        } else {
            binding.tilFloorAreaSqm.error = null
        }

        // 验证建成年份
        if (selectedYear.isBlank()) {
            binding.tilTopYear.error = "Please select construction year"
            isValid = false
        } else {
            binding.tilTopYear.error = null
        }

        // 验证公寓模型
        if (selectedFlatModel.isBlank()) {
            binding.tilFlatModel.error = "Please select flat model"
            isValid = false
        } else {
            binding.tilFlatModel.error = null
        }

        // 验证价格
        if (!validatePrice()) {
            isValid = false
        }

        return isValid
    }

    private fun validatePostalCode(): Boolean {
        val postalCode = binding.etPostalCode.text.toString()
        val postalCodePattern = Regex("^[0-9]{6}$")
        
        return if (postalCodePattern.matches(postalCode)) {
            binding.tilPostalCode.error = null
            true
        } else {
            binding.tilPostalCode.error = "Please enter 6-digit postal code"
            false
        }
    }

    private fun validatePrice(): Boolean {
        val price = binding.etResalePrice.text.toString().toFloatOrNull()
        
        return if (price != null && price >= 100000 && price <= 2000000) {
            binding.tilResalePrice.error = null
            true
        } else {
            binding.tilResalePrice.error = "Please enter valid price (100k-2M SGD)"
            false
        }
    }

    private fun submitProperty() {
        binding.btnSubmit.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        val propertyRequest = PropertyRequest(
            listingTitle = binding.etListingTitle.text.toString(),
            sellerId = 101L, // 临时使用固定值，后续从登录信息获取
            town = selectedTown,
            postalCode = binding.etPostalCode.text.toString(),
            bedroomNumber = binding.etBedroomNumber.text.toString().toInt(),
            bathroomNumber = binding.etBathroomNumber.text.toString().toInt(),
            block = binding.etBlock.text.toString(),
            streetName = binding.etStreetName.text.toString(),
            storey = binding.etStorey.text.toString(),
            floorAreaSqm = binding.etFloorAreaSqm.text.toString().toFloat(),
            topYear = selectedYear.toInt(),
            flatModel = selectedFlatModel,
            resalePrice = binding.etResalePrice.text.toString().toFloat(),
            status = "available"
        )

        lifecycleScope.launch {
            try {
                val response = propertyApi.createProperty(propertyRequest)
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Property added successfully!", Toast.LENGTH_LONG).show()
                    // 返回上一页
                    findNavController().navigateUp()
                } else {
                    Toast.makeText(requireContext(), "Failed to add property: ${response.message()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Network error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnSubmit.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun clearForm() {
        binding.etListingTitle.text?.clear()
        binding.etTown.text?.clear()
        binding.etPostalCode.text?.clear()
        binding.etBedroomNumber.text?.clear()
        binding.etBathroomNumber.text?.clear()
        binding.etBlock.text?.clear()
        binding.etStreetName.text?.clear()
        binding.etStorey.text?.clear()
        binding.etFloorAreaSqm.text?.clear()
        binding.etTopYear.text?.clear()
        binding.etFlatModel.text?.clear()
        binding.etResalePrice.text?.clear()
        
        selectedTown = ""
        selectedFlatModel = ""
        selectedYear = ""
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 