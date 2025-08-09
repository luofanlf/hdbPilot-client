package com.iss.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.iss.R
import com.iss.api.PropertyApi
import com.iss.databinding.ItemMyListingBinding
import com.iss.model.Property
import com.iss.model.PropertyImage
import com.iss.network.NetworkService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import android.widget.TextView
import androidx.core.content.ContextCompat
import android.util.Log

class MyListingsAdapter(
    private val onItemClick: (Property) -> Unit
) : ListAdapter<Property, MyListingsAdapter.ViewHolder>(PropertyDiffCallback()) {

    private val propertyApi = NetworkService.propertyApi
    private val imageCache = mutableMapOf<Long, String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMyListingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemMyListingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(property: Property) {
            binding.apply {
                propertyTitle.text = property.listingTitle
                propertyAddress.text = "${property.block} ${property.streetName}, ${property.town} ${property.postalCode}"
                propertyPrice.text = formatPrice(property.resalePrice)
                bedroomsText.text = "${property.bedroomNumber}BR"
                bathroomsText.text = "${property.bathroomNumber}BA"
                areaText.text = "${property.floorAreaSqm.toInt()}㎡"
                floorInfoText.text = "Level ${property.storey}"
                flatModelText.text = property.flatModel

                // 显示房源状态
                propertyStatus.text = formatStatus(property.status)
                setStatusStyle(propertyStatus, property.status)
                
                // 调试日志
                Log.d("MyListingsAdapter", "Property ID: ${property.id}, Status: ${property.status}, Formatted: ${formatStatus(property.status)}")

                // 使用Property对象中的imageList
                loadPropertyThumbnail(propertyImage, property)

                // 设置整个item的点击事件
                root.setOnClickListener {
                    onItemClick(property)
                }
            }
        }

        private fun formatPrice(price: Float): String {
            val formatter = NumberFormat.getCurrencyInstance(Locale("en", "SG"))
            return formatter.format(price)
        }

        private fun formatStatus(status: String?): String {
            return when (status?.lowercase()) {
                "pending" -> "Pending"
                "available" -> "Available"
                "sold" -> "Sold"
                "rejected" -> "Rejected"
                else -> status ?: "Unknown"
            }
        }

        private fun setStatusStyle(textView: TextView, status: String?) {
            val (backgroundRes, textColor) = when (status?.lowercase()) {
                "pending" -> Pair(R.drawable.status_pending_background, R.color.text_primary)
                "available" -> Pair(R.drawable.status_available_background, R.color.white)
                "sold" -> Pair(R.drawable.status_sold_background, R.color.text_primary)
                "rejected" -> Pair(R.drawable.status_rejected_background, R.color.white)
                else -> Pair(R.drawable.status_background, R.color.text_primary)
            }
            
            textView.setBackgroundResource(backgroundRes)
            textView.setTextColor(ContextCompat.getColor(textView.context, textColor))
        }

        private fun String.capitalize(): String {
            return if (this.isNotEmpty()) {
                this[0].uppercase() + this.substring(1)
            } else {
                this
            }
        }
    }

    private fun loadPropertyThumbnail(imageView: android.widget.ImageView, property: Property) {
        // 使用Property对象中的imageList
        val firstImageUrl = property.firstImageUrl
        
        if (!firstImageUrl.isNullOrEmpty()) {
            Glide.with(imageView.context)
                .load(firstImageUrl)
                .placeholder(R.drawable.ic_property_placeholder)
                .error(R.drawable.ic_property_placeholder)
                .centerCrop()
                .into(imageView)
        } else {
            // 没有图片时显示占位符
            imageView.setImageResource(R.drawable.ic_property_placeholder)
        }
    }

    fun clearCache() {
        imageCache.clear()
    }

    private class PropertyDiffCallback : DiffUtil.ItemCallback<Property>() {
        override fun areItemsTheSame(oldItem: Property, newItem: Property): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Property, newItem: Property): Boolean {
            return oldItem == newItem
        }
    }
} 