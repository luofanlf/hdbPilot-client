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

                // 加载缩略图
                loadPropertyThumbnail(propertyImage, property.id)

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

        private fun String.capitalize(): String {
            return if (this.isNotEmpty()) {
                this[0].uppercase() + this.substring(1)
            } else {
                this
            }
        }
    }

    private fun loadPropertyThumbnail(imageView: android.widget.ImageView, propertyId: Long) {
        // 暂时使用占位符图片，避免API调用错误
        Glide.with(imageView.context)
            .load(R.drawable.ic_property_placeholder)
            .centerCrop()
            .into(imageView)
        
        // TODO: 当PropertyImage API修复后，可以恢复以下代码
        /*
        // 首先检查缓存
        imageCache[propertyId]?.let { cachedUrl ->
            Glide.with(imageView.context)
                .load(cachedUrl)
                .placeholder(R.drawable.ic_property_placeholder)
                .error(R.drawable.ic_property_placeholder)
                .centerCrop()
                .into(imageView)
            return
        }

        // 如果没有缓存，从API获取图片
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = propertyApi.getPropertyImages(propertyId)
                if (response.isSuccessful) {
                    val images = response.body()?.data
                    if (!images.isNullOrEmpty()) {
                        val firstImageUrl = images.first().imageUrl
                        // 缓存URL
                        imageCache[propertyId] = firstImageUrl
                        
                        // 在主线程中加载图片
                        CoroutineScope(Dispatchers.Main).launch {
                            Glide.with(imageView.context)
                                .load(firstImageUrl)
                                .placeholder(R.drawable.ic_property_placeholder)
                                .error(R.drawable.ic_property_placeholder)
                                .centerCrop()
                                .into(imageView)
                        }
                    } else {
                        // 没有图片，显示占位符
                        CoroutineScope(Dispatchers.Main).launch {
                            Glide.with(imageView.context)
                                .load(R.drawable.ic_property_placeholder)
                                .centerCrop()
                                .into(imageView)
                        }
                    }
                } else {
                    // API调用失败，显示占位符
                    CoroutineScope(Dispatchers.Main).launch {
                        Glide.with(imageView.context)
                            .load(R.drawable.ic_property_placeholder)
                            .centerCrop()
                            .into(imageView)
                    }
                }
            } catch (e: Exception) {
                // 异常情况，显示占位符
                CoroutineScope(Dispatchers.Main).launch {
                    Glide.with(imageView.context)
                        .load(R.drawable.ic_property_placeholder)
                        .centerCrop()
                        .into(imageView)
                }
            }
        }
        */
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