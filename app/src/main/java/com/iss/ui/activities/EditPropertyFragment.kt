package com.iss.ui.activities

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.iss.R
import com.iss.api.PropertyApi
import com.iss.databinding.FragmentEditPropertyBinding
import com.iss.model.Property
import com.iss.model.PropertyRequest
import com.iss.network.NetworkService
import com.iss.utils.UserManager
import kotlinx.coroutines.launch
import retrofit2.Response

class EditPropertyFragment : Fragment() {

    private var _binding: FragmentEditPropertyBinding? = null
    private val binding get() = _binding!!
    private lateinit var propertyApi: PropertyApi
    private var propertyId: Long = -1
    private var originalProperty: Property? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditPropertyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化UserManager
        UserManager.init(requireContext())

        // 获取要编辑的房源ID
        arguments?.let {
            propertyId = it.getLong("property_id", -1)
        }

        if (propertyId == -1L) {
            Toast.makeText(requireContext(), "Invalid property ID", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        // 初始化API
        propertyApi = NetworkService.propertyApi

        // 加载房源数据
        loadPropertyData()

        // 设置下拉选择框
        setupDropdowns()

        // 设置提交按钮
        binding.btnSubmit.setOnClickListener {
            updateProperty()
        }
    }

    private fun loadPropertyData() {
        lifecycleScope.launch {
            try {
                val response = propertyApi.getPropertyById(propertyId)
                
                if (response.isSuccessful) {
                    originalProperty = response.body()?.data
                    originalProperty?.let { property ->
                        populateForm(property)
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to load property data", Toast.LENGTH_LONG).show()
                    findNavController().navigateUp()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                findNavController().navigateUp()
            }
        }
    }

    private fun populateForm(property: Property) {
        binding.apply {
            etTitle.setText(property.listingTitle)
            etStreetName.setText(property.streetName)
            etBlock.setText(property.block)
            etTown.setText(property.town)
            etPostalCode.setText(property.postalCode)
            etBedroomNumber.setText(property.bedroomNumber.toString())
            etBathroomNumber.setText(property.bathroomNumber.toString())
            etFloorArea.setText(property.floorAreaSqm.toString())
            etConstructionYear.setText(property.topYear.toString())
            etFlatModel.setText(property.flatModel)
            etResalePrice.setText(property.resalePrice.toString())
        }
    }

    private fun setupDropdowns() {
        // Town dropdown
        binding.etTown.setOnClickListener {
            showTownDialog()
        }

        // Construction Year dropdown
        binding.etConstructionYear.setOnClickListener {
            showConstructionYearDialog()
        }

        // Flat Model dropdown
        binding.etFlatModel.setOnClickListener {
            showFlatModelDialog()
        }
    }

    private fun showTownDialog() {
        val towns = arrayOf(
            "Ang Mo Kio", "Bedok", "Bishan", "Boon Lay", "Bukit Batok", "Bukit Merah",
            "Bukit Panjang", "Bukit Timah", "Central Area", "Choa Chu Kang", "Clementi",
            "Geylang", "Hougang", "Jurong East", "Jurong West", "Kallang", "Lim Chu Kang",
            "Marine Parade", "Pasir Ris", "Punggol", "Queenstown", "Sembawang", "Sengkang",
            "Serangoon", "Tampines", "Toa Payoh", "Woodlands", "Yishun"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Select Town")
            .setItems(towns) { _, which ->
                binding.etTown.setText(towns[which])
            }
            .show()
    }

    private fun showConstructionYearDialog() {
        val years = (1990..2024).toList().map { it.toString() }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Select Construction Year")
            .setItems(years) { _, which ->
                binding.etConstructionYear.setText(years[which])
            }
            .show()
    }

    private fun showFlatModelDialog() {
        val models = arrayOf(
            "1-Room", "2-Room", "3-Room", "4-Room", "5-Room", "Executive",
            "Multi-Generation", "Studio Apartment"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Select Flat Model")
            .setItems(models) { _, which ->
                binding.etFlatModel.setText(models[which])
            }
            .show()
    }

    private fun updateProperty() {
        // 验证表单
        if (!validateForm()) {
            return
        }

        // 检查用户是否已登录
        if (!UserManager.isLoggedIn()) {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_LONG).show()
            return
        }
        
        // 创建PropertyRequest对象
        val propertyRequest = PropertyRequest(
            listingTitle = binding.etTitle.text.toString(),
            sellerId = UserManager.getCurrentUserId(), // 使用当前登录用户的ID
            town = binding.etTown.text.toString(),
            postalCode = binding.etPostalCode.text.toString(),
            bedroomNumber = binding.etBedroomNumber.text.toString().toInt(),
            bathroomNumber = binding.etBathroomNumber.text.toString().toInt(),
            block = binding.etBlock.text.toString(),
            streetName = binding.etStreetName.text.toString(),
            storey = "15th Floor", // 默认值，因为表单中没有这个字段
            floorAreaSqm = binding.etFloorArea.text.toString().toFloat(),
            topYear = binding.etConstructionYear.text.toString().toInt(),
            flatModel = binding.etFlatModel.text.toString(),
            resalePrice = binding.etResalePrice.text.toString().toFloat()
        )

        // 调用更新API
        lifecycleScope.launch {
            try {
                val response = propertyApi.updateProperty(propertyId, propertyRequest)
                
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Property updated successfully", Toast.LENGTH_LONG).show()
                    findNavController().navigateUp()
                } else {
                    Toast.makeText(requireContext(), "Failed to update property", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Network error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun validateForm(): Boolean {
        val title = binding.etTitle.text.toString().trim()
        val streetName = binding.etStreetName.text.toString().trim()
        val block = binding.etBlock.text.toString().trim()
        val town = binding.etTown.text.toString().trim()
        val postalCode = binding.etPostalCode.text.toString().trim()
        val bedroomNumber = binding.etBedroomNumber.text.toString().trim()
        val bathroomNumber = binding.etBathroomNumber.text.toString().trim()
        val floorArea = binding.etFloorArea.text.toString().trim()
        val constructionYear = binding.etConstructionYear.text.toString().trim()
        val flatModel = binding.etFlatModel.text.toString().trim()
        val resalePrice = binding.etResalePrice.text.toString().trim()

        if (title.isEmpty()) {
            binding.etTitle.error = "Property title is required"
            return false
        }

        if (streetName.isEmpty()) {
            binding.etStreetName.error = "Street name is required"
            return false
        }

        if (block.isEmpty()) {
            binding.etBlock.error = "Block/Storey is required"
            return false
        }

        if (town.isEmpty()) {
            binding.etTown.error = "Town is required"
            return false
        }

        if (postalCode.isEmpty() || postalCode.length != 6) {
            binding.etPostalCode.error = "Valid postal code is required"
            return false
        }

        if (bedroomNumber.isEmpty()) {
            binding.etBedroomNumber.error = "Bedroom number is required"
            return false
        }

        if (bathroomNumber.isEmpty()) {
            binding.etBathroomNumber.error = "Bathroom number is required"
            return false
        }

        if (floorArea.isEmpty()) {
            binding.etFloorArea.error = "Floor area is required"
            return false
        }

        if (constructionYear.isEmpty()) {
            binding.etConstructionYear.error = "Construction year is required"
            return false
        }

        if (flatModel.isEmpty()) {
            binding.etFlatModel.error = "Flat model is required"
            return false
        }

        if (resalePrice.isEmpty()) {
            binding.etResalePrice.error = "Resale price is required"
            return false
        }

        // 验证数字字段
        try {
            val bedroom = bedroomNumber.toInt()
            val bathroom = bathroomNumber.toInt()
            val area = floorArea.toFloat()
            val year = constructionYear.toInt()
            val price = resalePrice.toFloat()

            if (bedroom < 1 || bedroom > 10) {
                binding.etBedroomNumber.error = "Bedroom number must be between 1 and 10"
                return false
            }

            if (bathroom < 1 || bathroom > 5) {
                binding.etBathroomNumber.error = "Bathroom number must be between 1 and 5"
                return false
            }

            if (area < 20 || area > 500) {
                binding.etFloorArea.error = "Floor area must be between 20 and 500 sqm"
                return false
            }

            if (year < 1990 || year > 2024) {
                binding.etConstructionYear.error = "Construction year must be between 1990 and 2024"
                return false
            }

            if (price < 100000 || price > 2000000) {
                binding.etResalePrice.error = "Resale price must be between 100,000 and 2,000,000"
                return false
            }

        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 