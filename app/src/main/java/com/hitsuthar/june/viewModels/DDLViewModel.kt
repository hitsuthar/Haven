package com.hitsuthar.june.viewModels

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitsuthar.june.screens.DDLStream
import com.hitsuthar.june.utils.dDLProviders.DDLProviders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class DDLState {
    object Loading : DDLState()
    data class PartialSuccess(
        val availableProviders: List<String>,
        val loadingProviders: List<String>,
        val failedProviders: Map<String, String>
    ) : DDLState()
    data class Error(val message: String) : DDLState()
}

class DDLViewModel : ViewModel() {
    private val _state = MutableStateFlow<DDLState>(DDLState.Loading)
    val state: StateFlow<DDLState> = _state

    private val _selectedProvider = MutableStateFlow<String?>(null)
    val selectedProvider: StateFlow<String?> = _selectedProvider

    private val _currentStreams = MutableStateFlow<List<DDLStream>>(emptyList())
    val currentStreams: StateFlow<List<DDLStream>> = _currentStreams

    private var cachedProvidersData = mutableMapOf<String, List<DDLStream>>()
    private val loadingProviders = mutableSetOf<String>()
    private val failedProviders = mutableMapOf<String, String>()

    fun selectProvider(providerName: String) {
        if (cachedProvidersData.containsKey(providerName)) {
            _selectedProvider.value = providerName
            _currentStreams.value = cachedProvidersData[providerName] ?: emptyList()
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun fetchAllProviders(contentDetail: ContentDetail) {
        viewModelScope.launch {
            _state.value = DDLState.Loading
            cachedProvidersData.clear()
            loadingProviders.clear()
            failedProviders.clear()
            _currentStreams.value = emptyList()
            _selectedProvider.value = null

            loadingProviders.addAll(DDLProviders.map { it.name })

            DDLProviders.map { provider ->
                launch {
                    try {
                        val result = when (contentDetail) {
                            is ContentDetail.Movie -> {
                                provider.fetchMoviesStreams(contentDetail.tmdbMovieDetail)
                            }

                            is ContentDetail.Show -> {
                                contentDetail.tmdbEpisode?.let { episode ->
                                    provider.fetchShowsStreams(
                                        contentDetail.tmdbShowDetail,
                                        episode
                                    )
                                } ?: Result.failure(Throwable("No episode data available"))
                            }

                            else -> {
                                Result.failure(Throwable("Unknown content type"))
                            }
                        }
                        loadingProviders.remove(provider.name)

                        if (result.isSuccess) {
                            val streams = result.getOrThrow()
                            if (streams.isNotEmpty()) {
                                cachedProvidersData[provider.name] = streams
                                // Auto-select first successful provider
                                if (_selectedProvider.value == null) {
                                    selectProvider(provider.name)
                                }
                            }
                        } else {
                            failedProviders[provider.name] =
                                result.exceptionOrNull()?.message ?: "Unknown error"
                        }
                        updateState()
                    } catch (e: Exception) {
                        loadingProviders.remove(provider.name)
                        failedProviders[provider.name] = e.message ?: "Unknown error"
                        updateState()
                    }
                }
            }
        }
    }

    private fun updateState() {
        _state.value = when {
            cachedProvidersData.isNotEmpty() -> DDLState.PartialSuccess(
                availableProviders = cachedProvidersData.keys.toList(),
                loadingProviders = loadingProviders.toList(),
                failedProviders = failedProviders
            )

            loadingProviders.isNotEmpty() -> DDLState.Loading
            else -> DDLState.Error(
                "All providers failed:\n${
                    failedProviders.entries.joinToString("\n") { "${it.key}: ${it.value}" }
                }"
            )
        }
    }
}