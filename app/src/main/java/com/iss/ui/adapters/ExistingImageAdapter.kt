package com.iss.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.iss.R
import com.iss.model.PropertyImage

class ExistingImageAdapter(
    private var existingImages: List<PropertyImage> = emptyList(),
    private val onImageDeleted: (PropertyImage) -> Unit
) : RecyclerView.Adapter<ExistingImageAdapter.ImageViewHolder>() {

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.selectedImageView)
        val removeButton: ImageView = itemView.findViewById(R.id.btnRemoveImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val propertyImage = existingImages[position]
        
        // 加载图片
        Glide.with(holder.imageView.context)
            .load(propertyImage.imageUrl)
            .placeholder(R.drawable.ic_property_placeholder)
            .error(R.drawable.ic_property_placeholder)
            .centerCrop()
            .into(holder.imageView)
        
        // 设置删除按钮点击事件
        holder.removeButton.setOnClickListener {
            onImageDeleted(propertyImage)
        }
    }

    override fun getItemCount(): Int = existingImages.size

    fun updateImages(newImages: List<PropertyImage>) {
        existingImages = newImages
        notifyDataSetChanged()
    }

    fun removeImage(propertyImage: PropertyImage) {
        val newList = existingImages.toMutableList()
        val index = newList.indexOfFirst { it.id == propertyImage.id }
        if (index != -1) {
            newList.removeAt(index)
            existingImages = newList
            notifyItemRemoved(index)
            println("Removing image from adapter: ${propertyImage.id}, position: $index")
        } else {
            println("Image not found for deletion: ${propertyImage.id}")
        }
    }
} 