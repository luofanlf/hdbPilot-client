package com.iss.ui.map

import android.Manifest
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterManager
import com.iss.R
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

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        view.findViewById<ImageButton>(R.id.zoomIn).setOnClickListener {
            googleMap.animateCamera(CameraUpdateFactory.zoomIn())
        }

        view.findViewById<ImageButton>(R.id.zoomOut).setOnClickListener {
            googleMap.animateCamera(CameraUpdateFactory.zoomOut())
        }

        view.findViewById<ImageButton>(R.id.myLocationButton).setOnClickListener {
            enableMyLocation()
        }

        return view
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        enableMyLocation()
        setupCluster()
        loadPropertyMarkers()
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }
        googleMap.isMyLocationEnabled = true
    }

    private fun setupCluster() {
        clusterManager = ClusterManager(requireContext(), googleMap)
        clusterManager.renderer = MapMarkerClusterRenderer(requireContext(), googleMap, clusterManager)
        googleMap.setOnCameraIdleListener(clusterManager)
        googleMap.setOnMarkerClickListener(clusterManager)

        clusterManager.setOnClusterItemClickListener { item ->
            val bundle = Bundle().apply {
                putLong("property_id", item.property.id)
            }
            findNavController().navigate(R.id.action_mapFragment_to_propertyDetailFragment, bundle)
            true
        }
    }

    private fun loadPropertyMarkers() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val properties = withContext(Dispatchers.IO) {
                    propertyRepository.getPropertyList().getOrThrow()
                }
                propertyList.clear()
                propertyList.addAll(properties)
                clusterManager.clearItems()

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
                    } else {
                        Log.w("MapFragment", "No location found for postal code: ${property.postalCode}")
                    }
                }

                clusterManager.cluster()

                highlightPropertyId?.let { id ->
                    val targetProperty = propertyList.find { it.id == id }
                    val targetLatLng = targetProperty?.latLng
                    if (targetLatLng != null) {
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(targetLatLng, 17f))
                        googleMap.addCircle(
                            CircleOptions()
                                .center(targetLatLng)
                                .radius(50.0)
                                .strokeColor(Color.RED)
                                .strokeWidth(5f)
                        )
                    }
                } ?: run {
                    val defaultLocation = LatLng(1.3521, 103.8198)
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

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }
}
