package com.hitsuthar.june.utils

import android.content.Context
import androidx.core.net.toUri
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.MediaPlayer.Event
import org.videolan.libvlc.util.VLCVideoLayout


class VlcPlayerHolder(val context: Context) {

  private var libVLC: LibVLC? = null
  private var mediaPlayer: MediaPlayer? = null
  private var currentUrl: String? = null
  private var isPlaying: Boolean = false
  private var lastKnownPosition: Long = 0L

  // Callbacks
  private var bufferingCallback: ((Boolean) -> Unit)? = null
  private var errorCallback: ((String) -> Unit)? = null
  private var positionCallback: ((Long) -> Unit)? = null
  private var completionCallback: (() -> Unit)? = null


  // Player options
  private val vlcOptions = ArrayList<String>().apply {
    add("--network-caching=1500") // Medium buffer for streaming
    add("--clock-jitter=0")
    add("--clock-synchro=0")
    add("--drop-late-frames")
    add("--skip-frames")
    add("--avcodec-fast")
  }

  init {
    initialize()
  }

  /**
   * Initialize or reinitialize the VLC player
   */
  private fun initialize() {
    release()

    try {
      libVLC = LibVLC(context, vlcOptions)
      mediaPlayer = MediaPlayer(libVLC).apply {
        setEventListener { event ->
          handlePlayerEvent(event)
        }
      }

      // Restore playback state if available
      if (isPlaying) {
        play()
      }
    } catch (e: Exception) {
      errorCallback?.invoke("Failed to initialize VLC: ${e.message}")
    }
  }

  /**
   * Handle VLC player events
   */
  private fun handlePlayerEvent(event: Event) {
    when (event.type) {
      Event.Opening -> bufferingCallback?.invoke(true)
      Event.Buffering -> bufferingCallback?.invoke(event.buffering > 0f)
      Event.Playing -> {
        bufferingCallback?.invoke(false)
        isPlaying = true
      }
      Event.Paused -> isPlaying = false
      Event.Stopped -> isPlaying = false
      Event.EndReached -> completionCallback?.invoke()
      Event.EncounteredError -> errorCallback?.invoke("Player error occurred")
      Event.TimeChanged -> positionCallback?.invoke(event.timeChanged)
      Event.PositionChanged -> lastKnownPosition = (event.positionChanged * 1000).toLong()
    }
  }


  /**
   * Attach to a VLCVideoLayout (recommended for new implementations)
   */
  fun attachToVlcLayout(vlcLayout: VLCVideoLayout) {
    mediaPlayer?.attachViews(vlcLayout, null, false, false)
  }

  /**
   * Detach all views
   */
  fun detachViews() {
    mediaPlayer?.detachViews()
  }


  /**
   * Load and play a new URL
   */
  fun playNewUrl(url: String) {
    if (url == currentUrl) {
      // Already playing this URL, just ensure it's playing
      play()
      return
    }

    currentUrl = url
    mediaPlayer?.let { player ->
      try {
        player.stop()
        val media = Media(libVLC, url.toUri()).apply {
          setHWDecoderEnabled(true, false)
          addOption(":network-caching=1500")
          addOption(":file-caching=1500")
        }
        player.media = media
        player.play()
      } catch (e: Exception) {
        errorCallback?.invoke("Failed to play URL: ${e.message}")
        initialize() // Try reinitializing on error
        playNewUrl(url) // Retry
      }
    } ?: run {
      initialize()
      playNewUrl(url)
    }
  }

  /**
   * Control methods
   */
  fun play() {
    mediaPlayer?.play()
    isPlaying = true
  }

  fun pause() {
    mediaPlayer?.pause()
    isPlaying = false
  }

  fun stop() {
    mediaPlayer?.stop()
    isPlaying = false
  }

  fun seekTo(position: Long) {
    mediaPlayer?.time = position
    lastKnownPosition = position
  }


  fun getCurrentPosition(): Long = mediaPlayer?.time ?: lastKnownPosition

  fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false

  fun getDuration(): Long = mediaPlayer?.length ?: 0L

  // Add to VlcPlayerHolder class
  fun setAudioTrack(trackId: Int) {
    mediaPlayer?.setAudioTrack(trackId)
  }

  fun setSubtitleTrack(trackId: Int) {
    mediaPlayer?.setSpuTrack(trackId)
  }

  fun getAudioTracks(): List<MediaPlayer.TrackDescription> {
    return mediaPlayer?.audioTracks?.toList() ?: emptyList()
  }

  fun getSubtitleTracks(): List<MediaPlayer.TrackDescription> {
    return mediaPlayer?.spuTracks?.toList() ?: emptyList()
  }


  /**
   * Release resources
   */
  fun release() {
    try {
      detachViews()
      mediaPlayer?.let {
        it.stop()
        it.release()
      }
      libVLC?.release()
      mediaPlayer = null
      libVLC = null
    } catch (e: Exception) {
      errorCallback?.invoke("Error releasing player: ${e.message}")
    }
  }

  /**
   * Callback setters
   */
  fun setBufferingCallback(callback: (Boolean) -> Unit) {
    bufferingCallback = callback
  }

  fun setErrorCallback(callback: (String) -> Unit) {
    errorCallback = callback
  }

  fun setPositionCallback(callback: (Long) -> Unit) {
    positionCallback = callback
  }

  fun setCompletionCallback(callback: () -> Unit) {
    completionCallback = callback
  }

  // Other control methods (play, pause, seek, etc.)
}