package com.hitsuthar.june.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.videolan.libvlc.MediaPlayer


class VideoPlayerViewModel : ViewModel() {
    // State variables wrapped in mutableStateOf
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentDuration = MutableStateFlow(0L)
    val currentDuration = _currentDuration.asStateFlow()

    private val _totalDuration = MutableStateFlow(0L)
    var totalDuration = _totalDuration.asStateFlow()

    private val _audioTracks = MutableStateFlow(emptyList<MediaPlayer.TrackDescription>())
    var audioTracks = _audioTracks.asStateFlow()

    private val _selectedAudioTrack = MutableStateFlow(1)
    var selectedAudioTrack = _selectedAudioTrack.asStateFlow()

    private val _subtitleTracks = MutableStateFlow(emptyList<MediaPlayer.TrackDescription>())
    var subtitleTracks = _subtitleTracks.asStateFlow()

    private val _selectedSubtitleTrack = MutableStateFlow(-1)
    var selectedSubtitleTrack = _selectedSubtitleTrack.asStateFlow()

    private val _videoUrl = MutableStateFlow(null.toString())
    var videoUrl = _videoUrl.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    var isLoading = _isLoading.asStateFlow()

    private val _isFullScreen = MutableStateFlow(false)
    var isFullScreen = _isFullScreen.asStateFlow()

    // Functions to update state
    fun play() {
        _isPlaying.update { true }
        Log.d("VideoPlayerViewModel", "play: ")
    }

    fun pause() {
        _isPlaying.update { false }
    }

    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
    }

    fun updateCurrentDuration(duration: Long) {
        _currentDuration.value = duration
    }

    fun updateTotalDuration(duration: Long) {
        _totalDuration.value = duration
    }

    fun updateAudioTracks(tracks: List<MediaPlayer.TrackDescription>) {
        _audioTracks.value = tracks
    }

    fun selectAudioTrack(trackId: Int) {
        _selectedAudioTrack.value = trackId
    }

    fun updateSubtitleTracks(tracks: List<MediaPlayer.TrackDescription>) {
        _subtitleTracks.value = tracks
    }

    fun selectSubtitleTrack(trackId: Int) {
        _selectedSubtitleTrack.value = trackId
    }


    fun setVideoUrl(url: String?) {
        if (url != null) {
            _videoUrl.value = url
        }
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun toggleFullScreen() {
        _isFullScreen.value = !_isFullScreen.value
    }

    fun reset(){
        _isPlaying.value = false
        _currentDuration.value = 0L
        _totalDuration.value = 0L
        _audioTracks.value = emptyList()
        _selectedAudioTrack.value = 1
        _subtitleTracks.value = emptyList()
        _selectedSubtitleTrack.value = -1
        _videoUrl.value = null.toString()
        _isLoading.value = true
        _isFullScreen.value = false
    }
}