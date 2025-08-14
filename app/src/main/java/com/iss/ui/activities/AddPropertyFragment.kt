package com.iss.ui.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.iss.PredActivity
import com.iss.api.PropertyApi
import com.iss.databinding.FragmentAddPropertyBinding
import com.iss.network.NetworkService
import com.iss.ui.adapters.SelectedImageAdapter
import com.iss.utils.UserManager
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.InputStream
import java.time.LocalDateTime
import com.google.android.material.textfield.TextInputLayout

class AddPropertyFragment : Fragment() {

    private var _binding: FragmentAddPropertyBinding? = null
    private val binding get() = _binding!!
    private lateinit var propertyApi: PropertyApi
    private lateinit var selectedImageAdapter: SelectedImageAdapter
    private val selectedImages = mutableListOf<String>()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(requireContext(), "Permission denied. Please grant storage permission in Settings to select images.", Toast.LENGTH_LONG).show()
        }
    }

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

        UserManager.init(requireContext())
        propertyApi = NetworkService.propertyApi

        setupDropdowns()
        setupFormValidation()
        setupImageUpload()
        setupSubmitButton()

        // 为预测按钮设置点击事件
        binding.btnPredicate.setOnClickListener {
            if (validateFormForPrediction()) {
                sendDataToPredictionActivity()
            }
        }
    }

    private fun setupDropdowns() {
        binding.etTown.setOnClickListener {
            showTownDialog()
        }
        binding.etFlatModel.setOnClickListener {
            showFlatModelDialog()
        }
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
        binding.tilPostalCode.setEndIconOnClickListener {
            validatePostalCode()
        }
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

    private fun setupImageUpload() {
        selectedImageAdapter = SelectedImageAdapter(
            selectedImages = emptyList(),
            onImageRemoved = { position ->
                selectedImages.removeAt(position)
                selectedImageAdapter.removeImage(position)
                updateImageCount()
                updateAddImageButton()
            }
        )
        binding.rvSelectedImages.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = selectedImageAdapter
        }
        binding.btnAddImage.setOnClickListener {
            checkPermissionAndPickImage()
        }
    }

    private fun checkPermissionAndPickImage() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            openImagePicker()
        } else {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    openImagePicker()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                    showPermissionRationaleDialog()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
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

    private fun openImagePicker() {
        imagePickerLauncher.launch("image/*")
    }

    private fun updateImageCount() {
        if (selectedImages.isNotEmpty()) {
            binding.tvImageCount.text = "${selectedImages.size}/10 images selected"
            binding.tvImageCount.visibility = View.VISIBLE
            binding.rvSelectedImages.visibility = View.VISIBLE
        } else {
            binding.tvImageCount.visibility = View.GONE
            binding.rvSelectedImages.visibility = View.GONE
        }
    }

    private fun updateAddImageButton() {
        if (selectedImages.size >= 10) {
            binding.btnAddImage.text = "Maximum Images Reached"
            binding.btnAddImage.isEnabled = false
        } else {
            binding.btnAddImage.text = "Add Images (${selectedImages.size}/10)"
            binding.btnAddImage.isEnabled = true
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true
        if (binding.etListingTitle.text.isNullOrBlank()) {
            binding.tilListingTitle.error = "Please enter listing title"
            isValid = false
        } else {
            binding.tilListingTitle.error = null
        }
        if (selectedTown.isBlank()) {
            binding.tilTown.error = "Please select a town"
            isValid = false
        } else {
            binding.tilTown.error = null
        }
        if (!validatePostalCode()) {
            isValid = false
        }
        val bedroomNumber = binding.etBedroomNumber.text.toString().toIntOrNull()
        if (bedroomNumber == null || bedroomNumber < 1 || bedroomNumber > 10) {
            binding.tilBedroomNumber.error = "Please enter valid bedroom number (1-10)"
            isValid = false
        } else {
            binding.tilBedroomNumber.error = null
        }
        val bathroomNumber = binding.etBathroomNumber.text.toString().toIntOrNull()
        if (bathroomNumber == null || bathroomNumber < 1 || bathroomNumber > 5) {
            binding.tilBathroomNumber.error = "Please enter valid bathroom number (1-5)"
            isValid = false
        } else {
            binding.tilBathroomNumber.error = null
        }
        if (binding.etBlock.text.isNullOrBlank()) {
            binding.tilBlock.error = "Please enter block number"
            isValid = false
        } else {
            binding.tilBlock.error = null
        }
        if (binding.etStreetName.text.isNullOrBlank()) {
            binding.tilStreetName.error = "Please enter street name"
            isValid = false
        } else {
            binding.tilStreetName.error = null
        }
        if (binding.etStorey.text.isNullOrBlank()) {
            binding.tilStorey.error = "Please enter storey information"
            isValid = false
        } else {
            binding.tilStorey.error = null
        }
        val floorArea = binding.etFloorAreaSqm.text.toString().toFloatOrNull()
        if (floorArea == null || floorArea < 20 || floorArea > 200) {
            binding.tilFloorAreaSqm.error = "Please enter valid floor area (20-200 sqm)"
            isValid = false
        } else {
            binding.tilFloorAreaSqm.error = null
        }
        if (selectedYear.isBlank()) {
            binding.tilTopYear.error = "Please select construction year"
            isValid = false
        } else {
            binding.tilTopYear.error = null
        }
        if (selectedFlatModel.isBlank()) {
            binding.tilFlatModel.error = "Please select flat model"
            isValid = false
        } else {
            binding.tilFlatModel.error = null
        }
        if (!validatePrice()) {
            isValid = false
        }
        return isValid
    }

    private fun validateFormForPrediction(): Boolean {
        var isValid = true

        val fieldsToValidate = listOf(
            binding.etFloorAreaSqm,
            binding.etBedroomNumber,
            binding.etTopYear
        )

        for (field in fieldsToValidate) {
            if (field.text.isNullOrBlank()) {
                val til = (field.parent as? TextInputLayout)
                til?.error = "This field is required for prediction"
                isValid = false
            } else {
                (field.parent as? TextInputLayout)?.error = null
            }
        }

        if (selectedTown.isBlank()) {
            binding.tilTown.error = "Please select a town"
            isValid = false
        } else {
            binding.tilTown.error = null
        }
        if (selectedFlatModel.isBlank()) {
            binding.tilFlatModel.error = "Please select flat model"
            isValid = false
        } else {
            binding.tilFlatModel.error = null
        }
        if (binding.etStorey.text.isNullOrBlank()) {
            binding.tilStorey.error = "Please enter storey information"
            isValid = false
        } else {
            binding.tilStorey.error = null
        }

        return isValid
    }

    private fun validatePostalCode(): Boolean {
        val postalCode = binding.etPostalCode.text.toString()
        val postalCodePattern = Regex("^[0-9]{6}$")
        if (postalCodePattern.matches(postalCode)) {
            binding.tilPostalCode.error = null
            return true
        } else {
            binding.tilPostalCode.error = "Please enter 6-digit postal code"
            return false
        }
    }

    private fun validatePrice(): Boolean {
        val price = binding.etResalePrice.text.toString().toFloatOrNull()
        if (price != null && price >= 100000 && price <= 2000000) {
            binding.tilResalePrice.error = null
            return true
        } else {
            binding.tilResalePrice.error = "Please enter valid price (100k-2M SGD)"
            return false
        }
    }

    private fun sendDataToPredictionActivity() {
        val intent = Intent(requireContext(), PredActivity::class.java).apply {
            putExtra("floorAreaSqm", binding.etFloorAreaSqm.text.toString().toFloatOrNull() ?: 0f)
            putExtra("town", selectedTown)
            putExtra("flatModel", selectedFlatModel)
            putExtra("storey", binding.etStorey.text.toString())
            putExtra("topYear", selectedYear.toIntOrNull() ?: 0)

            val bedroomNumber = binding.etBedroomNumber.text.toString().toIntOrNull() ?: 0
            val flatType = when (bedroomNumber) {
                1 -> "1 ROOM"
                2 -> "2 ROOM"
                3 -> "3 ROOM"
                4 -> "4 ROOM"
                5 -> "5 ROOM"
                else -> ""
            }
            putExtra("flatType", flatType)

            val currentMonth = LocalDateTime.now().toString().substring(0, 7)
            putExtra("month", currentMonth)
        }

        // --- 核心修正：添加这行代码来启动 Activity ---
        startActivity(intent)
    }

    private fun submitProperty() {
        binding.btnSubmit.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        if (!UserManager.isLoggedIn()) {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_LONG).show()
            binding.btnSubmit.isEnabled = true
            binding.progressBar.visibility = View.GONE
            return
        }

        lifecycleScope.launch {
            try {
                val multipartBuilder = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("listingTitle", binding.etListingTitle.text.toString())
                    .addFormDataPart("sellerId", UserManager.getCurrentUserId().toString())
                    .addFormDataPart("town", selectedTown)
                    .addFormDataPart("postalCode", binding.etPostalCode.text.toString())
                    .addFormDataPart("bedroomNumber", binding.etBedroomNumber.text.toString())
                    .addFormDataPart("bathroomNumber", binding.etBathroomNumber.text.toString())
                    .addFormDataPart("block", binding.etBlock.text.toString())
                    .addFormDataPart("streetName", binding.etStreetName.text.toString())
                    .addFormDataPart("storey", binding.etStorey.text.toString())
                    .addFormDataPart("floorAreaSqm", binding.etFloorAreaSqm.text.toString())
                    .addFormDataPart("topYear", selectedYear.toString())
                    .addFormDataPart("flatModel", selectedFlatModel)
                    .addFormDataPart("resalePrice", binding.etResalePrice.text.toString())
                    .addFormDataPart("status", "pending")

                for (imageUri in selectedImages) {
                    val uri = Uri.parse(imageUri)
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    val file = createTempFileFromInputStream(inputStream, "image_${System.currentTimeMillis()}")
                    val requestBody = RequestBody.create("image/*".toMediaTypeOrNull(), file)
                    multipartBuilder.addFormDataPart("imageFiles", file.name, requestBody)
                }

                val multipartBody = multipartBuilder.build()
                val response = propertyApi.createPropertyWithImages(multipartBody)

                if (response.isSuccessful) {
                    val createdProperty = response.body()?.data
                    if (createdProperty != null) {
                        Toast.makeText(requireContext(), "Property added successfully!", Toast.LENGTH_LONG).show()
                        findNavController().navigateUp()
                    } else {
                        Toast.makeText(requireContext(), "Failed to add property", Toast.LENGTH_LONG).show()
                    }
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

    private fun createTempFileFromInputStream(inputStream: InputStream?, fileName: String): File {
        val file = File.createTempFile(fileName, ".jpg")
        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file
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