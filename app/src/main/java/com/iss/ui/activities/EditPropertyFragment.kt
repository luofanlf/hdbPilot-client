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
import androidx.recyclerview.widget.LinearLayoutManager
import com.iss.R
import com.iss.api.PropertyApi
import com.iss.databinding.FragmentEditPropertyBinding
import com.iss.model.Property
import com.iss.model.PropertyRequest
import com.iss.model.PropertyImage
import com.iss.network.NetworkService
import com.iss.ui.adapters.SelectedImageAdapter
import com.iss.ui.adapters.ExistingImageAdapter
import com.iss.utils.UserManager
import kotlinx.coroutines.launch
import retrofit2.Response
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.InputStream

class EditPropertyFragment : Fragment() {

    private var _binding: FragmentEditPropertyBinding? = null
    private val binding get() = _binding!!
    private lateinit var propertyApi: PropertyApi
    private lateinit var selectedImageAdapter: SelectedImageAdapter
    private lateinit var existingImageAdapter: ExistingImageAdapter
    private var propertyId: Long = -1
    private var originalProperty: Property? = null
    private val selectedImages = mutableListOf<String>()
    private val existingImages = mutableListOf<PropertyImage>()
    
    // 图片选择权限请求
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 图片选择结果
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val imagePath = selectedUri.toString()
            if (selectedImages.size < 10) {
                selectedImages.add(imagePath)
                selectedImageAdapter.addImage(imagePath)
                updateImageCount()
                updateAddImageButton()
            } else {
                Toast.makeText(requireContext(), "Maximum 10 images allowed", Toast.LENGTH_SHORT).show()
            }
        }
    }

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

        // 初始化图片管理
        setupImageManagement()

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
            etStatus.setText(property.status) // 填充状态
        }
    }

    private fun setupDropdowns() {
        // 设置城镇下拉选择
        binding.etTown.setOnClickListener {
            showTownDialog()
        }

        // 设置建筑年份下拉选择
        binding.etConstructionYear.setOnClickListener {
            showYearDialog()
        }

        // 设置公寓模型下拉选择
        binding.etFlatModel.setOnClickListener {
            showFlatModelDialog()
        }

        // 设置状态下拉选择
        binding.etStatus.setOnClickListener {
            showStatusDialog()
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

    private fun showYearDialog() {
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
            "2-ROOM", "3GEN", "ADJOINED FLAT", "APARTMENT", "DBSS", "IMPROVED", 
            "IMPROVED-MAISONETTE", "MAISONETTE", "MODEL A", "MODEL A-MAISONETTE", 
            "MODEL A2", "MULTI GENERATION", "NEW GENERATION", "PREMIUM APARTMENT", 
            "PREMIUM APARTMENT LOFT", "PREMIUM MAISONETTE", "SIMPLIFIED", "STANDARD", 
            "TERRACE", "TYPE S1", "TYPE S2"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Select Flat Model")
            .setItems(models) { _, which ->
                binding.etFlatModel.setText(models[which])
            }
            .show()
    }

    private fun showStatusDialog() {
        val statusOptions = arrayOf("Available", "Sold")
        val currentStatus = binding.etStatus.text.toString()
        val currentIndex = statusOptions.indexOf(currentStatus)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Select Property Status")
            .setSingleChoiceItems(statusOptions, currentIndex) { _, which ->
                binding.etStatus.setText(statusOptions[which])
            }
            .setPositiveButton("OK", null)
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
        
        // 获取当前表单值
        val currentTitle = binding.etTitle.text.toString().trim()
        val currentStreetName = binding.etStreetName.text.toString().trim()
        val currentBlock = binding.etBlock.text.toString().trim()
        val currentTown = binding.etTown.text.toString().trim()
        val currentPostalCode = binding.etPostalCode.text.toString().trim()
        val currentBedroomNumber = binding.etBedroomNumber.text.toString().trim()
        val currentBathroomNumber = binding.etBathroomNumber.text.toString().trim()
        val currentFloorArea = binding.etFloorArea.text.toString().trim()
        val currentConstructionYear = binding.etConstructionYear.text.toString().trim()
        val currentFlatModel = binding.etFlatModel.text.toString().trim()
        val currentResalePrice = binding.etResalePrice.text.toString().trim()
        val currentStatus = binding.etStatus.text.toString().trim()

        // 获取原始值
        val originalTitle = originalProperty?.listingTitle ?: ""
        val originalStreetName = originalProperty?.streetName ?: ""
        val originalBlock = originalProperty?.block ?: ""
        val originalTown = originalProperty?.town ?: ""
        val originalPostalCode = originalProperty?.postalCode ?: ""
        val originalBedroomNumber = originalProperty?.bedroomNumber?.toString() ?: ""
        val originalBathroomNumber = originalProperty?.bathroomNumber?.toString() ?: ""
        val originalFloorArea = originalProperty?.floorAreaSqm?.toString() ?: ""
        val originalConstructionYear = originalProperty?.topYear?.toString() ?: ""
        val originalFlatModel = originalProperty?.flatModel ?: ""
        val originalResalePrice = originalProperty?.resalePrice?.toString() ?: ""
        val originalStatus = originalProperty?.status ?: ""

        // 创建PropertyRequest对象，只包含被修改的字段
        val propertyRequest = PropertyRequest(
            listingTitle = if (currentTitle != originalTitle) currentTitle else originalTitle,
            sellerId = UserManager.getCurrentUserId(),
            town = if (currentTown != originalTown) currentTown else originalTown,
            postalCode = if (currentPostalCode != originalPostalCode) currentPostalCode else originalPostalCode,
            bedroomNumber = if (currentBedroomNumber != originalBedroomNumber) currentBedroomNumber.toInt() else (originalProperty?.bedroomNumber ?: 0),
            bathroomNumber = if (currentBathroomNumber != originalBathroomNumber) currentBathroomNumber.toInt() else (originalProperty?.bathroomNumber ?: 0),
            block = if (currentBlock != originalBlock) currentBlock else originalBlock,
            streetName = if (currentStreetName != originalStreetName) currentStreetName else originalStreetName,
            storey = originalProperty?.storey ?: "15th Floor", // 保持原始值
            floorAreaSqm = if (currentFloorArea != originalFloorArea) currentFloorArea.toFloat() else (originalProperty?.floorAreaSqm ?: 0f),
            topYear = if (currentConstructionYear != originalConstructionYear) currentConstructionYear.toInt() else (originalProperty?.topYear ?: 0),
            flatModel = if (currentFlatModel != originalFlatModel) currentFlatModel else originalFlatModel,
            resalePrice = if (currentResalePrice != originalResalePrice) currentResalePrice.toFloat() else (originalProperty?.resalePrice ?: 0f),
            status = if (currentStatus != originalStatus) currentStatus else originalStatus
        )

        // 调用更新API
        lifecycleScope.launch {
            try {
                val response = propertyApi.updateProperty(propertyId, propertyRequest)
                
                if (response.isSuccessful) {
                    // 上传新选择的图片
                    if (selectedImages.isNotEmpty()) {
                        uploadNewImages()
                    } else {
                        Toast.makeText(requireContext(), "Property updated successfully", Toast.LENGTH_LONG).show()
                        findNavController().navigateUp()
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to update property", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Network error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun uploadNewImages() {
        var uploadedCount = 0
        var failedCount = 0

        for (imageUri in selectedImages) {
            try {
                val uri = Uri.parse(imageUri)
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val file = createTempFileFromInputStream(inputStream, "image_${System.currentTimeMillis()}")
                
                val requestBody = RequestBody.create("image/*".toMediaTypeOrNull(), file)
                val multipartBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("imageFile", file.name, requestBody)
                    .build()

                val response = propertyApi.addPropertyImage(propertyId, multipartBody)
                
                if (response.isSuccessful) {
                    uploadedCount++
                    println("Image uploaded successfully: $imageUri")
                } else {
                    failedCount++
                    println("Image upload failed: $imageUri, error code: ${response.code()}, error message: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                failedCount++
                println("Image upload exception: $imageUri, exception message: ${e.message}")
                e.printStackTrace()
            }
        }

        val message = when {
            uploadedCount > 0 && failedCount == 0 -> "Property updated and $uploadedCount images uploaded successfully"
            uploadedCount > 0 && failedCount > 0 -> "Property updated and $uploadedCount images uploaded, $failedCount failed"
            uploadedCount == 0 && failedCount > 0 -> "Property updated but image upload failed"
            else -> "Property updated successfully"
        }

        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        findNavController().navigateUp()
    }

    private fun createTempFileFromInputStream(inputStream: InputStream?, fileName: String): File {
        val file = File.createTempFile(fileName, ".jpg")
        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    private fun validateForm(): Boolean {
        // 获取当前表单值
        val currentTitle = binding.etTitle.text.toString().trim()
        val currentStreetName = binding.etStreetName.text.toString().trim()
        val currentBlock = binding.etBlock.text.toString().trim()
        val currentTown = binding.etTown.text.toString().trim()
        val currentPostalCode = binding.etPostalCode.text.toString().trim()
        val currentBedroomNumber = binding.etBedroomNumber.text.toString().trim()
        val currentBathroomNumber = binding.etBathroomNumber.text.toString().trim()
        val currentFloorArea = binding.etFloorArea.text.toString().trim()
        val currentConstructionYear = binding.etConstructionYear.text.toString().trim()
        val currentFlatModel = binding.etFlatModel.text.toString().trim()
        val currentResalePrice = binding.etResalePrice.text.toString().trim()
        val currentStatus = binding.etStatus.text.toString().trim()

        // 获取原始值
        val originalTitle = originalProperty?.listingTitle ?: ""
        val originalStreetName = originalProperty?.streetName ?: ""
        val originalBlock = originalProperty?.block ?: ""
        val originalTown = originalProperty?.town ?: ""
        val originalPostalCode = originalProperty?.postalCode ?: ""
        val originalBedroomNumber = originalProperty?.bedroomNumber?.toString() ?: ""
        val originalBathroomNumber = originalProperty?.bathroomNumber?.toString() ?: ""
        val originalFloorArea = originalProperty?.floorAreaSqm?.toString() ?: ""
        val originalConstructionYear = originalProperty?.topYear?.toString() ?: ""
        val originalFlatModel = originalProperty?.flatModel ?: ""
        val originalResalePrice = originalProperty?.resalePrice?.toString() ?: ""
        val originalStatus = originalProperty?.status ?: ""

        // 检查哪些字段被修改了
        val titleChanged = currentTitle != originalTitle
        val streetNameChanged = currentStreetName != originalStreetName
        val blockChanged = currentBlock != originalBlock
        val townChanged = currentTown != originalTown
        val postalCodeChanged = currentPostalCode != originalPostalCode
        val bedroomNumberChanged = currentBedroomNumber != originalBedroomNumber
        val bathroomNumberChanged = currentBathroomNumber != originalBathroomNumber
        val floorAreaChanged = currentFloorArea != originalFloorArea
        val constructionYearChanged = currentConstructionYear != originalConstructionYear
        val flatModelChanged = currentFlatModel != originalFlatModel
        val resalePriceChanged = currentResalePrice != originalResalePrice
        val statusChanged = currentStatus != originalStatus

        // 如果没有字段被修改，提示用户
        if (!titleChanged && !streetNameChanged && !blockChanged && !townChanged && 
            !postalCodeChanged && !bedroomNumberChanged && !bathroomNumberChanged && 
            !floorAreaChanged && !constructionYearChanged && !flatModelChanged && 
            !resalePriceChanged && !statusChanged) {
            Toast.makeText(requireContext(), "No changes detected", Toast.LENGTH_SHORT).show()
            return false
        }

        // 验证被修改的字段
        if (titleChanged && currentTitle.isEmpty()) {
            binding.etTitle.error = "Property title is required"
            return false
        }

        if (streetNameChanged && currentStreetName.isEmpty()) {
            binding.etStreetName.error = "Street name is required"
            return false
        }

        if (blockChanged && currentBlock.isEmpty()) {
            binding.etBlock.error = "Block/Storey is required"
            return false
        }

        if (townChanged && currentTown.isEmpty()) {
            binding.etTown.error = "Town is required"
            return false
        }

        if (postalCodeChanged && (currentPostalCode.isEmpty() || currentPostalCode.length != 6)) {
            binding.etPostalCode.error = "Valid postal code is required"
            return false
        }

        if (bedroomNumberChanged && currentBedroomNumber.isEmpty()) {
            binding.etBedroomNumber.error = "Bedroom number is required"
            return false
        }

        if (bathroomNumberChanged && currentBathroomNumber.isEmpty()) {
            binding.etBathroomNumber.error = "Bathroom number is required"
            return false
        }

        if (floorAreaChanged && currentFloorArea.isEmpty()) {
            binding.etFloorArea.error = "Floor area is required"
            return false
        }

        if (constructionYearChanged && currentConstructionYear.isEmpty()) {
            binding.etConstructionYear.error = "Construction year is required"
            return false
        }

        if (flatModelChanged && currentFlatModel.isEmpty()) {
            binding.etFlatModel.error = "Flat model is required"
            return false
        }

        if (resalePriceChanged && currentResalePrice.isEmpty()) {
            binding.etResalePrice.error = "Resale price is required"
            return false
        }

        if (statusChanged && currentStatus.isEmpty()) {
            binding.etStatus.error = "Property status is required"
            return false
        }

        // 验证被修改的数字字段
        try {
            if (bedroomNumberChanged) {
                val bedroom = currentBedroomNumber.toInt()
                if (bedroom < 1 || bedroom > 10) {
                    binding.etBedroomNumber.error = "Bedroom number must be between 1 and 10"
                    return false
                }
            }

            if (bathroomNumberChanged) {
                val bathroom = currentBathroomNumber.toInt()
                if (bathroom < 1 || bathroom > 5) {
                    binding.etBathroomNumber.error = "Bathroom number must be between 1 and 5"
                    return false
                }
            }

            if (floorAreaChanged) {
                val area = currentFloorArea.toFloat()
                if (area < 20 || area > 500) {
                    binding.etFloorArea.error = "Floor area must be between 20 and 500 sqm"
                    return false
                }
            }

            if (constructionYearChanged) {
                val year = currentConstructionYear.toInt()
                if (year < 1990 || year > 2024) {
                    binding.etConstructionYear.error = "Construction year must be between 1990 and 2024"
                    return false
                }
            }

            if (resalePriceChanged) {
                val price = currentResalePrice.toFloat()
                if (price < 100000 || price > 2000000) {
                    binding.etResalePrice.error = "Resale price must be between 100,000 and 2,000,000"
                    return false
                }
            }

        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun setupImageManagement() {
        // 初始化新选择的图片适配器
        selectedImageAdapter = SelectedImageAdapter(
            onImageRemoved = { position ->
                selectedImages.removeAt(position)
                selectedImageAdapter.removeImage(position)
                updateImageCount()
                updateAddImageButton()
            }
        )

        // 初始化现有图片适配器
        existingImageAdapter = ExistingImageAdapter(
            onImageDeleted = { propertyImage ->
                deleteExistingImage(propertyImage)
            }
        )

        // 设置新选择图片的RecyclerView
        binding.rvSelectedImages.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = selectedImageAdapter
        }

        // 设置现有图片的RecyclerView
        binding.rvExistingImages.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = existingImageAdapter
        }

        // 设置添加图片按钮
        binding.btnAddImage.setOnClickListener {
            checkPermissionAndPickImage()
        }

        // 加载现有图片
        loadExistingImages()
    }

    private fun loadExistingImages() {
        lifecycleScope.launch {
            try {
                val response = propertyApi.getPropertyImages(propertyId)
                if (response.isSuccessful) {
                    val images = response.body()?.data
                    if (!images.isNullOrEmpty()) {
                        existingImages.clear()
                        existingImages.addAll(images)
                        existingImageAdapter.updateImages(existingImages)
                        binding.rvExistingImages.visibility = View.VISIBLE
                        updateImageCount()
                    } else {
                        binding.rvExistingImages.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load images: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteExistingImage(propertyImage: PropertyImage) {
        println("Starting to delete image: ${propertyImage.id}")
        lifecycleScope.launch {
            try {
                val response = propertyApi.deletePropertyImage(propertyImage.id)
                if (response.isSuccessful) {
                    println("Backend deletion successful, updating UI")
                    // Remove from adapter (this will update the adapter's internal list)
                    existingImageAdapter.removeImage(propertyImage)
                    
                    // Synchronize local list update
                    val removedCount = existingImages.removeAll { it.id == propertyImage.id }
                    println("Removed from local list: $removedCount items")
                    
                    updateImageCount()
                    updateAddImageButton()
                    
                    // 如果没有现有图片了，隐藏RecyclerView
                    if (existingImages.isEmpty()) {
                        binding.rvExistingImages.visibility = View.GONE
                    }
                    
                    Toast.makeText(requireContext(), "Image deleted successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Failed to delete image", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissionAndPickImage() {
        // 对于Android 13及以上版本，直接打开图片选择器，不需要特殊权限
        // 对于较低版本，检查READ_EXTERNAL_STORAGE权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13及以上版本，直接打开图片选择器
            openImagePicker()
        } else {
            // Android 13以下版本，需要检查READ_EXTERNAL_STORAGE权限
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    openImagePicker()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                    // 显示权限说明对话框
                    showPermissionRationaleDialog()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }

    private fun openImagePicker() {
        imagePickerLauncher.launch("image/*")
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Required")
            .setMessage("This app needs access to your photos to allow you to select images for your property listing.")
            .setPositiveButton("Grant Permission") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            .setNegativeButton("Cancel") { _, _ ->
                Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun updateImageCount() {
        val totalImages = existingImages.size + selectedImages.size
        binding.tvImageCount.text = "$totalImages/10 images"
        
        // 更新新选择图片的显示
        if (selectedImages.isNotEmpty()) {
            binding.rvSelectedImages.visibility = View.VISIBLE
        } else {
            binding.rvSelectedImages.visibility = View.GONE
        }
    }

    private fun updateAddImageButton() {
        val totalImages = existingImages.size + selectedImages.size
        binding.btnAddImage.isEnabled = totalImages < 10
        if (totalImages >= 10) {
            binding.btnAddImage.text = "Max Images Reached"
        } else {
            binding.btnAddImage.text = "Add Image"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 