package com.iss.ui.favorite

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
import com.iss.model.Favorite
import com.iss.repository.FavoriteRepository
import com.iss.ui.adapters.FavoriteAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FavoriteListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyText: TextView
    private lateinit var favoriteAdapter: FavoriteAdapter
    private val favoriteRepository = FavoriteRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favorite_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        loadFavorites()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.favoriteRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        emptyText = view.findViewById(R.id.emptyText)
    }

    private fun setupRecyclerView() {
        favoriteAdapter = FavoriteAdapter(
            onFavoriteClick = { favorite ->
                // 导航到房源详情页面
                navigateToPropertyDetail(favorite.propertyId)
            },
            onRemoveFavorite = { favorite ->
                removeFavorite(favorite)
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = favoriteAdapter
        }
    }

    private fun loadFavorites() {
        showLoading(true)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = favoriteRepository.getUserFavorites()
                result.fold(
                    onSuccess = { pageResponse ->
                        showLoading(false)
                        if (pageResponse.records.isNotEmpty()) {
                            // 添加调试日志
                            Log.d("FavoriteListFragment", "Received ${pageResponse.records.size} favorites")
                            pageResponse.records.forEach { favorite ->
                                Log.d("FavoriteListFragment", "Favorite ID: ${favorite.id}, Property: ${favorite.property}")
                                Log.d("FavoriteListFragment", "Property imageList: ${favorite.property?.imageList}")
                            }
                            
                            favoriteAdapter.updateFavorites(pageResponse.records)
                            showContent()
                        } else {
                            showEmpty()
                        }
                    },
                    onFailure = { exception ->
                        showLoading(false)
                        showError("Failed to load favorites: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                showLoading(false)
                showError("Error: ${e.message}")
            }
        }
    }

    private fun removeFavorite(favorite: Favorite) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = favoriteRepository.removeFavorite(favorite.id)
                result.fold(
                    onSuccess = { success ->
                        if (success) {
                            Toast.makeText(requireContext(), "Removed from favorites", Toast.LENGTH_SHORT).show()
                            // 重新加载收藏列表
                            loadFavorites()
                        } else {
                            Toast.makeText(requireContext(), "Failed to remove from favorites", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFailure = { exception ->
                        Toast.makeText(requireContext(), "Failed to remove favorite: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToPropertyDetail(propertyId: Long) {
        // 使用Navigation组件导航到房源详情页面
        val bundle = Bundle().apply {
            putLong("property_id", propertyId)
        }
        findNavController().navigate(R.id.action_favoriteListFragment_to_propertyDetailFragment, bundle)
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
        emptyText.text = "No favorites yet"
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        showEmpty()
    }
} 