package com.hitsuthar.june.viewModels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.activity.result.launch
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.lang.ref.WeakReference

data class VideoPlayerUIState(
  val videoUrl: String? = null,
  val title: String? = null,
  val isPlaying: Boolean = false,
  val isLoading: Boolean = true, // Start with loading true until media is prepared
  val isBuffering: Boolean = false,
  val bufferingProgress: Float = 0f,
  val currentDuration: Long? = null,
  val totalDuration: Long? = null,
  val audioTracks: List<MediaPlayer.TrackDescription> = emptyList(),
  val selectedAudioTrackId: Int = -1, // LibVLC usually uses -1 for default/no selection or ID
  val subtitleTracks: List<MediaPlayer.TrackDescription> = emptyList(),
  val selectedSubtitleTrackId: Int = -1,
  val isFullScreen: Boolean = false,
  val error: String? = null
)

class VideoPlayerViewModel(application: Application) : AndroidViewModel(application) {
  //  private companion object {
  val TAG = "VideoPlayerViewModel"
  val vlcOptions = ArrayList<String>().apply {
//    add("--network-caching=10000") // Medium buffer for streaming
//    add("--no-video-paused")
    add("--clock-jitter=0")
    add("--clock-synchro=0")
    add("--drop-late-frames")
    add("--skip-frames")
    add("--avcodec-fast")
    add("--avcodec-hw=any")
  }
//  }

  private val _libVLC: LibVLC =
    LibVLC(application, vlcOptions) // Initialize VLC options here or pass them
  private val _mediaPlayer: MediaPlayer = MediaPlayer(_libVLC)
  val mediaPlayer = _mediaPlayer

  private val _uiState = MutableStateFlow(VideoPlayerUIState())
  val uiState = _uiState.asStateFlow()

  private var vlcVideoLayoutRef: WeakReference<VLCVideoLayout>? = null
  private var progressTrackerJob: Job? = null
  private var wasPlayingBeforePause: Boolean = false // For ON_PAUSE/ON_RESUME

  init {
    setupMediaPlayerEventListeners()
  }


  private fun setupMediaPlayerEventListeners() {
    _mediaPlayer.setEventListener { event ->
      when (event.type) {
        MediaPlayer.Event.Playing -> {
          Log.d(TAG, "Event: Playing")
          _uiState.update {
            it.copy(
              isPlaying = true, isLoading = false, isBuffering = false
            )
          }
          startProgressTracker()
        }

        MediaPlayer.Event.Paused -> {
          Log.d(TAG, "Event: Paused")
          _uiState.update { it.copy(isPlaying = false, isBuffering = false) }
          stopProgressTracker()
        }

        MediaPlayer.Event.Stopped -> {
          Log.d(TAG, "Event: Stopped")
          _uiState.update { it.copy(isPlaying = false, isLoading = false, currentDuration = 0L) }
          stopProgressTracker()
        }

        MediaPlayer.Event.EndReached -> {
          Log.d(TAG, "Event: EndReached")
          _uiState.update {
            it.copy(
              isPlaying = false, currentDuration = it.totalDuration, // Or 0L to reset
              isLoading = false
            )
          }
          stopProgressTracker()
        }

        MediaPlayer.Event.EncounteredError -> {
          Log.e(TAG, "Event: EncounteredError")
          _uiState.update {
            it.copy(
              error = "Video playback error.", isLoading = false, isPlaying = false
            )
          }
          stopProgressTracker()
        }

        MediaPlayer.Event.MediaChanged -> {
          Log.d(TAG, "Event: MediaChanged")
          _uiState.update {
            it.copy(
              isLoading = true, // New media, start loading
              currentDuration = 0L,
              totalDuration = 0L,
              audioTracks = emptyList(),
              subtitleTracks = emptyList(),
              selectedAudioTrackId = _mediaPlayer.audioTrack,
              selectedSubtitleTrackId = _mediaPlayer.spuTrack
            )
          }
        }

        MediaPlayer.Event.Opening -> {
          Log.d(TAG, "Event: Opening")
          _uiState.update { it.copy(isLoading = true, error = null) }
        }

        MediaPlayer.Event.Buffering -> {
          // Log.d(TAG, "Event: Buffering ${event.buffering}")
          val isActuallyBuffering = event.buffering < 100f && event.buffering >= 0f
          if (_uiState.value.isBuffering != isActuallyBuffering || _uiState.value.bufferingProgress != event.buffering) {
            _uiState.update {
              it.copy(
                isBuffering = isActuallyBuffering,
                bufferingProgress = event.buffering,
                isLoading = if (isActuallyBuffering) true else it.isLoading // Keep loading if buffering starts
              )
            }
          }
        }

        MediaPlayer.Event.TimeChanged -> {
          // This event is frequent, update only if significantly different or rely on manual tracker
          // _uiState.update { it.copy(currentDuration = event.timeChanged) }
        }

        MediaPlayer.Event.LengthChanged -> {
          Log.d(TAG, "Event: LengthChanged ${event.lengthChanged}")
          _uiState.update { it.copy(totalDuration = event.lengthChanged) }
        }


        MediaPlayer.Event.ESAdded, MediaPlayer.Event.ESDeleted, MediaPlayer.Event.ESSelected -> {
          Log.d(TAG, "Event: ES event - refreshing tracks")
          // Elementary Stream event, refresh tracks
          refreshTracks()
        }

        // Add more event handling as needed
        else -> {
          // Log.v(TAG, "MediaPlayer Event: ${event.type}")
        }
      }
    }
  }

