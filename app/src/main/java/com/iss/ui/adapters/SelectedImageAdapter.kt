package com.iss.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.iss.R

class SelectedImageAdapter(
    private var selectedImages: List<String> = emptyList(),
    private val onImageRemoved: (Int) -> Unit
) : RecyclerView.Adapter<SelectedImageAdapter.ImageViewHolder>() {

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
        val imagePath = selectedImages[position]
        
        // 加载图片
        Glide.with(holder.imageView.context)
            .load(imagePath)
            .placeholder(R.drawable.ic_property_placeholder)
            .error(R.drawable.ic_property_placeholder)
            .centerCrop()
            .into(holder.imageView)
        
        // 设置删除按钮点击事件
        holder.removeButton.setOnClickListener {
            onImageRemoved(position)
        }
    }

    override fun getItemCount(): Int = selectedImages.size

    fun updateImages(newImages: List<String>) {
        selectedImages = newImages
        notifyDataSetChanged()
    }

    fun addImage(imagePath: String) {
        val newList = selectedImages.toMutableList()
        newList.add(imagePath)
        selectedImages = newList
        notifyItemInserted(selectedImages.size - 1)
    }

    fun removeImage(position: Int) {
        if (position in 0 until selectedImages.size) {
            val newList = selectedImages.toMutableList()
            newList.removeAt(position)
            selectedImages = newList
            notifyItemRemoved(position)
        }
    }
} 