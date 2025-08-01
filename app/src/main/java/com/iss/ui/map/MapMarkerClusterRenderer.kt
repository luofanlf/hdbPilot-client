package com.iss.ui.map

import android.content.Context
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer

class MapMarkerClusterRenderer(
    context: Context,
    map: GoogleMap,
    clusterManager: ClusterManager<PropertyClusterItem>
) : DefaultClusterRenderer<PropertyClusterItem>(context, map, clusterManager) {

    override fun onBeforeClusterItemRendered(item: PropertyClusterItem, markerOptions: MarkerOptions) {
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            .title(item.title)
            .snippet(item.snippet)
    }
}

