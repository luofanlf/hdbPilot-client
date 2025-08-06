package com.iss.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.maps.android.clustering.ClusterManager
import com.iss.R
import com.iss.adapter.PropertyAdapter
import com.iss.model.Property
import com.iss.repository.PropertyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale


class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var clusterManager: ClusterManager<PropertyClusterItem>
    private val propertyList = mutableListOf<Property>()
    private val propertyRepository = PropertyRepository()

    private var highlightPropertyId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        highlightPropertyId = arguments?.getLong("highlight_property_id")?.takeIf { it != 0L }
        Log.d("MapFragment", "onCreate: highlightPropertyId=$highlightPropertyId")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        mapView = view.findViewById(R.id.mapView)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        view.findViewById<ImageButton>(R.id.zoomIn).setOnClickListener {
            if (::googleMap.isInitialized) {
                Log.d("MapFragment", "Zoom In clicked")
                googleMap.animateCamera(CameraUpdateFactory.zoomIn())
            } else {
                Log.w("MapFragment", "Zoom In clicked but googleMap not initialized")
            }
        }

        view.findViewById<ImageButton>(R.id.zoomOut).setOnClickListener {
            if (::googleMap.isInitialized) {
                Log.d("MapFragment", "Zoom Out clicked")
                googleMap.animateCamera(CameraUpdateFactory.zoomOut())
            } else {
                Log.w("MapFragment", "Zoom Out clicked but googleMap not initialized")
            }
        }

        view.findViewById<ImageButton>(R.id.myLocationButton).setOnClickListener {
            Log.d("MapFragment", "My Location button clicked")
            enableMyLocation()
            moveToMyLocation()
        }

        return view
    }

    override fun onMapReady(map: GoogleMap) {
        Log.d("MapFragment", "onMapReady called")
        googleMap = map
        googleMap.uiSettings.isMyLocationButtonEnabled = false
        enableMyLocation()
        setupCluster()
        loadPropertyMarkers()
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("MapFragment", "Requesting location permission")
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }
        googleMap.isMyLocationEnabled = true
        Log.d("MapFragment", "Location enabled on map")
    }

    private fun setupCluster() {
        clusterManager = ClusterManager(requireContext(), googleMap)
        clusterManager.renderer = MapMarkerClusterRenderer(requireContext(), googleMap, clusterManager)

        googleMap.setOnCameraIdleListener(clusterManager)
        googleMap.setOnMarkerClickListener(clusterManager)

        // Modify the cluster click listener here to call the popup function
        clusterManager.setOnClusterClickListener { cluster ->
            // Get all properties inside the cluster
            val propertiesInCluster = cluster.items.map { it.property }.toList()
            showClusterPropertyListDialog(propertiesInCluster)
            true // Indicate that the event has been consumed
        }

        clusterManager.setOnClusterItemClickListener { item ->
            // When a single marker is clicked, navigate to the detail page
            val bundle = Bundle().apply {
                putLong("property_id", item.property.id)
            }
            findNavController().navigate(R.id.action_mapFragment_to_propertyDetailFragment, bundle)
            true
        }
    }

    // Put your popup dialog function here
    private fun showClusterPropertyListDialog(properties: List<Property>) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_property_list, null)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvDialogListings)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = PropertyAdapter(properties) { selectedProperty ->
            val bundle = Bundle().apply {
                putLong("property_id", selectedProperty.id)
            }
            dialog.dismiss()
            findNavController().navigate(R.id.action_mapFragment_to_propertyDetailFragment, bundle)
        }

        dialog.setContentView(view)
        dialog.show()
    }


    private fun loadPropertyMarkers() {
        Log.d("MapFragment", "Start loading property markers")
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val properties = withContext(Dispatchers.IO) {
                    propertyRepository.getPropertyList().getOrThrow()
                }
                propertyList.clear()
                propertyList.addAll(properties)
                clusterManager.clearItems()
                Log.d("MapFragment", "Loaded ${propertyList.size} properties")

                val geocoder = Geocoder(requireContext(), Locale.getDefault())

                val latLngList = coroutineScope {
                    propertyList.map { property ->
                        async(Dispatchers.IO) {
                            geocodeAddress(geocoder, property.postalCode)
                        }
                    }.awaitAll()
                }

                for (i in propertyList.indices) {
                    val latLng = latLngList[i]
                    val property = propertyList[i]
                    if (latLng != null) {
                        property.latLng = latLng
                        val item = PropertyClusterItem(property, latLng)
                        clusterManager.addItem(item)
                        Log.d("MapFragment", "Added marker for property id=${property.id} at $latLng")
                    } else {
                        Log.w("MapFragment", "No location found for postal code: ${property.postalCode}")
                    }
                }

                clusterManager.cluster()
                Log.d("MapFragment", "Cluster manager clustered items")

                highlightPropertyId?.let { id ->
                    val targetProperty = propertyList.find { it.id == id }
                    val targetLatLng = targetProperty?.latLng
                    if (targetLatLng != null) {
                        Log.d("MapFragment", "Highlighting property id=$id at $targetLatLng")
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(targetLatLng, 17f))
                        googleMap.addCircle(
                            CircleOptions()
                                .center(targetLatLng)
                                .radius(50.0)
                                .strokeColor(Color.RED)
                                .strokeWidth(5f)
                        )
                    } else {
                        Log.w("MapFragment", "Highlight property id=$id not found or has no location")
                    }
                } ?: run {
                    val defaultLocation = LatLng(1.3521, 103.8198)
                    Log.d("MapFragment", "No highlight property, move to default location Singapore")
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 11f))
                }

            } catch (e: Exception) {
                showError("Failed to load properties: ${e.message}")
                Log.e("MapFragment", "Failed to load properties", e)
            }
        }
    }

    private suspend fun geocodeAddress(geocoder: Geocoder, locationName: String): LatLng? {
        return withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocationName(locationName, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(results: MutableList<Address>) {
                            if (results.isNotEmpty()) {
                                val address = results[0]
                                continuation.resume(LatLng(address.latitude, address.longitude), null)
                            } else {
                                continuation.resume(null, null)
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            Log.e("MapFragment", "Geocode error: $errorMessage")
                            continuation.resume(null, null)
                        }
                    })
                }
            } else {
                try {
                    val addresses = geocoder.getFromLocationName(locationName, 1)
                    if (!addresses.isNullOrEmpty()) {
                        LatLng(addresses[0].latitude, addresses[0].longitude)
                    } else null
                } catch (e: Exception) {
                    Log.e("MapFragment", "Geocoding failed for $locationName: ${e.message}")
                    null
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun moveToMyLocation() {
        if (::googleMap.isInitialized) {
            val locationProvider = LocationServices.getFusedLocationProviderClient(requireActivity())
            locationProvider.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    Log.d("MapFragment", "Moving to current location: $latLng")
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                } else {
                    Log.w("MapFragment", "Location is null, fallback to Singapore")
                    val fallbackLatLng = LatLng(1.3521, 103.8198)
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(fallbackLatLng, 11f))
                    Toast.makeText(requireContext(), "Unable to get current location", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Log.e("MapFragment", "Failed to retrieve location", it)
                Toast.makeText(requireContext(), "Failed to retrieve location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d("MapFragment", "onRequestPermissionsResult: requestCode=$requestCode, grantResults=${grantResults.joinToString()}")
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation()
            moveToMyLocation()
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        Log.e("MapFragment", message)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        Log.d("MapFragment", "onResume called")
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        Log.d("MapFragment", "onPause called")
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        Log.d("MapFragment", "onDestroy called")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
        Log.d("MapFragment", "onLowMemory called")
    }
}
