package com.hitsuthar.june.utils.dDLProviders

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import app.moviebase.tmdb.model.TmdbEpisode
import app.moviebase.tmdb.model.TmdbMovieDetail
import app.moviebase.tmdb.model.TmdbShowDetail
import com.hitsuthar.june.screens.DDLStream
import com.hitsuthar.june.utils.DocumentFetcher

data class DDLProvider(
    val name: String,
    val itemFetcher: suspend (TmdbMovieDetail) -> Result<List<DDLStream>>,
    val itemFetcherTv: suspend (TmdbShowDetail, TmdbEpisode) -> Result<List<DDLStream>>,
) {
    suspend fun fetchMoviesStreams(tmdbMovieDetail: TmdbMovieDetail): Result<List<DDLStream>> {
        return itemFetcher(tmdbMovieDetail)
    }

    suspend fun fetchShowsStreams(
        tmdbShowDetail: TmdbShowDetail, tmdbEpisode: TmdbEpisode
    ): Result<List<DDLStream>> {
        Log.d("fetch", tmdbShowDetail.externalIds.toString())
        return itemFetcherTv(tmdbShowDetail, tmdbEpisode)
    }
}

val fetcher = DocumentFetcher()

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
val DDLProviders = listOf(
    DDLProvider("Movies Drive", { tmdbMovieDetail ->
        getMoviesDriveDDL(
            title = tmdbMovieDetail.title,
            year = tmdbMovieDetail.releaseDate?.year,
            type = "movie",
            fetcher = fetcher
        )
    }, { tmdbShowDetail, tmdbEpisode ->
        getMoviesDriveDDL(
            title = tmdbShowDetail.name,
            type = "tv",
            season = tmdbEpisode.seasonNumber,
            episode = tmdbEpisode.episodeNumber,
            fetcher = fetcher
        )
    }),
    DDLProvider("Open Directory", { tmdbMovieDetail ->
        getOpenDirectoryDDL(
            title = tmdbMovieDetail.title,
            year = tmdbMovieDetail.releaseDate?.year,
            type = "movie",
            fetcher = fetcher
        )
    }, { tmdbShowDetail, tmdbEpisode ->
        getOpenDirectoryDDL(
            title = tmdbShowDetail.name,
            type = "tv",
            season = tmdbEpisode.seasonNumber,
            episode = tmdbEpisode.episodeNumber,
            fetcher = fetcher
        )
    }),
    DDLProvider("Rog Movies", { tmdbMovieDetail ->
        getRogMoviesDDL(
            title = tmdbMovieDetail.title,
            year = tmdbMovieDetail.releaseDate?.year,
            type = "movie",
            fetcher = fetcher
        )
    }, { tmdbShowDetail, tmdbEpisode ->
        getRogMoviesDDL(
            title = tmdbShowDetail.name,
            type = "tv",
            season = tmdbEpisode.seasonNumber,
            episode = tmdbEpisode.episodeNumber,
            fetcher = fetcher
        )
    }),
)