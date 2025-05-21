package com.hitsuthar.june

import app.moviebase.tmdb.model.TmdbMediaListItem

enum class MediaType{ MOVIE, TV_SHOW }

sealed class MediaCategory {
    object PopularMovies : MediaCategory()
    object PopularShows : MediaCategory()
    object TopRatedMovies : MediaCategory()
    object TopRatedShows : MediaCategory()
    object OnStreamingNetflix : MediaCategory()
    object OnStreamingPrimeVideo : MediaCategory()
    object OnStreamingAppleTV : MediaCategory()
    object OnStreamingDisneyPlus : MediaCategory()
}

data class MediaCategoryState(val items: List<TmdbMediaListItem> = emptyList(), val page: Int = 1)

data class MediaListState(
    val popularMovies: MediaCategoryState = MediaCategoryState(),
    val popularShows: MediaCategoryState = MediaCategoryState(),
    val topRatedMovies: MediaCategoryState = MediaCategoryState(),
    val topRatedShows: MediaCategoryState = MediaCategoryState(),
    val onStreamingNetflix: MediaCategoryState = MediaCategoryState(),
    val onStreamingPrimeVideo: MediaCategoryState = MediaCategoryState(),
    val onStreamingAppleTV: MediaCategoryState = MediaCategoryState(),
    val onStreamingDisneyPlus: MediaCategoryState = MediaCategoryState(),
    val isLoadingMore: Boolean = false
)