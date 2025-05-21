package com.hitsuthar.june.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitsuthar.june.MediaListState
import com.hitsuthar.june.utils.TmdbRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeScreenViewModel(private val repository: TmdbRepository) : ViewModel() {
    private val _state = MutableStateFlow(MediaListState())
    val state = _state.asStateFlow()

    init {
        loadPopularMovies()
        loadPopularShows()
        loadTopRatedMovies()
        loadTopRatedShows()
        loadOnStreamingNetflix()
        loadOnStreamingPrimeVideo()
        loadOnStreamingAppleTV()
        loadOnStreamingDisneyPlus()
    }

    private fun loadPopularMovies(loadMore: Boolean = false) {
        viewModelScope.launch {
            val currentCategoryState = state.value.popularMovies
            val page = if (loadMore) currentCategoryState.page + 1 else currentCategoryState.page
            try {
                val newItem = repository.getPopularMovies(page)
                val updatedItems = if (loadMore) currentCategoryState.items + newItem else newItem
                _state.value = state.value.copy(
                    popularMovies = currentCategoryState.copy(
                        items = updatedItems, page = page
                    ),
                    isLoadingMore = false
                )
            } catch (e: Exception) {
                _state.value = state.value.copy(isLoadingMore = false)
            }
        }
    }

    private fun loadPopularShows(loadMore: Boolean = false) {
        viewModelScope.launch {
            val currentCategoryState = state.value.popularShows
            val page = if (loadMore) currentCategoryState.page + 1 else currentCategoryState.page
            try {
                val newItem = repository.getPopularShows(page)
                val updatedItems = if (loadMore) currentCategoryState.items + newItem else newItem
                _state.value = state.value.copy(
                    popularShows = currentCategoryState.copy(
                        items = updatedItems, page = page
                    )
                )
            } catch (e: Exception) {
                _state.value = state.value.copy(isLoadingMore = false)
            }
        }
    }

    private fun loadTopRatedMovies(loadMore: Boolean = false) {
        viewModelScope.launch {
            val currentCategoryState = state.value.topRatedMovies
            val page = if (loadMore) currentCategoryState.page + 1 else currentCategoryState.page
            try {
                val newItem = repository.getTopRatedMovies(page)
                val updatedItems = if (loadMore) currentCategoryState.items + newItem else newItem
                _state.value = state.value.copy(
                    topRatedMovies = currentCategoryState.copy(
                        items = updatedItems, page = page
                    )
                )
            } catch (e: Exception) {
                _state.value = state.value.copy(isLoadingMore = false)
            }
        }
    }

    private fun loadTopRatedShows(loadMore: Boolean = false) {
        viewModelScope.launch {
            val currentCategoryState = state.value.topRatedShows
            val page = if (loadMore) currentCategoryState.page + 1 else currentCategoryState.page
            try {
                val newItem = repository.getTopRatedShows(page)
                val updatedItems = if (loadMore) currentCategoryState.items + newItem else newItem
                _state.value = state.value.copy(
                    topRatedShows = currentCategoryState.copy(
                        items = updatedItems, page = page
                    )
                )
            } catch (e: Exception) {
                _state.value = state.value.copy(isLoadingMore = false)
            }
        }
    }

    private fun loadOnStreamingNetflix(loadMore: Boolean = false) {
        viewModelScope.launch {
            val currentCategoryState = state.value.onStreamingNetflix
            val page = if (loadMore) currentCategoryState.page + 1 else currentCategoryState.page
            try {
                val newItem = repository.getOnStreamingNetflix(page)
                val updatedItems = if (loadMore) currentCategoryState.items + newItem else newItem
                _state.value = state.value.copy(
                    onStreamingNetflix = currentCategoryState.copy(
                        items = updatedItems, page = page
                    )
                )
            } catch (e: Exception) {
                _state.value = state.value.copy(isLoadingMore = false)
            }
        }
    }

    private fun loadOnStreamingPrimeVideo(loadMore: Boolean = false) {
        viewModelScope.launch {
            val currentCategoryState = state.value.onStreamingPrimeVideo
            val page = if (loadMore) currentCategoryState.page + 1 else currentCategoryState.page
            try {
                val newItem = repository.getOnStreamingPrimeVideo(page)
                val updatedItems = if (loadMore) currentCategoryState.items + newItem else newItem
                _state.value = state.value.copy(
                    onStreamingPrimeVideo = currentCategoryState.copy(
                        items = updatedItems, page = page
                    )
                )
            } catch (e: Exception) {
                _state.value = state.value.copy(isLoadingMore = false)
            }
        }
    }

    private fun loadOnStreamingAppleTV(loadMore: Boolean = false) {
        viewModelScope.launch {
            val currentCategoryState = state.value.onStreamingAppleTV
            val page = if (loadMore) currentCategoryState.page + 1 else currentCategoryState.page
            try {
                val newItem = repository.getOnStreamingAppleTV(page)
                val updatedItems = if (loadMore) currentCategoryState.items + newItem else newItem
                _state.value = state.value.copy(
                    onStreamingAppleTV = currentCategoryState.copy(
                        items = updatedItems, page = page
                    )
                )
            } catch (e: Exception) {
                _state.value = state.value.copy(isLoadingMore = false)
            }
        }
    }

    private fun loadOnStreamingDisneyPlus(loadMore: Boolean = false) {
        viewModelScope.launch {
            val currentCategoryState = state.value.onStreamingDisneyPlus
            val page = if (loadMore) currentCategoryState.page + 1 else currentCategoryState.page
            try {
                val newItem = repository.getOnStreamingDisneyPlus(page)
                val updatedItems = if (loadMore) currentCategoryState.items + newItem else newItem
                _state.value = state.value.copy(
                    onStreamingDisneyPlus = currentCategoryState.copy(
                        items = updatedItems, page = page
                    )
                )
            } catch (e: Exception) {
                _state.value = state.value.copy(isLoadingMore = false)
            }
        }
    }

}