package com.hitsuthar.june

import app.moviebase.tmdb.model.TmdbMovie
import app.moviebase.tmdb.model.TmdbShow

sealed class MediaItem {
    data class Movie(val movie: TmdbMovie) : MediaItem()
    data class TvShow(val show: TmdbShow) : MediaItem()
}