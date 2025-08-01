package com.iss.ui.map

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import com.iss.model.Property

class PropertyClusterItem(
    val property: Property,
    private val position: LatLng,
) : ClusterItem {

    override fun getPosition(): LatLng = position

    override fun getTitle(): String = property.listingTitle

    override fun getSnippet(): String = property.fullAddress

    override fun getZIndex(): Float = 0f
}


