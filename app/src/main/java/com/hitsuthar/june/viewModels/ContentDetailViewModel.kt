package com.hitsuthar.june.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.moviebase.tmdb.model.TmdbEpisode
import app.moviebase.tmdb.model.TmdbEpisodeDetail
import app.moviebase.tmdb.model.TmdbMovieDetail
import app.moviebase.tmdb.model.TmdbShowDetail
import com.hitsuthar.june.utils.TmdbRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class ContentDetail {
    data class Movie(val tmdbMovieDetail: TmdbMovieDetail) : ContentDetail()
    data class Show(val tmdbShowDetail: TmdbShowDetail, var tmdbEpisode: TmdbEpisode?) :
        ContentDetail()

    data class Episode(val contentId: Int?, val tmdbEpisodeDetail: TmdbEpisodeDetail) :
        ContentDetail()

    object Loading : ContentDetail()
    object Error : ContentDetail()
}

class ContentDetailViewModel : ViewModel() {
    private val _contentDetail = MutableStateFlow<ContentDetail>(ContentDetail.Loading)
    val contentDetail: StateFlow<ContentDetail> = _contentDetail.asStateFlow()


    fun setContentDetail(
        contentId: Int? = null,
        episodeId: Int? = null,
        contentType: String?,
        repository: TmdbRepository,
        tmdbEpisode: TmdbEpisode? = null
    ) {
        viewModelScope.launch {
            _contentDetail.update { ContentDetail.Loading }

            if (contentId == null || contentType.isNullOrEmpty()) {
                _contentDetail.update { ContentDetail.Error }
                return@launch
            }
            Log.d(contentId.toString(), contentType)
            try {
                _contentDetail.update {
                    when (contentType) {
                        "movie" -> ContentDetail.Movie(repository.getMovieDetail(contentId))
                        "tv" -> ContentDetail.Show(
                            repository.getShowDetail(contentId), null
                        )

                        "episode" -> ContentDetail.Episode(
                            contentId,
                            repository.getShowEpisodeDetail(
                                contentId,
                                tmdbEpisode?.seasonNumber!!,
                                tmdbEpisode.episodeNumber
                            )
                        )

                        else -> ContentDetail.Error
                    }
                }
            } catch (e: Exception) {
                Log.e("ContentDetailViewModel", "Error fetching content details", e)
                _contentDetail.update { ContentDetail.Error }
            }
        }
    }
}
