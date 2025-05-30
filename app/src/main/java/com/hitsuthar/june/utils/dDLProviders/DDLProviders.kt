package com.hitsuthar.june.utils.dDLProviders

import android.util.Log
import app.moviebase.tmdb.model.TmdbEpisode
import app.moviebase.tmdb.model.TmdbMovieDetail
import app.moviebase.tmdb.model.TmdbShowDetail
import com.hitsuthar.june.screens.MediaContent
import com.hitsuthar.june.utils.DocumentFetcher

data class DDLProvider(
  val name: String,
  val itemFetcher: suspend (TmdbMovieDetail) -> Result<MediaContent.Movie>,
  val itemFetcherTv: suspend (TmdbShowDetail, TmdbEpisode) -> Result<MediaContent.TvSeries>,
) {
  suspend fun fetchMoviesStreams(tmdbMovieDetail: TmdbMovieDetail): Result<MediaContent> {
    return itemFetcher(tmdbMovieDetail)
  }

  suspend fun fetchShowsStreams(
    tmdbShowDetail: TmdbShowDetail, tmdbEpisode: TmdbEpisode
  ): Result<MediaContent> {
    Log.d("fetch", tmdbShowDetail.externalIds.toString())
    return itemFetcherTv(tmdbShowDetail, tmdbEpisode)
  }
}

val fetcher = DocumentFetcher()

val DDLProviders = listOf(
//  DDLProvider("Movies Drive", { tmdbMovieDetail ->
//    getMoviesDriveDDL(
//      title = tmdbMovieDetail.title,
//      year = tmdbMovieDetail.releaseDate?.year,
//      type = "movie",
//      fetcher = fetcher
//    ).map { it as MediaContent.Movie }
//  }, { tmdbShowDetail, tmdbEpisode ->
//    getMoviesDriveDDL(
//      title = tmdbShowDetail.name,
//      type = "tv",
//      season = tmdbEpisode.seasonNumber,
//      episode = tmdbEpisode.episodeNumber,
//      fetcher = fetcher
//    ).map {
//      // Convert the result to ensure proper TvSeries structure
//      when (it) {
//        is MediaContent.TvSeries -> it
//        else -> MediaContent.TvSeries(emptyList()) // Fallback empty structure
//      }
//    }
//  }),
//    DDLProvider("Open Directory", { tmdbMovieDetail ->
//        getOpenDirectoryDDL(
//            title = tmdbMovieDetail.title,
//            year = tmdbMovieDetail.releaseDate?.year,
//            type = "movie",
//            fetcher = fetcher
//        )
//    }, { tmdbShowDetail, tmdbEpisode ->
//        getOpenDirectoryDDL(
//            title = tmdbShowDetail.name,
//            type = "tv",
//            season = tmdbEpisode.seasonNumber,
//            episode = tmdbEpisode.episodeNumber,
//            fetcher = fetcher
//        )
//    }),
//  DDLProvider("Rog Movies", { tmdbMovieDetail ->
//    getRogMoviesDDL(
//      title = tmdbMovieDetail.title,
//      year = tmdbMovieDetail.releaseDate?.year,
//      type = "movie",
//      fetcher = fetcher
//    ).map { it as MediaContent.Movie }
//  }, { tmdbShowDetail, tmdbEpisode ->
//    getRogMoviesDDL(
//      title = tmdbShowDetail.name,
//      type = "tv",
//      season = tmdbEpisode.seasonNumber,
//      episode = tmdbEpisode.episodeNumber,
//      fetcher = fetcher
//    ).map {
//      when (it) {
//        is MediaContent.TvSeries -> it
//        else -> MediaContent.TvSeries(emptyList()) // Fallback empty structure
//      }
//    }
//  }),
//  DDLProvider("Movies Mod", { tmdbMovieDetail ->
//    getMoviesModDDL(
//      title = tmdbMovieDetail.title,
//      year = tmdbMovieDetail.releaseDate?.year,
//      type = "movie",
//      fetcher = fetcher
//    ).map { it as MediaContent.Movie }
//  }, { tmdbShowDetail, tmdbEpisode ->
//    getMoviesModDDL(
//      title = tmdbShowDetail.name,
//      type = "tv",
//      season = tmdbEpisode.seasonNumber,
//      episode = tmdbEpisode.episodeNumber,
//      fetcher = fetcher
//    ).map {
//      when (it) {
//        is MediaContent.TvSeries -> it
//        else -> MediaContent.TvSeries(emptyList()) // Fallback empty structure
//      }
//    }
//  }),
//  DDLProvider("UHD Movies", { tmdbMovieDetail ->
//    getUHDMoviesDDL(
//      title = tmdbMovieDetail.title,
//      year = tmdbMovieDetail.releaseDate?.year,
//      type = "movie",
//      fetcher = fetcher
//    ).map { it as MediaContent.Movie }
//  }, { tmdbShowDetail, tmdbEpisode ->
//    getUHDMoviesDDL(
//      title = tmdbShowDetail.name,
//      type = "tv",
//      season = tmdbEpisode.seasonNumber,
//      episode = tmdbEpisode.episodeNumber,
//      fetcher = fetcher
//    ).map {
//      when (it) {
//        is MediaContent.TvSeries -> it
//        else -> MediaContent.TvSeries(emptyList()) // Fallback empty structure
//      }
//    }
//  }),
//  DDLProvider("Cinema Lux", { tmdbMovieDetail ->
//    getCinemaLuxDDL(
//      title = tmdbMovieDetail.title,
//      year = tmdbMovieDetail.releaseDate?.year,
//      type = "movie",
//      fetcher = fetcher
//    ).map { it as MediaContent.Movie }
//  }, { tmdbShowDetail, tmdbEpisode ->
//    getCinemaLuxDDL(
//      title = "${tmdbShowDetail.name} ${tmdbShowDetail.firstAirDate?.year}",
//      type = "tv",
//      season = tmdbEpisode.seasonNumber,
//      episode = tmdbEpisode.episodeNumber,
//      fetcher = fetcher
//    ).map {
//      when (it) {
//        is MediaContent.TvSeries -> it
//        else -> MediaContent.TvSeries(emptyList()) // Fallback empty structure
//      }
//    }
//  }),
  DDLProvider("HdHub4U", { tmdbMovieDetail ->
    getHdHub4UDDL(
      title = tmdbMovieDetail.title,
      year = tmdbMovieDetail.releaseDate?.year,
      type = "movie",
      fetcher = fetcher
    ).map { it as MediaContent.Movie }
  }, { tmdbShowDetail, tmdbEpisode ->
    getHdHub4UDDL(
      title = tmdbShowDetail.name,
      type = "tv",
      season = tmdbEpisode.seasonNumber,
      episode = tmdbEpisode.episodeNumber,
      fetcher = fetcher
    ).map {
      when (it) {
        is MediaContent.TvSeries -> it
        else -> MediaContent.TvSeries(emptyList()) // Fallback empty structure
      }
    }
  }),
)