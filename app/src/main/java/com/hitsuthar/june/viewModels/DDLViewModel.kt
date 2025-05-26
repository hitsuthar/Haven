package com.hitsuthar.june.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitsuthar.june.screens.MediaContent
import com.hitsuthar.june.utils.dDLProviders.DDLProviders
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed class DDLState {
  object Loading : DDLState()
  data class PartialSuccess(
    val availableProviders: List<String>,
    val loadingProviders: List<String>,
    val failedProviders: Map<String, String>
  ) : DDLState()

  data class Error(val message: String) : DDLState()
}

object DDLResponseCache {
  data class CacheEntry(
    val providerResults: Map<String, MediaContent>,
    val failedProviders: Map<String, String>
  )

  private val cache = mutableMapOf<ContentDetail, CacheEntry>()
  private val lock = Mutex()

  suspend fun get(key: ContentDetail): CacheEntry? = lock.withLock {
    cache[key]
  }

  suspend fun put(
    key: ContentDetail,
    providerName: String,
    results: MediaContent
  ) = lock.withLock {
    val current = cache[key] ?: CacheEntry(mutableMapOf(), mutableMapOf())
    val updatedResults = current.providerResults + (providerName to results)
    cache[key] = current.copy(providerResults = updatedResults)
  }

  suspend fun putFailed(
    key: ContentDetail,
    providerName: String,
    error: String
  ) = lock.withLock {
    val current = cache[key] ?: CacheEntry(mutableMapOf(), mutableMapOf())
    val updatedFailures = current.failedProviders + (providerName to error)
    cache[key] = current.copy(failedProviders = updatedFailures)
  }

  suspend fun clear() = lock.withLock {
    cache.clear()
  }

  suspend fun getProviders(key: ContentDetail): List<String> = lock.withLock {
    cache[key]?.providerResults?.keys?.toList() ?: emptyList()
  }

  suspend fun getFailedProviders(key: ContentDetail): Map<String, String> = lock.withLock {
    cache[key]?.failedProviders ?: emptyMap()
  }
}

class DDLViewModel : ViewModel() {
  private val _state = MutableStateFlow<DDLState>(DDLState.Loading)
  val state: StateFlow<DDLState> = _state

  private val _selectedProvider = MutableStateFlow<String?>(null)
  val selectedProvider: StateFlow<String?> = _selectedProvider

  private val _currentStreams = MutableStateFlow<MediaContent?>(null)
  val currentStreams: StateFlow<MediaContent?> = _currentStreams

  //    private var cachedProvidersData = mutableMapOf<String, List<DDLStream>>()
  private val loadingProviders = mutableSetOf<String>()
  private val failedProviders = mutableMapOf<String, String>()
  private var currentContentDetail: ContentDetail? = null

  fun selectProvider(providerName: String) {
//        if (cachedProvidersData.containsKey(providerName)) {
//            _selectedProvider.value = providerName
//            _currentStreams.value = cachedProvidersData[providerName] ?: emptyList()
//        }
    viewModelScope.launch {
      currentContentDetail?.let { contentDetail ->
        DDLResponseCache.get(contentDetail)?.providerResults?.get(providerName)
          ?.let { streams ->
            _selectedProvider.value = providerName
            _currentStreams.value = streams
          }
      }
    }
  }

  fun fetchAllProviders(contentDetail: ContentDetail) {
    currentContentDetail = contentDetail
    viewModelScope.launch {
      // Reset UI state immediately when receiving new content detail
      _currentStreams.value = null
      _selectedProvider.value = null

      val cachedEntry = DDLResponseCache.get(contentDetail)
      if (cachedEntry != null &&
        (cachedEntry.providerResults.isNotEmpty() || cachedEntry.failedProviders.isNotEmpty())
      ) {
        // Data exists in cache - use it
        _state.value = DDLState.PartialSuccess(
          availableProviders = cachedEntry.providerResults.keys.toList(),
          loadingProviders = emptyList(),
          failedProviders = cachedEntry.failedProviders
        )

        // Auto-select first provider if none selected
        if (cachedEntry.providerResults.isNotEmpty()) {
          selectProvider(cachedEntry.providerResults.keys.first())
        }
        return@launch
      }
      // No cached data - proceed with fetching
      _state.value = DDLState.Loading
//            cachedProvidersData.clear()
      loadingProviders.clear()
      failedProviders.clear()
      _currentStreams.value = null
      _selectedProvider.value = null

      loadingProviders.addAll(DDLProviders.map { it.name })

      DDLProviders.map { provider ->
        async {
          try {
            val result = when (contentDetail) {
              is ContentDetail.Movie -> {
                provider.fetchMoviesStreams(contentDetail.tmdbMovieDetail).let {
                  Result.success(it)
                }.getOrThrow()
              }

              is ContentDetail.Show -> {
                provider.fetchShowsStreams(
                  contentDetail.tmdbShowDetail, contentDetail.tmdbEpisode!!
                )
              }

              else -> {
                Result.failure(Throwable("Unknown content type"))
              }
            }
            loadingProviders.remove(provider.name)

            if (result.isSuccess) {
                val mediaContent = result.getOrThrow()
                if (mediaContent.streams.isNotEmpty()) {
          //                                cachedProvidersData[provider.name] = streams
                  DDLResponseCache.put(contentDetail, provider.name, mediaContent)
                  // Auto-select first successful provider
                  if (_selectedProvider.value == null) {
                    selectProvider(provider.name)
                  }
                } else {
                  val error = "Not Found"
                  failedProviders[provider.name] = error
                  DDLResponseCache.putFailed(contentDetail, provider.name, error)
                }
              } else {
                val error = "Not Found"
                failedProviders[provider.name] = error
                DDLResponseCache.putFailed(contentDetail, provider.name, error)
              }
            updateState(contentDetail)
          } catch (e: Exception) {
            loadingProviders.remove(provider.name)
            val error = e.message ?: "Unknown error"
            failedProviders[provider.name] = error
            DDLResponseCache.putFailed(contentDetail, provider.name, error)
            updateState(contentDetail)
          }
        }
      }.awaitAll()
    }
  }

  private suspend fun updateState(contentDetail: ContentDetail) {
    val cachedEntry = DDLResponseCache.get(contentDetail)
    cachedEntry?.let {
      _state.value = DDLState.PartialSuccess(
        availableProviders = it.providerResults.keys.toList(),
        loadingProviders = loadingProviders.toList(),
        failedProviders = it.failedProviders
      )

      // Ensure we have valid selection
      if (_selectedProvider.value == null && it.providerResults.isNotEmpty()) {
        selectProvider(it.providerResults.keys.first())
      }
    } ?: run {
      _state.value = when {
        loadingProviders.isNotEmpty() -> DDLState.Loading
        else -> DDLState.Error("No links found :(")
      }
    }
  }
}