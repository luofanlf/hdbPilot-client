package com.iss.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.maps.android.clustering.ClusterManager
import com.iss.R
import com.iss.adapter.PropertyAdapter
import com.iss.databinding.DialogPropertyListBinding
import com.iss.databinding.FragmentMapBinding
import com.iss.model.Property
import com.iss.repository.PropertyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var googleMap: GoogleMap
    private lateinit var clusterManager: ClusterManager<PropertyClusterItem>
    private val propertyList = mutableListOf<Property>()
    private val propertyRepository = PropertyRepository()
    private var highlightPropertyId: Long? = null

    // In-memory cache to avoid duplicate Geocode requests
    private val geocodeCache = mutableMapOf<String, LatLng>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        highlightPropertyId = arguments?.getLong("highlight_property_id")?.takeIf { it != 0L }
    }
    private fun safeZoom(action: () -> Unit) {
        if (::googleMap.isInitialized)
            action()
        else
            showError("Map not ready")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        binding.zoomIn.setOnClickListener { safeZoom { googleMap.animateCamera(CameraUpdateFactory.zoomIn()) } }
        binding.zoomOut.setOnClickListener { safeZoom { googleMap.animateCamera(CameraUpdateFactory.zoomOut()) } }
        binding.myLocationButton.setOnClickListener { checkLocationPermissionAndMove() }

        return binding.root
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isMyLocationButtonEnabled = false
        setupCluster()
        loadPropertyMarkers()
    }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) moveToMyLocation()
            else showError("Location permission denied")
        }

    private fun checkLocationPermissionAndMove() {
        val permissionStatus = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            moveToMyLocation()
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            showError("Location permission needed")
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun setupCluster() {
        clusterManager = ClusterManager(requireContext(), googleMap)
        clusterManager.renderer = MapMarkerClusterRenderer(requireContext(), googleMap, clusterManager)

        googleMap.setOnCameraIdleListener(clusterManager)
        googleMap.setOnMarkerClickListener(clusterManager)

        clusterManager.setOnClusterClickListener { cluster ->
            showClusterPropertyListDialog(cluster.items.map { it.property })
            true
        }

        clusterManager.setOnClusterItemClickListener { item ->
            navigateToPropertyDetail(item.property.id)
            true
        }
    }

    private fun navigateToPropertyDetail(propertyId: Long) {
        findNavController().navigate(
            R.id.action_mapFragment_to_propertyDetailFragment,
            Bundle().apply { putLong("property_id", propertyId) }
        )
    }

    private fun showClusterPropertyListDialog(properties: List<Property>) {
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogPropertyListBinding.inflate(layoutInflater)
        dialogBinding.rvDialogListings.layoutManager = LinearLayoutManager(requireContext())
        dialogBinding.rvDialogListings.adapter = PropertyAdapter(properties) {
            dialog.dismiss()
            navigateToPropertyDetail(it.id)
        }
        dialog.setContentView(dialogBinding.root)
        dialog.show()
    }

    private fun loadPropertyMarkers() {

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    val properties = withContext(Dispatchers.IO) {
                        propertyRepository.getPropertyList().getOrThrow()
                    }

                    propertyList.clear()
                    propertyList.addAll(properties)
                    clusterManager.clearItems()

                    val geocoder = Geocoder(requireContext(), Locale.getDefault())

                    val latLngList = propertyList.map { property ->
                        async(Dispatchers.IO) {
                            try {
                                property.postalCode?.let { postal ->
                                    geocodeCache[postal] ?: geocodeAddressSafe(geocoder, postal)?.also {
                                        geocodeCache[postal] = it
                                    }
                                }
                            } catch (_: Exception) { null }
                        }
                    }.awaitAll()

                    propertyList.zip(latLngList).forEach { (property, latLng) ->
                        latLng?.let {
                            property.latLng = it
                            clusterManager.addItem(PropertyClusterItem(property, it))
                        }
                    }

                    clusterManager.cluster()

                    highlightPropertyId?.let { id ->
                        propertyList.find { it.id == id }?.latLng?.let { highlight(it) }
                            ?: moveToDefaultLocation()
                    } ?: moveToDefaultLocation()

                } catch (e: Exception) {
                    showError("Failed to load properties: ${e.message}")
                }
            }
        }
    }

    private suspend fun geocodeAddressSafe(geocoder: Geocoder, locationName: String): LatLng? =
        suspendCancellableCoroutine { cont ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocationName(locationName, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(results: MutableList<Address>) {
                            cont.resume(results.firstOrNull()?.let { LatLng(it.latitude, it.longitude) }, null)
                        }
                        override fun onError(errorMessage: String?) { cont.resume(null, null) }
                    })
                } else {
                    val address = geocoder.getFromLocationName(locationName, 1)?.firstOrNull()
                    cont.resume(address?.let { LatLng(it.latitude, it.longitude) }, null)
                }
            } catch (_: Exception) {
                cont.resume(null, null)
            }
        }

    private fun highlight(latLng: LatLng) {
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
        googleMap.addCircle(CircleOptions().center(latLng).radius(50.0).strokeColor(Color.RED).strokeWidth(5f))
    }

    private fun moveToDefaultLocation() {
        val singapore = LatLng(1.3521, 103.8198)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(singapore, 11f))
    }

    private fun moveToMyLocation() {
        if (!::googleMap.isInitialized) return
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return

        val locationProvider = LocationServices.getFusedLocationProviderClient(requireActivity())
        locationProvider.lastLocation.addOnSuccessListener { location ->
            val latLng = location?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(1.3521, 103.8198)
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, if (location != null) 15f else 11f))
        }.addOnFailureListener { showError("Failed to retrieve location") }

        googleMap.isMyLocationEnabled = true
    }

    private fun showError(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    override fun onResume() { super.onResume(); binding.mapView.onResume() }
    override fun onPause() { super.onPause(); binding.mapView.onPause() }
    override fun onDestroyView() { super.onDestroyView(); binding.mapView.onDestroy(); _binding = null }
    override fun onLowMemory() { super.onLowMemory(); binding.mapView.onLowMemory() }

}
