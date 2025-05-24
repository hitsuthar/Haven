package com.hitsuthar.june.utils

import app.moviebase.tmdb.Tmdb3
import app.moviebase.tmdb.discover.DiscoverCategory
import app.moviebase.tmdb.model.AppendResponse
import app.moviebase.tmdb.model.TmdbDiscoverFilter
import app.moviebase.tmdb.model.TmdbEpisodeDetail
import app.moviebase.tmdb.model.TmdbMediaListItem
import app.moviebase.tmdb.model.TmdbMediaType
import app.moviebase.tmdb.model.TmdbMovieDetail
import app.moviebase.tmdb.model.TmdbSeasonDetail
import app.moviebase.tmdb.model.TmdbShowDetail
import app.moviebase.tmdb.model.TmdbWatchProviderId


class TmdbRepository(tbdApiKey: String) {
    companion object {
        private const val LANGUAGE = "en"
        private const val REGION = "US"
    }

    private val tmdbClient: Tmdb3 = Tmdb3(tbdApiKey)

    private suspend fun discoverMedia(
        page: Int = 1,
        language: String = LANGUAGE,
        region: String? = null,
        category: DiscoverCategory
    ): List<TmdbMediaListItem> {
        return tmdbClient.discover.discoverByCategory(
            page = page, language = language, region = region, category = category
        ).results
    }

    suspend fun getPopularMovies(page: Int = 1): List<TmdbMediaListItem> {
        return discoverMedia(
            page = page,
            language = LANGUAGE,
            region = REGION,
            category = DiscoverCategory.Popular(mediaType = TmdbMediaType.MOVIE),
        )
    }

    suspend fun getPopularShows(page: Int = 1): List<TmdbMediaListItem> {
        return discoverMedia(
            page = page,
            language = LANGUAGE,
            region = REGION,
            category = DiscoverCategory.Popular(mediaType = TmdbMediaType.SHOW),
        )
    }

    suspend fun getTopRatedMovies(page: Int = 1): List<TmdbMediaListItem> {
        return discoverMedia(
            page = page,
            language = LANGUAGE,
            region = REGION,
            category = DiscoverCategory.TopRated(mediaType = TmdbMediaType.MOVIE)
        )
    }

    suspend fun getTopRatedShows(page: Int = 1): List<TmdbMediaListItem> {
        return discoverMedia(
            page = page,
            language = LANGUAGE,
            region = REGION,
            category = DiscoverCategory.TopRated(mediaType = TmdbMediaType.SHOW)
        )
    }

    suspend fun getOnStreamingNetflix(page: Int = 1): List<TmdbMediaListItem> {
        return discoverMedia(
            page = page,
            region = REGION,
            language = LANGUAGE,
            category = DiscoverCategory.OnStreaming.Netflix(
                mediaType = TmdbMediaType.SHOW, watchRegion = REGION
            )
        )
    }

    suspend fun getOnStreamingAppleTV(page: Int = 1): List<TmdbMediaListItem> {
        return discoverMedia(
            page = page,
            language = LANGUAGE,
            region = REGION,
            category = DiscoverCategory.OnStreaming.AppleTv(
                mediaType = TmdbMediaType.SHOW, watchRegion = REGION
            )
        )
    }

    suspend fun getOnStreamingDisneyPlus(page: Int = 1): List<TmdbMediaListItem> {
        return discoverMedia(
            page = page,
            language = LANGUAGE,
            region = REGION,
            category = DiscoverCategory.OnStreaming.DisneyPlus(
                mediaType = TmdbMediaType.SHOW, watchRegion = REGION
            )
        )
    }

    suspend fun getOnStreamingPrimeVideo(page: Int = 1): List<TmdbMediaListItem> {
        return discoverMedia(
            page = page,
            language = LANGUAGE,
            region = REGION,
            category = DiscoverCategory.OnStreaming(
                mediaType = TmdbMediaType.SHOW,
                watchRegion = REGION,
                watchProviders = TmdbDiscoverFilter(items = listOf(TmdbWatchProviderId.Flatrate.AMAZON_PRIME_VIDEO_TIER_A)),
            )
        )
    }

    suspend fun search(query: String, page: Int = 1): List<TmdbMediaListItem> {
        return tmdbClient.search.findMulti(
            query = query, page = page
        ).results.filterIsInstance<TmdbMediaListItem>()
    }

    suspend fun getMovieDetail(movieId: Int): TmdbMovieDetail {
        return tmdbClient.movies.getDetails(
            movieId = movieId, appendResponses = listOf(AppendResponse.IMAGES)
        )
    }

    suspend fun getShowDetail(showId: Int): TmdbShowDetail {
        return tmdbClient.show.getDetails(
            showId = showId,
            appendResponses = listOf(AppendResponse.IMAGES, AppendResponse.EXTERNAL_IDS)
        )
    }

    suspend fun getShowSeasonDetail(showId: Int, seasonNumber: Int): TmdbSeasonDetail {
        return tmdbClient.showSeasons.getDetails(
            showId, seasonNumber, appendResponses = listOf(AppendResponse.EXTERNAL_IDS)
        )
    }

    suspend fun getShowEpisodeDetail(
        showId: Int,
        seasonNumber: Int,
        episodeNumber: Int
    ): TmdbEpisodeDetail {
        return tmdbClient.showEpisodes.getDetails(
            showId = showId,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            appendResponses = listOf(AppendResponse.EXTERNAL_IDS),
        )
    }

}