  private fun refreshTracks() {
    val audio = _mediaPlayer.audioTracks?.toList() ?: emptyList()
    val subtitles = _mediaPlayer.spuTracks?.toList() ?: emptyList()
    _uiState.update {
      it.copy(
        audioTracks = audio,
        subtitleTracks = subtitles,
        selectedAudioTrackId = _mediaPlayer.audioTrack, // Ensure current selection is reflected
        selectedSubtitleTrackId = _mediaPlayer.spuTrack
      )
    }
    Log.d(TAG, "Refreshed Audio Tracks: ${audio.size}, Subtitle Tracks: ${subtitles.size}")
    Log.d(
      TAG,
      "Selected Audio ID: ${_mediaPlayer.audioTrack}, Selected Subtitle ID: ${_mediaPlayer.spuTrack}"
    )
  }

  fun loadVideo(url: String?, title: String? = "Video") {
    if (url == _uiState.value.videoUrl && _mediaPlayer.hasMedia() && (_mediaPlayer.isPlaying || _mediaPlayer.isSeekable)) {
      Log.d(TAG, "Video URL is the same and media is already loaded. Skipping reload.")
      // If the same video is "reloaded", decide if you want to restart or continue
      // For now, we do nothing if it's the same URL.
      // If you want to restart: _mediaPlayer.stop(); _mediaPlayer.play()
      return
    }

    Log.d(TAG, "Loading video: $url, Title: $title")
    _uiState.update {
      it.copy(
        videoUrl = url,
        title = title,
        isLoading = true,
        currentDuration = 0L,
        totalDuration = 0L,
        error = null
      )
    }

    if (url.isNullOrBlank()) {
      _mediaPlayer.stop()
      _uiState.update { it.copy(isLoading = false, error = "No video URL provided.") }
      return
    }

    viewModelScope.launch {
      try {
        val media = Media(_libVLC, url.toUri()).apply {
          setHWDecoderEnabled(true, false) // Enable hardware decoding
          // Add specific media options if needed:
          addOption(":network-caching=10000") // Already in VLC_OPTIONS generally
          addOption(":file-caching=10000")          // File buffer in milliseconds
          addOption(":live-caching=10000")          // Live stream buffer
          addOption(":disc-caching=10000")          // Optical disc buffer
          addOption(":no-video-paused")
        }
        _mediaPlayer.media = media
        media.release() // Release the media object once set to player

        // Attach views if already available
        vlcVideoLayoutRef?.get()?.let { layout ->
          Log.d(TAG, "Attempting to attach views in loadVideo...")
          _mediaPlayer.attachViews(layout, null, false, false)
        }
        // Play is usually called by the Playing event or explicitly by user
        // _mediaPlayer.play() // Let event handler or user action trigger play
      } catch (e: Exception) {
        Log.e(TAG, "Error loading media", e)
        _uiState.update { it.copy(isLoading = false, error = "Failed to load video: ${e.message}") }
      }
    }
  }


