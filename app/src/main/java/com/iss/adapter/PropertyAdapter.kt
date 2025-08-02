package com.iss.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.iss.R
import com.iss.model.Property
import com.iss.model.PropertyImage
import com.iss.api.PropertyApi
import com.iss.network.NetworkService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PropertyAdapter(
    private var properties: List<Property>,
    private val onItemClick: (Property) -> Unit
) : RecyclerView.Adapter<PropertyAdapter.PropertyViewHolder>() {

    private var allProperties = properties
    private val propertyApi = NetworkService.propertyApi
    private val imageCache = mutableMapOf<Long, String>() // 简单的图片URL缓存

    class PropertyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val propertyImage: ImageView = itemView.findViewById(R.id.propertyImage)
        val propertyTitle: TextView = itemView.findViewById(R.id.propertyTitle)
        val propertyAddress: TextView = itemView.findViewById(R.id.propertyAddress)
        val propertyPrice: TextView = itemView.findViewById(R.id.propertyPrice)
        val bedroomsText: TextView = itemView.findViewById(R.id.bedroomsText)
        val bathroomsText: TextView = itemView.findViewById(R.id.bathroomsText)
        val areaText: TextView = itemView.findViewById(R.id.areaText)
        val floorInfoText: TextView = itemView.findViewById(R.id.floorInfoText)
        val flatModelText: TextView = itemView.findViewById(R.id.flatModelText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropertyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_property, parent, false)
        return PropertyViewHolder(view)
    }

    override fun onBindViewHolder(holder: PropertyViewHolder, position: Int) {
        val property = properties[position]
        android.util.Log.d("PropertyAdapter", "Binding property at position $position: ${property.listingTitle}")
        
        holder.propertyTitle.text = property.listingTitle
        holder.propertyAddress.text = property.fullAddress
        holder.propertyPrice.text = property.formattedResalePrice
        holder.bedroomsText.text = "${property.bedroomNumber}BR"
        holder.bathroomsText.text = "${property.bathroomNumber}BA"
        holder.areaText.text = property.formattedArea
        holder.floorInfoText.text = property.floorInfo
        holder.flatModelText.text = property.flatModel
        
        // 加载房源缩略图
        loadPropertyThumbnail(holder.propertyImage, property.id)
        
        // 设置点击事件
        holder.itemView.setOnClickListener {
            onItemClick(property)
        }
    }

    private fun loadPropertyThumbnail(imageView: ImageView, propertyId: Long) {
        // 检查缓存
        val cachedImageUrl = imageCache[propertyId]
        if (cachedImageUrl != null) {
            Glide.with(imageView.context)
                .load(cachedImageUrl)
                .placeholder(R.drawable.ic_property_placeholder)
                .error(R.drawable.ic_property_placeholder)
                .centerCrop()
                .into(imageView)
            return
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = propertyApi.getPropertyImages(propertyId)
                
                if (response.isSuccessful) {
                    val images = response.body()?.data ?: emptyList()
                    if (images.isNotEmpty()) {
                        // 使用第一张图片作为缩略图
                        val firstImage = images.first()
                        val imageUrl = firstImage.imageUrl
                        
                        // 缓存图片URL
                        imageCache[propertyId] = imageUrl
                        
                        Glide.with(imageView.context)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_property_placeholder)
                            .error(R.drawable.ic_property_placeholder)
                            .centerCrop()
                            .into(imageView)
                    } else {
                        // 没有图片时显示占位符
                        imageView.setImageResource(R.drawable.ic_property_placeholder)
                    }
                } else {
                    // 加载失败时显示占位符
                    imageView.setImageResource(R.drawable.ic_property_placeholder)
                }
            } catch (e: Exception) {
                // 异常时显示占位符
                imageView.setImageResource(R.drawable.ic_property_placeholder)
            }
        }
    }

    override fun getItemCount(): Int = properties.size

    fun updateProperties(newProperties: List<Property>) {
        android.util.Log.d("PropertyAdapter", "updateProperties called with ${newProperties.size} properties")
        allProperties = newProperties
        properties = newProperties
        notifyDataSetChanged()
        android.util.Log.d("PropertyAdapter", "notifyDataSetChanged called, itemCount: $itemCount")
    }

    fun addProperties(newProperties: List<Property>) {
        android.util.Log.d("PropertyAdapter", "addProperties called with ${newProperties.size} properties")
        val oldSize = properties.size
        allProperties = allProperties + newProperties
        properties = allProperties
        notifyItemRangeInserted(oldSize, newProperties.size)
        android.util.Log.d("PropertyAdapter", "notifyItemRangeInserted called, oldSize: $oldSize, newSize: ${newProperties.size}, total: $itemCount")
    }

    fun filterProperties(query: String) {
        val filteredList = if (query.isEmpty()) {
            allProperties
        } else {
            allProperties.filter { property ->
                property.listingTitle.contains(query, ignoreCase = true) ||
                property.town.contains(query, ignoreCase = true) ||
                property.streetName.contains(query, ignoreCase = true) ||
                property.postalCode.contains(query, ignoreCase = true) ||
                property.flatModel.contains(query, ignoreCase = true)
            }
        }
        properties = filteredList
        notifyDataSetChanged()
    }
    
    fun clearCache() {
        imageCache.clear()
    }
} 