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
import com.iss.model.PropertyImage

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
                loadPropertyImage(property.id)
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

        private fun loadPropertyImage(propertyId: Long) {
            // 这里可以加载房源的图片
            // 暂时使用默认图片
            propertyImageView.setImageResource(R.drawable.ic_house)
        }
    }
} 