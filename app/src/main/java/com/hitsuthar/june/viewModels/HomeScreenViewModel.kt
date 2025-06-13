package com.hitsuthar.june.viewModels

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.input.key.type
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.moviebase.tmdb.model.TmdbMediaListItem
import com.hitsuthar.june.MediaListState
import com.hitsuthar.june.screens.Category
import com.hitsuthar.june.screens.PagingData
import com.hitsuthar.june.screens.Subcategory
import com.hitsuthar.june.utils.TmdbRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeScreenViewModel(private val repository: TmdbRepository) : ViewModel() {
  private val _categories = MutableStateFlow<List<Category>>(emptyList())
  val categories: StateFlow<List<Category>> = _categories.asStateFlow()

  // Holds the currently selected subcategory type for each main category (by category name)
  private val _selectedSubcategoryTypes = mutableStateMapOf<String, String>()
  val selectedSubcategoryTypes: Map<String, String> get() = _selectedSubcategoryTypes

  // Holds the items and paging data for each *selected* subcategory
  // Key: "CategoryName-SubcategoryType"
  private val _subcategoryPagingData = mutableStateMapOf<String, PagingData>()
  val subcategoryPagingData: Map<String, PagingData> get() = _subcategoryPagingData

  // Tracks loading state for each subcategory's pagination
  // Key: "CategoryName-SubcategoryType"
  private val _isLoadingMore = mutableStateMapOf<String, Boolean>()
  val isLoadingMore: Map<String, Boolean> get() = _isLoadingMore


  init {
    loadInitialCategories()
    // Automatically load data for the default first subcategory of each category
    viewModelScope.launch {
      _categories.value.forEach { category ->
        category.subcategories.firstOrNull()?.let { firstSubcategory ->
          // Initialize selected type
          _selectedSubcategoryTypes[category.name] = firstSubcategory.type
          // Load initial data
          loadOrRefreshItemsForSubcategory(category, firstSubcategory, isRefresh = true)
        }
      }
    }
  }

  private fun loadInitialCategories() {
    // Define your categories here or fetch them if they are dynamic
    _categories.value = listOf(
      Category(
        "Popular", listOf(
          Subcategory("Movies", { page -> repository.getPopularMovies(page) }),
          Subcategory("Shows", { page -> repository.getPopularShows(page) })
        )
      ), Category(
        "Top Rated", listOf(
          Subcategory("Movies", { page -> repository.getTopRatedMovies(page) }),
          Subcategory("Shows", { page -> repository.getTopRatedShows(page) })
        )
      ), Category(
        "Streaming Now", listOf(
          Subcategory("Netflix", { page -> repository.getOnStreamingNetflix(page) }),
          Subcategory("Prime Video", { page -> repository.getOnStreamingPrimeVideo(page) }),
          Subcategory("Apple TV", { page -> repository.getOnStreamingAppleTV(page) }),
          Subcategory("Disney+", { page -> repository.getOnStreamingDisneyPlus(page) }),
        )
      )
    )
  }


  fun onSubcategorySelected(category: Category, newSelectedSubcategory: Subcategory) {
    val oldSelectedType = _selectedSubcategoryTypes[category.name]
    if (oldSelectedType == newSelectedSubcategory.type) return // No change

    _selectedSubcategoryTypes[category.name] = newSelectedSubcategory.type
    loadOrRefreshItemsForSubcategory(category, newSelectedSubcategory, isRefresh = true)
  }

  fun loadMoreItems(category: Category, subcategory: Subcategory) {
    if (_selectedSubcategoryTypes[category.name] != subcategory.type) {
      // This shouldn't happen if UI is driven by selectedSubcategoryTypes, but as a safeguard:
      // If trying to load more for a non-selected subcategory, switch to it and refresh.
      onSubcategorySelected(category, subcategory)
      return
    }
    loadOrRefreshItemsForSubcategory(category, subcategory, isRefresh = false)
  }

  private fun loadOrRefreshItemsForSubcategory(
    category: Category,
    subcategory: Subcategory,
    isRefresh: Boolean
  ) {
    val key = subcategoryKey(category, subcategory)
    if (_isLoadingMore[key] == true) return // Already loading

    viewModelScope.launch {
      _isLoadingMore[key] = true
      try {
        val currentPagingData = if (isRefresh) PagingData() else (_subcategoryPagingData[key] ?: PagingData())
        val newItems = subcategory.itemFetcher.invoke(currentPagingData.currentPage + 1)

        if (newItems.isNotEmpty()) {
          _subcategoryPagingData[key] = currentPagingData.copy(
            items = if (isRefresh) newItems else currentPagingData.items + newItems,
            currentPage = currentPagingData.currentPage + 1
          )
        } else {
          // No new items, might be end of list. Ensure current data is set if refreshing with no results.
          if (isRefresh) {
            _subcategoryPagingData[key] = currentPagingData
          }
          // Optionally, you can add a flag here to indicate "end of list" for this subcategory
        }
      } catch (e: Exception) {
        // Handle error (e.g., update a UI error state)
        // Log.e("HomeViewModel", "Error loading items for $key", e)
      } finally {
        _isLoadingMore[key] = false
      }
    }
  }

  fun subcategoryKey(category: Category, subcategory: Subcategory): String {
    return "${category.name}-${subcategory.type}"
  }

  fun subcategoryKey(categoryName: String, subcategoryType: String): String {
    return "$categoryName-$subcategoryType"
  }


}