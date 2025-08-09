package com.iss.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iss.R
import com.iss.model.Comment
import com.iss.model.Property
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import android.util.Log

class CommentAdapter(
    private val onCommentClick: (Comment) -> Unit,
    private val onDeleteComment: (Comment) -> Unit
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private var comments: List<CommentWithProperty> = emptyList()
    
    // 日期格式化器
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val displayFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    data class CommentWithProperty(
        val comment: Comment,
        val property: Property?
    )

    fun updateComments(comments: List<Comment>, properties: List<Property>? = null) {
        this.comments = comments.map { comment ->
            val property = properties?.find { it.id == comment.propertyId }
            CommentWithProperty(comment, property)
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(comments[position])
    }

    override fun getItemCount(): Int = comments.size

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)
        private val tvComment: TextView = itemView.findViewById(R.id.tvComment)
        private val tvPropertyTitle: TextView = itemView.findViewById(R.id.tvPropertyTitle)
        private val tvPropertyLocation: TextView = itemView.findViewById(R.id.tvPropertyLocation)
        private val tvCommentDate: TextView = itemView.findViewById(R.id.tvCommentDate)
        private val btnDelete: Button = itemView.findViewById(R.id.btnDelete)

        fun bind(commentWithProperty: CommentWithProperty) {
            val comment = commentWithProperty.comment
            val property = commentWithProperty.property

            Log.d("CommentAdapter", "Binding comment: ${comment.content}, property: ${property?.listingTitle}")

            // 设置评分
            ratingBar.rating = comment.rating.toFloat()
            ratingBar.isEnabled = false // 只读

            // 设置评论内容
            tvComment.text = comment.content

            // 设置房源信息
            property?.let {
                tvPropertyTitle.text = it.listingTitle ?: "Unknown Property"
                tvPropertyLocation.text = "${it.town ?: ""} ${it.streetName ?: ""}".trim()
            } ?: run {
                tvPropertyTitle.text = "Property not found"
                tvPropertyLocation.text = "Location unavailable"
            }

            // 设置评论日期
            comment.createdAt?.let { dateString ->
                try {
                    val dateTime = LocalDateTime.parse(dateString, dateFormatter)
                    val formattedDate = dateTime.format(displayFormatter)
                    tvCommentDate.text = "Commented on: $formattedDate"
                } catch (e: DateTimeParseException) {
                    // 如果解析失败，显示原始字符串
                    tvCommentDate.text = "Commented on: $dateString"
                }
            } ?: run {
                tvCommentDate.text = "Date unknown"
            }

            // 设置点击事件
            itemView.setOnClickListener {
                onCommentClick(comment)
            }
            
            // 设置删除按钮点击事件
            btnDelete.setOnClickListener {
                onDeleteComment(comment)
            }
        }
    }
}