  // State variables wrapped in mutableStateOf
//    private val _isPlaying = MutableStateFlow(false)
//    val isPlaying = _isPlaying.asStateFlow()
//
//    private val _isBuffering = MutableStateFlow<Boolean?>(null)
//    val isBuffering = _isBuffering.asStateFlow()
//
//    private val _buffering = MutableStateFlow<Float>(0f)
//    val buffering = _buffering.asStateFlow()
//
//    private val _currentDuration = MutableStateFlow(0L)
//    val currentDuration = _currentDuration.asStateFlow()
//
//    private val _totalDuration = MutableStateFlow(0L)
//    var totalDuration = _totalDuration.asStateFlow()
//
//    private val _audioTracks = MutableStateFlow(emptyList<MediaPlayer.TrackDescription>())
//    var audioTracks = _audioTracks.asStateFlow()
//
//    private val _selectedAudioTrack = MutableStateFlow(1)
//    var selectedAudioTrack = _selectedAudioTrack.asStateFlow()
//
//    private val _subtitleTracks = MutableStateFlow(emptyList<MediaPlayer.TrackDescription>())
//    var subtitleTracks = _subtitleTracks.asStateFlow()
//
//    private val _selectedSubtitleTrack = MutableStateFlow(-1)
//    var selectedSubtitleTrack = _selectedSubtitleTrack.asStateFlow()
//
//    private val _videoUrl = MutableStateFlow<String?>(null)
//    var videoUrl = _videoUrl.asStateFlow()
//
//    private val _isLoading = MutableStateFlow(false)
//    var isLoading = _isLoading.asStateFlow()
//
//    private val _isFullScreen = MutableStateFlow(false)
//    var isFullScreen = _isFullScreen.asStateFlow()
//
//    private val _title = MutableStateFlow<String?>(null)
//    var title = _title.asStateFlow()

  // Functions to update state
  fun play() {
    if (!_mediaPlayer.hasMedia()) {
      Log.w(TAG, "Play called but no media loaded.")
      // Optionally try to reload last known URL or show error
      _uiState.value.videoUrl?.let { loadVideo(it, _uiState.value.title) }
      return
    }
    if (!_mediaPlayer.isPlaying) {
      Log.d(TAG, "Requesting Play")
      _mediaPlayer.play()
    }
    Log.d("VideoPlayerViewModel", "play: ")
  }

  fun pause() {
    if (!_mediaPlayer.hasMedia()) {
      Log.w(TAG, "Play called but no media loaded.")
      // Optionally try to reload last known URL or show error
      _uiState.value.videoUrl?.let { loadVideo(it, _uiState.value.title) }
      return
    }
    if (_mediaPlayer.isPlaying) {
      Log.d(TAG, "Requesting Pause")
      _mediaPlayer.pause()
    }
  }

  fun togglePlayPause() {
    if (_mediaPlayer.isPlaying) {
      pause()
    } else {
      play()
    }
  }

  fun seekTo(positionMs: Long) {
    if (_mediaPlayer.isSeekable) {
      Log.d(TAG, "Seeking to: $positionMs")
      _mediaPlayer.time = positionMs
      // Update current duration immediately for responsiveness, event will follow
      _uiState.update { it.copy(currentDuration = positionMs) }
    } else {
      Log.w(TAG, "Cannot seek, media not seekable.")
    }
  }


  fun attachView(vlcVideoLayout: VLCVideoLayout) {
    Log.d(TAG, "Attaching VLCVideoLayout")
    this.vlcVideoLayoutRef = WeakReference(vlcVideoLayout)
    // It's crucial that attachViews is called when media is ready or layout is available
    if (_mediaPlayer.hasMedia()) {
      Log.d(TAG, "Attempting to attach views in loadVideo...")
      _mediaPlayer.attachViews(vlcVideoLayout, null, false, false)
    }
  }

  fun detachView() {
    Log.d(TAG, "Detaching VLCVideoLayout")
    _mediaPlayer.detachViews()
    this.vlcVideoLayoutRef?.clear()
  }

  // Call this from Activity/Fragment's onPause
  fun onHostPause() {
    Log.d(TAG, "Host onPause")
    wasPlayingBeforePause = _mediaPlayer.isPlaying
    if (wasPlayingBeforePause) {
      pause() // This will trigger event and update UI state
    }
    // Detach views when going to background to free resources and prevent issues
    // detachVideoLayout() // Or do this in onStop if preferred
  }

  // Call this from Activity/Fragment's onResume
  fun onHostResume() {
    Log.d(TAG, "Host onResume")
    // Re-attach views if they were detached
    vlcVideoLayoutRef?.get()?.let { layout ->
      attachView(layout) // Will re-run attachViews
    }
    if (wasPlayingBeforePause) {
      play() // This will trigger event and update UI state
      wasPlayingBeforePause = false // Reset flag
    }
  }

