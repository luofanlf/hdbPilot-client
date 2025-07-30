package com.iss.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.iss.databinding.ItemMyListingBinding
import com.iss.model.Property
import java.text.NumberFormat
import java.util.Locale

class MyListingsAdapter(
    private val onItemClick: (Property) -> Unit
) : ListAdapter<Property, MyListingsAdapter.ViewHolder>(PropertyDiffCallback()) {

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

    private class PropertyDiffCallback : DiffUtil.ItemCallback<Property>() {
        override fun areItemsTheSame(oldItem: Property, newItem: Property): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Property, newItem: Property): Boolean {
            return oldItem == newItem
        }
    }
} 