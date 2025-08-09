package com.iss.ui.comment

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iss.R
import com.iss.model.Comment
import com.iss.repository.CommentRepository
import com.iss.ui.adapters.CommentAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.iss.utils.UserManager

class CommentListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyText: TextView
    private lateinit var commentAdapter: CommentAdapter
    private val commentRepository = CommentRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_comment_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        loadUserComments()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.commentRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        emptyText = view.findViewById(R.id.emptyText)
    }

    private fun setupRecyclerView() {
        commentAdapter = CommentAdapter(
            onCommentClick = { comment ->
                // 导航到房源详情页面
                navigateToPropertyDetail(comment.propertyId)
            },
            onDeleteComment = { comment ->
                // 删除评论
                deleteComment(comment)
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = commentAdapter
        }
    }

    private fun loadUserComments() {
        // 检查用户是否已登录
        val userId = UserManager.getCurrentUserId()
        val isLoggedIn = UserManager.isLoggedIn()
        
        Log.d("CommentListFragment", "User ID: $userId, Is Logged In: $isLoggedIn")
        
        if (!isLoggedIn) {
            showError("Please login to view your comments")
            return
        }
        
        showLoading(true)
        Log.d("CommentListFragment", "Starting to load user comments for user: $userId")
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = commentRepository.getUserComments()
                Log.d("CommentListFragment", "Got result from repository: $result")
                result.fold(
                    onSuccess = { comments ->
                        Log.d("CommentListFragment", "Successfully loaded ${comments.size} comments")
                        if (comments.isNotEmpty()) {
                            // 获取所有评论对应的房源ID
                            val propertyIds = comments.map { it.propertyId }.distinct()
                            Log.d("CommentListFragment", "Need to fetch ${propertyIds.size} properties: $propertyIds")
                            
                            // 获取房源详情
                            val propertiesResult = commentRepository.getPropertiesByIds(propertyIds)
                            propertiesResult.fold(
                                onSuccess = { properties ->
                                    Log.d("CommentListFragment", "Successfully loaded ${properties.size} properties")
                                    showLoading(false)
                                    commentAdapter.updateComments(comments, properties)
                                    showContent()
                                },
                                onFailure = { propertyException ->
                                    Log.w("CommentListFragment", "Failed to load properties: ${propertyException.message}")
                                    showLoading(false)
                                    // 即使获取房源信息失败，也显示评论（没有房源详情）
                                    commentAdapter.updateComments(comments, null)
                                    showContent()
                                }
                            )
                        } else {
                            Log.d("CommentListFragment", "No comments found for user: $userId")
                            showLoading(false)
                            showEmpty()
                        }
                    },
                    onFailure = { exception ->
                        Log.e("CommentListFragment", "Failed to load comments for user: $userId", exception)
                        showLoading(false)
                        showError("Failed to load comments: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e("CommentListFragment", "Error in loadUserComments for user: $userId", e)
                showLoading(false)
                showError("Error: ${e.message}")
            }
        }
    }

    private fun navigateToPropertyDetail(propertyId: Long) {
        // 使用Navigation组件导航到房源详情页面
        val bundle = Bundle().apply {
            putLong("property_id", propertyId)
        }
        findNavController().navigate(R.id.action_commentListFragment_to_propertyDetailFragment, bundle)
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showContent() {
        recyclerView.visibility = View.VISIBLE
        emptyText.visibility = View.GONE
    }

    private fun showEmpty() {
        recyclerView.visibility = View.GONE
        emptyText.visibility = View.VISIBLE
        emptyText.text = "No comments yet"
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        showEmpty()
    }
    
    private fun deleteComment(comment: Comment) {
        // Show confirmation dialog
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Comment")
            .setMessage("Are you sure you want to delete this comment?")
            .setPositiveButton("Delete") { _, _ ->
                performDeleteComment(comment)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun performDeleteComment(comment: Comment) {
        showLoading(true)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = commentRepository.deleteComment(comment.id)
                result.fold(
                    onSuccess = {
                        Toast.makeText(requireContext(), "Comment deleted successfully", Toast.LENGTH_SHORT).show()
                        // Reload comment list
                        loadUserComments()
                    },
                    onFailure = { exception ->
                        Log.e("CommentListFragment", "Failed to delete comment", exception)
                        showLoading(false)
                        Toast.makeText(requireContext(), "Failed to delete comment: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                Log.e("CommentListFragment", "Error in deleteComment", e)
                showLoading(false)
                Toast.makeText(requireContext(), "Error occurred while deleting comment: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
