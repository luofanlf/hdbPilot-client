package com.iss.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.iss.R
import com.iss.model.PropertyImage

class PropertyImageAdapter(
    private var images: List<PropertyImage> = emptyList()
) : RecyclerView.Adapter<PropertyImageAdapter.ImageViewHolder>() {

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.propertyImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_property_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val image = images[position]
        
        // 使用Glide加载图片
        Glide.with(holder.imageView.context)
            .load(image.imageUrl)
            .placeholder(R.drawable.ic_property_placeholder)
            .error(R.drawable.ic_property_placeholder)
            .centerCrop()
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = images.size

    fun updateImages(newImages: List<PropertyImage>) {
        images = newImages
        notifyDataSetChanged()
    }
} 