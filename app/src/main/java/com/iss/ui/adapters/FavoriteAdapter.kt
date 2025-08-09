package com.iss.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.iss.R
import com.iss.model.Favorite
import android.util.Log

class FavoriteAdapter(
    private val onFavoriteClick: (Favorite) -> Unit,
    private val onRemoveFavorite: (Favorite) -> Unit
) : RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder>() {

    private var favorites: List<Favorite> = emptyList()

    fun updateFavorites(newFavorites: List<Favorite>) {
        favorites = newFavorites
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite, parent, false)
        return FavoriteViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(favorites[position])
    }

    override fun getItemCount(): Int = favorites.size

    inner class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val propertyImageView: ImageView = itemView.findViewById(R.id.propertyImageView)
        private val titleText: TextView = itemView.findViewById(R.id.titleText)
        private val addressText: TextView = itemView.findViewById(R.id.addressText)
        private val priceText: TextView = itemView.findViewById(R.id.priceText)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)

        fun bind(favorite: Favorite) {
            // 设置房源信息
            favorite.property?.let { property ->
                titleText.text = property.listingTitle
                addressText.text = property.fullAddress
                priceText.text = property.formattedResalePrice

                // 加载房源图片
                loadPropertyImage(favorite)
            }

            // 设置点击事件
            itemView.setOnClickListener {
                onFavoriteClick(favorite)
            }

            // 设置移除收藏按钮
            btnRemove.setOnClickListener {
                onRemoveFavorite(favorite)
            }
        }

        private fun loadPropertyImage(favorite: Favorite) {
            // 添加调试日志
            Log.d("FavoriteAdapter", "Loading image for favorite: ${favorite.id}")
            Log.d("FavoriteAdapter", "Property: ${favorite.property}")
            Log.d("FavoriteAdapter", "Property imageList: ${favorite.property?.imageList}")
            Log.d("FavoriteAdapter", "First image URL: ${favorite.property?.firstImageUrl}")
            
            // 使用 Property 对象中的 imageList 来加载房源缩略图
            val firstImageUrl = favorite.property?.firstImageUrl
            
            if (!firstImageUrl.isNullOrEmpty()) {
                Log.d("FavoriteAdapter", "Loading actual image: $firstImageUrl")
                Glide.with(propertyImageView.context)
                    .load(firstImageUrl)
                    .placeholder(R.drawable.ic_property_placeholder)
                    .error(R.drawable.ic_property_placeholder)
                    .centerCrop()
                    .into(propertyImageView)
            } else {
                Log.d("FavoriteAdapter", "No image URL found, showing placeholder")
                // 没有图片时显示占位符
                propertyImageView.setImageResource(R.drawable.ic_property_placeholder)
            }
        }
    }
} 