  private fun startProgressTracker() {
    stopProgressTracker() // Ensure only one tracker is running
    if (_mediaPlayer.isPlaying) {
      progressTrackerJob = viewModelScope.launch {
        while (isActive && _mediaPlayer.isPlaying) {
          val currentTime = _mediaPlayer.time
          val totalTime = _mediaPlayer.length
          if (_uiState.value.currentDuration != currentTime || _uiState.value.totalDuration != totalTime) {
            _uiState.update {
              it.copy(
                currentDuration = currentTime,
                totalDuration = if (totalTime > 0) totalTime else it.totalDuration // Keep old total if new one is 0
              )
            }
          }
          delay(500L) // Update progress every 500ms
        }
      }
    }
  }

  fun resetPlayerState() {
    Log.d(TAG, "Resetting player state")
    _mediaPlayer.stop()
    stopProgressTracker()
    _uiState.value = VideoPlayerUIState() // Reset to initial state
  }

  fun clearError() {
    _uiState.update { it.copy(error = null) }
  }

  private fun stopProgressTracker() {
    progressTrackerJob?.cancel()
    progressTrackerJob = null
  }


//    fun setIsBuffering(isBuffering: Boolean?) {
//        _isBuffering.value = isBuffering
//    }
//
//    fun setBuffering(buffering: Float) {
//        _buffering.value = buffering
//    }
//
//    fun updateCurrentDuration(duration: Long) {
//        _currentDuration.value = duration
//    }
//
//    fun updateTotalDuration(duration: Long) {
//        _totalDuration.value = duration
//    }
//
//    fun updateAudioTracks(tracks: List<MediaPlayer.TrackDescription>) {
//        _audioTracks.value = tracks
//    }

  fun selectAudioTrack(trackId: Int) {
    Log.d(TAG, "Selecting audio track: $trackId")
    if (_mediaPlayer.setAudioTrack(trackId)) {
      _uiState.update { it.copy(selectedAudioTrackId = trackId) }
    } else {
      Log.e(TAG, "Failed to set audio track $trackId")
    }
  }

//    fun updateSubtitleTracks(tracks: List<MediaPlayer.TrackDescription>) {
//        _subtitleTracks.value = tracks
//    }

  fun selectSubtitleTrack(trackId: Int) {
    Log.d(TAG, "Selecting subtitle track: $trackId")
    if (_mediaPlayer.setSpuTrack(trackId)) { // SPU is for subtitles
      _uiState.update { it.copy(selectedSubtitleTrackId = trackId) }
    } else {
      Log.e(TAG, "Failed to set subtitle track $trackId")
    }
  }


//    fun setVideoUrl(url: String?, title: String?) {
//        url?.let {
//            val media = Media(_libVLC, it.toUri()).apply { /* ... */ }
//            _mediaPlayer.media = media
//            media.release()
//            _mediaPlayer.play() // Or handle play/pause based on previous state
//            _isLoading.value = false
//            _videoUrl.value = url
//            _title.value = title
//        }
//    }

//    fun setLoading(loading: Boolean) {
//        _isLoading.value = loading
//    }

  fun toggleFullScreen() {
    _uiState.update { it.copy(isFullScreen = !it.isFullScreen) }
  }
//    fun setTitle(title: String?){
//        _title.value = title
//
//    }

  override fun onCleared() {
    super.onCleared()
    Log.d(TAG, "onCleared - Releasing MediaPlayer and LibVLC")
    stopProgressTracker()
    vlcVideoLayoutRef?.clear() // Clear the reference
    // Release MediaPlayer first
    _mediaPlayer.stop()
    _mediaPlayer.setEventListener(null) // Remove listener
    _mediaPlayer.detachViews() // Ensure views are detached
    _mediaPlayer.release()
    // Then release LibVLC
    _libVLC.release()
    Log.d(TAG, "MediaPlayer and LibVLC released.")
//        _isPlaying.value = false
//        _currentDuration.value = 0L
//        _totalDuration.value = 0L
//        _audioTracks.value = emptyList()
//        _selectedAudioTrack.value = 1
//        _subtitleTracks.value = emptyList()
//        _selectedSubtitleTrack.value = 1
//        _videoUrl.value = null
//        _isLoading.value = true
//        _isFullScreen.value = false
  }
}