package com.hitsuthar.june.screens

import MovieSyncViewModel
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.hitsuthar.june.R
import com.hitsuthar.june.SharedPreferencesManager
import com.hitsuthar.june.TORRSERVER_BASE_URL
import com.hitsuthar.june.utils.getFormatedTime
import com.hitsuthar.june.viewModels.ContentDetail
import com.hitsuthar.june.viewModels.ContentDetailViewModel
import com.hitsuthar.june.viewModels.SelectedVideoViewModel
import com.hitsuthar.june.viewModels.Stream
import com.hitsuthar.june.viewModels.VideoPlayerViewModel
import com.hitsuthar.june.viewModels.WatchPartyViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import kotlin.math.abs

@SuppressLint("SourceLockedOrientationActivity", "StateFlowValueCalledInComposition")
@Composable
fun VideoPlayerScreen(
  modifier: Modifier = Modifier,
  navController: NavController,
  window: WindowInsetsControllerCompat,
  selectedVideo: SelectedVideoViewModel,
  watchPartyViewModel: WatchPartyViewModel,
  context: Context,
  innersPadding: PaddingValues,
  videoPlayerViewModel: VideoPlayerViewModel,
  contentDetailViewModel: ContentDetailViewModel,
  movieSyncViewModel: MovieSyncViewModel,

  ) {
  val vlcOptions = ArrayList<String>().apply {
    add("--network-caching=1500") // Medium buffer for streaming
    add("--clock-jitter=0")
    add("--clock-synchro=0")
    add("--drop-late-frames")
    add("--skip-frames")
    add("--avcodec-fast")
  }
  val activity = LocalActivity.current
  val libVLC = remember { LibVLC(context, vlcOptions) }
  val mediaPlayer = remember { MediaPlayer(libVLC) }
  val video = selectedVideo.selectedVideo.collectAsState()
  val contentDetail by contentDetailViewModel.contentDetail.collectAsState()
  val currentRoom by movieSyncViewModel.currentRoom.collectAsState()
  val syncState by movieSyncViewModel.syncState.collectAsState()
  var isLocallyBuffering by remember { mutableStateOf(false) }

  val totalDuration = videoPlayerViewModel.totalDuration.collectAsState().value
  val isPlaying = videoPlayerViewModel.isPlaying.collectAsState().value
  val currentDuration = videoPlayerViewModel.currentDuration.collectAsState().value
  val audioTracks = videoPlayerViewModel.audioTracks.collectAsState().value
  val selectedAudioTrack = videoPlayerViewModel.selectedAudioTrack.collectAsState().value
  val subtitleTracks = videoPlayerViewModel.subtitleTracks.collectAsState().value
  val selectedSubtitleTrack = videoPlayerViewModel.selectedSubtitleTrack.collectAsState().value
  val videoUrl = videoPlayerViewModel.videoUrl.collectAsState().value
  val isLoading = videoPlayerViewModel.isLoading.collectAsState().value
  val isFullScreen = videoPlayerViewModel.isFullScreen.collectAsState().value

  val userID = SharedPreferencesManager(context.applicationContext).getData("USER_ID", "")


  fun updatePlayer(state: MovieSyncViewModel.SyncState) {
    // Avoid feedback loops by checking if update came from us
    if (abs(videoPlayerViewModel.currentDuration.value - state.currentTime) > 1000) {
      mediaPlayer.setTime(state.currentTime)
      videoPlayerViewModel.updateCurrentDuration(state.currentTime)
    }

    if (state.playbackState == "playing" && !videoPlayerViewModel.isPlaying.value) {
      videoPlayerViewModel.play()
      mediaPlayer.play()
    } else if (state.playbackState == "paused" && videoPlayerViewModel.isPlaying.value) {
      videoPlayerViewModel.pause()
      mediaPlayer.pause()
    }
  }





  LaunchedEffect((currentDuration / 1000) % 60) {
    if (abs(videoPlayerViewModel.currentDuration.value - movieSyncViewModel.syncState.value?.currentTime!!) > 1000) {
      movieSyncViewModel.updatePlaybackState(videoPlayerViewModel.currentDuration.value, isPlaying)
    }
  }

  LaunchedEffect(isFullScreen) {
    if (isFullScreen) {
      window.hide(WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.systemBars())
      activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    } else {
      mediaPlayer.aspectRatio = "16:9"
      window.show(WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.systemBars())
      activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
  }

  if (currentRoom != null) {
    LaunchedEffect(Unit, movieSyncViewModel.currentMovie.collectAsState().value) {
//      Log.d("VideoplayerScreen", "launchEffect: ${movieSyncViewModel.currentMovie.value}")
      if (movieSyncViewModel.currentMovie.value?.url != null) {
        videoPlayerViewModel.setVideoUrl(movieSyncViewModel.currentMovie.value?.url)
        mediaPlayer.stop()
        val media = Media(libVLC, videoPlayerViewModel.videoUrl.value?.toUri()).apply {
          setHWDecoderEnabled(true, false)
          addOption(":network-caching=1500")
          addOption(":file-caching=1500")
        }
        mediaPlayer.media = media
        media.release()
      }
      videoPlayerViewModel.setLoading(false)
    }
    LaunchedEffect(Unit, movieSyncViewModel.syncState.collectAsState().value) {
//      Log.d("VideoPlayerScreen", "LaunchedEffect: ${movieSyncViewModel.syncState.value}")
      updatePlayer(movieSyncViewModel.syncState.value!!)
    }
  } else {
    LaunchedEffect(video.value) {
      videoPlayerViewModel.setVideoUrl(
        when (video.value) {
          is Stream.DDL -> (video.value as Stream.DDL).ddlStream.url.replace(
            oldValue = " ", newValue = "%20"
          )

          is Stream.Torrent -> TORRSERVER_BASE_URL.toUri().buildUpon().appendPath("stream")
            .appendQueryParameter(
              "link",
              (video.value as Stream.Torrent).torrentStream.magnet
                ?: (video.value as Stream.Torrent).torrentStream.infoHash
            ).appendQueryParameter(
              "index", ((video.value as Stream.Torrent).torrentStream.fileIndex).toString()
            ).appendQueryParameter("play", null).build().toString()

          else -> null
        }
      )
      videoPlayerViewModel.setLoading(false)
    }
  }

  val lifecycleOwner = LocalLifecycleOwner.current
  var vlcVideoLayout: VLCVideoLayout? = null
  var wasPlaying = false

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_PAUSE -> {
          wasPlaying = mediaPlayer.isPlaying
          mediaPlayer.pause()
        }

        Lifecycle.Event.ON_RESUME -> {
          vlcVideoLayout?.let { layout ->
            mediaPlayer.attachViews(layout, null, false, false)
            if (wasPlaying) {
              mediaPlayer.play()
            }
          }
        }

        else -> {}
      }
    }

    lifecycleOwner.lifecycle.addObserver(observer)

    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
//      vlcPlayerHolder.release()
    }
  }

  DisposableEffect(Unit) {
    mediaPlayer.setEventListener {
      when (it.type) {
        MediaPlayer.Event.ESAdded -> {
          // Fetch tracks after media is parsed
        }

        MediaPlayer.Event.Paused -> {
          videoPlayerViewModel.pause()
          movieSyncViewModel.updatePlaybackState(
            videoPlayerViewModel.currentDuration.value,
            videoPlayerViewModel.isPlaying.value
          )
        }

        MediaPlayer.Event.Playing -> {
          videoPlayerViewModel.play()
          movieSyncViewModel.updatePlaybackState(
            videoPlayerViewModel.currentDuration.value,
            videoPlayerViewModel.isPlaying.value
          )
        }

        MediaPlayer.Event.TimeChanged -> {
          videoPlayerViewModel.updateCurrentDuration(mediaPlayer.time)
          videoPlayerViewModel.updateTotalDuration(mediaPlayer.length)
          videoPlayerViewModel.updateAudioTracks(
            mediaPlayer.audioTracks.orEmpty().toList()
          )
          videoPlayerViewModel.updateSubtitleTracks(
            mediaPlayer.spuTracks.orEmpty().toList()
          )
        }

        MediaPlayer.Event.Buffering -> {
          Log.d("VideoPlayerScreen", "Buffering: ${it.buffering}")
          videoPlayerViewModel.setIsBuffering(true)
//          movieSyncViewModel.setBufferingState(true, userID)

        }
      }
    }

    onDispose {
      mediaPlayer.stop()
      mediaPlayer.detachViews()
      mediaPlayer.release()
      libVLC.release()
      window.show(WindowInsetsCompat.Type.navigationBars())
      videoPlayerViewModel.reset()
      activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
  }

  if (isLoading) {
    // Show a loading indicator
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator()
    }
  } else if (videoUrl != null) {
    VideoPlayerComposable(
      videoUrl = videoUrl,
      mediaPlayer = mediaPlayer,
      modifier = modifier,
      onPlayPauseToggle = {
        if (mediaPlayer.isPlaying) mediaPlayer.pause() else mediaPlayer.play()
      },
      onStop = {
        mediaPlayer.stop()
        videoPlayerViewModel.pause()
      },
      isPlaying = isPlaying,
      currentDuration = currentDuration,
      totalDuration = totalDuration,
      onSeek = { position ->
        mediaPlayer.time = position
        videoPlayerViewModel.updateCurrentDuration(position)
      },
      audioTracks = audioTracks,
      selectedAudioTrack = selectedAudioTrack,
      onAudioTrackChange = { trackId ->
        mediaPlayer.setAudioTrack(trackId)
        videoPlayerViewModel.selectAudioTrack(trackId)
      },
      subtitleTracks = subtitleTracks,
      selectedSubtitleTrack = selectedSubtitleTrack,
      onSubtitleTrackChange = { trackId ->
        mediaPlayer.setSpuTrack(trackId)
        videoPlayerViewModel.selectSubtitleTrack(trackId)
      },
      navController = navController,
      window = window,
      isFullScreen = isFullScreen,
      toggleFullScreen = { videoPlayerViewModel.toggleFullScreen() },
      innersPadding = innersPadding,
      contentDetail = contentDetail,
      vlcVideoLayout = { vlcVideoLayout = it },

    )
  }
}

@Composable
fun BufferingIndicator(isLocal: Boolean) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.7f)),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      CircularProgressIndicator()
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = if (isLocal) "Buffering..." else "Waiting for others...",
        color = Color.White
      )
    }
  }
}

@Composable
fun VideoPlayerComposable(
  videoUrl: String?,
  mediaPlayer: MediaPlayer,
  isPlaying: Boolean,
  onPlayPauseToggle: () -> Unit,
  onStop: () -> Unit,
  modifier: Modifier = Modifier,
  currentDuration: Long,
  totalDuration: Long,
  onSeek: (Long) -> Unit,
  audioTracks: List<MediaPlayer.TrackDescription>,
  selectedAudioTrack: Int,
  onAudioTrackChange: (Int) -> Unit,
  subtitleTracks: List<MediaPlayer.TrackDescription>,
  selectedSubtitleTrack: Int,
  onSubtitleTrackChange: (Int) -> Unit,
  navController: NavController,
  window: WindowInsetsControllerCompat,
  isFullScreen: Boolean,
  toggleFullScreen: () -> Unit,
  innersPadding: PaddingValues,
  contentDetail: ContentDetail,
  vlcVideoLayout: (VLCVideoLayout) -> Unit,
) {
  var controlsVisible by remember { mutableStateOf(true) }
  val coroutineScope = rememberCoroutineScope()
  var hideControlsJob by remember { mutableStateOf<Job?>(null) }

  // Function to show controls and reset auto-hide timer
  fun showControls() {
    controlsVisible = true
    hideControlsJob?.cancel() // Cancel any previous hide timer
    if (isPlaying) {
      hideControlsJob = coroutineScope.launch {
        delay(5000) // Delay before auto-hiding
        controlsVisible = false
      }
    }
  }

  LaunchedEffect(controlsVisible, isFullScreen) {
    if (!controlsVisible && isFullScreen) {
      window.hide(WindowInsetsCompat.Type.systemBars())
    } else window.show(WindowInsetsCompat.Type.systemBars())
  }
  // Show controls initially
  LaunchedEffect(isPlaying, isFullScreen) { showControls() }
  Column(
    Modifier
      .background(color = MaterialTheme.colorScheme.background)
//            .fillMaxSize()
      .padding(top = if (!isFullScreen) innersPadding.calculateTopPadding() else 0.dp)
  ) {

    Box(
      modifier = modifier
        .pointerInput(Unit) {
          detectTapGestures(onTap = {
            if (!controlsVisible) showControls() else controlsVisible = false
          })
        }
        .background(color = MaterialTheme.colorScheme.background)
        .then(
          if (isFullScreen) Modifier.fillMaxSize() else Modifier.aspectRatio(16 / 9f)
        )) {
      AndroidView(
        factory = { context ->
          VLCVideoLayout(context).apply {
            vlcVideoLayout(this)
            mediaPlayer.attachViews(this, null, false, false)
            keepScreenOn = true
          }
        }, modifier = modifier.background(color = MaterialTheme.colorScheme.background)
      )

      ControlsOverlay(
        visible = controlsVisible,
        isPlaying = isPlaying,
        currentDuration = currentDuration,
        totalDuration = totalDuration,
        audioTracks = audioTracks,
        selectedAudioTrack = selectedAudioTrack,
        subtitleTracks = subtitleTracks,
        selectedSubtitleTrack = selectedSubtitleTrack,
        onPlayPauseToggle = { onPlayPauseToggle(); showControls() },
        onStop = onStop,
        onSeek = { onSeek(it); showControls() },
        onAudioTrackChange = onAudioTrackChange,
        onSubtitleTrackChange = onSubtitleTrackChange,
        showControls = { showControls() },
        navController = navController,
        isFullScreen = isFullScreen,
        toggleFullScreen = { toggleFullScreen() },
        contentDetail = contentDetail,
        innersPadding = innersPadding
      )
//      isBuffering?.let {
//        BufferingIndicator(isLocal = it)
//
//      }
    }
  }
}

@SuppressLint("UnrememberedMutableInteractionSource")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlsOverlay(
  visible: Boolean,
  isPlaying: Boolean,
  currentDuration: Long,
  totalDuration: Long,
  audioTracks: List<MediaPlayer.TrackDescription>,
  selectedAudioTrack: Int,
  subtitleTracks: List<MediaPlayer.TrackDescription>,
  selectedSubtitleTrack: Int,
  onPlayPauseToggle: () -> Unit,
  onStop: () -> Unit,
  onSeek: (Long) -> Unit,
  onAudioTrackChange: (Int) -> Unit,
  onSubtitleTrackChange: (Int) -> Unit,
  showControls: () -> Unit,
  navController: NavController,
  isFullScreen: Boolean,
  toggleFullScreen: () -> Unit,
  contentDetail: ContentDetail,
  innersPadding: PaddingValues
) {
  val sheetState = rememberModalBottomSheetState()
  var showAudioBottomSheet by remember { mutableStateOf(false) }
  var showSubBottomSheet by remember { mutableStateOf(false) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(
        top = if (isFullScreen) innersPadding.calculateTopPadding() else 8.dp,
        start = if (isFullScreen) 32.dp else 16.dp,
        end = if (isFullScreen) 32.dp else 16.dp,
        bottom = if (isFullScreen) innersPadding.calculateBottomPadding() else 8.dp
      )
  ) {
//        if (!videoUrl.isNullOrEmpty()) {
//            Text(
//                text = videoUrl, style = TextStyle(color = Color.White)
//            )
//        }
    AnimatedVisibility(
      visible = visible,
      enter = slideInVertically(animationSpec = tween(durationMillis = 500)) + fadeIn(
        animationSpec = tween(durationMillis = 500)
      ).plus(if (!isFullScreen) expandIn(initialSize = { it }) else EnterTransition.None),
      exit = slideOutVertically(animationSpec = tween(durationMillis = 500)) + fadeOut(
        animationSpec = tween(durationMillis = 500)
      ).plus(if (!isFullScreen) shrinkOut(targetSize = { it }) else ExitTransition.None)
    ) {
      Row(
        Modifier.fillMaxWidth(), Arrangement.SpaceBetween
      ) {
        IconButton(
          onClick = { navController.popBackStack() }, colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
              alpha = 0.5f
            )
          ), modifier = Modifier
            .height(48.dp)
            .width(48.dp)
            .border(
              width = 0.2.dp,
              color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
              CircleShape
            )
//                modifier = Modifier.padding(16.dp)
        ) {
          Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          IconButton(
            onClick = { showSubBottomSheet = true; showControls() },
            colors = IconButtonDefaults.iconButtonColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                alpha = 0.5f
              )
            ),
            modifier = Modifier
              .height(48.dp)
              .width(48.dp)
              .border(
                width = 0.2.dp,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                CircleShape
              )
          ) {
            Icon(
              painter = painterResource(id = R.drawable.baseline_subtitles_24),
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
          IconButton(
            onClick = { showAudioBottomSheet = true; showControls() },
            colors = IconButtonDefaults.iconButtonColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                alpha = 0.5f
              )
            ),
            modifier = Modifier
              .height(48.dp)
              .width(48.dp)
              .border(
                width = 0.2.dp,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                CircleShape
              )
          ) {
            Icon(
              painter = painterResource(id = R.drawable.baseline_audiotrack_24),
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
          IconButton(
            onClick = toggleFullScreen, colors = IconButtonDefaults.iconButtonColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            ), modifier = Modifier
              .height(48.dp)
              .width(48.dp)
              .border(
                width = 0.2.dp,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                CircleShape
              )
          ) {
            Icon(
              painter = painterResource(id = if (isFullScreen) R.drawable.baseline_fullscreen_exit_24 else R.drawable.baseline_fullscreen_24),
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
        }
      }
    }
//            SeekButton("-10s") { onSeek((currentDuration - 10000).coerceAtLeast(0)) }
    AnimatedVisibility(
      visible,
      modifier = Modifier.align(Alignment.Center),
      enter = fadeIn(animationSpec = tween(durationMillis = 500)),
      exit = fadeOut(animationSpec = tween(durationMillis = 500))
    ) {
      IconButton(
        onClick = onPlayPauseToggle, colors = IconButtonDefaults.iconButtonColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ), modifier = Modifier
          .height(64.dp)
          .width(64.dp)
          .border(
            width = 0.2.dp,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
            CircleShape
          )
      ) {
        Icon(
          painter = painterResource(
            id = if (isPlaying) R.drawable.baseline_pause_24 else R.drawable.baseline_play_arrow_24
          ), contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
      }
    }

//            SeekButton("+10s") { onSeek((currentDuration + 10000).coerceAtMost(totalDuration)) }
    AnimatedVisibility(
      visible, modifier = Modifier.align(Alignment.BottomCenter), enter = slideInVertically(
        initialOffsetY = { it }, animationSpec = tween(durationMillis = 500)
      ) + fadeIn(animationSpec = tween(durationMillis = 500)).plus(
        if (!isFullScreen) expandIn(
          initialSize = { it }) else EnterTransition.None
      ), exit = slideOutVertically(
        targetOffsetY = { it }, animationSpec = tween(durationMillis = 500)
      ) + fadeOut(animationSpec = tween(durationMillis = 500)).plus(
        if (!isFullScreen) shrinkOut(
          targetSize = { it }) else ExitTransition.None
      )
    ) {
      Column(Modifier.fillMaxWidth()) {
        Row(
          horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = when (contentDetail) {
              is ContentDetail.Movie -> contentDetail.tmdbMovieDetail.title
              is ContentDetail.Show -> contentDetail.tmdbShowDetail.name
              else -> ""
            },
            style = if (isFullScreen) MaterialTheme.typography.headlineMedium.copy(
              fontWeight = FontWeight.ExtraLight
            ) else MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraLight),
          )
          Text(
            text = "${getFormatedTime(currentDuration)} / ${
              getFormatedTime(
                totalDuration
              )
            }",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraLight),
            modifier = Modifier
              .align(Alignment.Bottom)
              .padding(end = 4.dp)
          )
        }
        Box(Modifier.height(16.dp)) {
          Slider(
            value = currentDuration.toFloat(),
//            valueRange = 0f..totalDuration.toFloat(),
            valueRange = 0f..totalDuration.toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            track = { sliderState ->
              SliderDefaults.Track(
                sliderState = sliderState,
                modifier = Modifier.height(12.dp),
                thumbTrackGapSize = 0.dp,
                colors = SliderDefaults.colors(
                  activeTrackColor = MaterialTheme.colorScheme.onSecondaryContainer,
                  inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                    alpha = 0.5f
                  )
                )
              )
            },
            thumb = { sliderState ->
              SliderDefaults.Thumb(
                interactionSource = MutableInteractionSource(),
                modifier = Modifier
                  .height(12.dp)
                  .width(12.dp)
                  .align(Alignment.Center),
                colors = SliderDefaults.colors(
                  thumbColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
              )
            },
            modifier = Modifier
          )
        }
      }
    }


    if (showAudioBottomSheet) {
      ModalBottomSheet(
        onDismissRequest = { showAudioBottomSheet = false }, sheetState = sheetState
      ) {
        DropdownMenuSelector(
          label = "Audio Tracks",
          options = audioTracks,
          selectedOption = selectedAudioTrack,
          onOptionSelected = onAudioTrackChange
        )
      }
    }
    if (showSubBottomSheet) {
      ModalBottomSheet(
        onDismissRequest = { showSubBottomSheet = false }, sheetState = sheetState
      ) {
        DropdownMenuSelector(
          label = "Subtitle Tracks",
          options = subtitleTracks,
          selectedOption = selectedSubtitleTrack,
          onOptionSelected = onSubtitleTrackChange
        )
      }
    }
  }
}

@Composable
fun DropdownMenuSelector(
  label: String,
  options: List<MediaPlayer.TrackDescription>,
  selectedOption: Int,
  onOptionSelected: (Int) -> Unit
) {
  Column {
    Text(
      label, style = MaterialTheme.typography.titleLarge.copy(
        fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onPrimaryContainer
      ), modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
    )

    Box(
      Modifier
        .padding(bottom = 12.dp)
        .height(1.dp)
        .fillMaxWidth()
        .background(color = MaterialTheme.colorScheme.onPrimaryContainer),
    )
    options.forEach { track ->
      Button(
        onClick = { onOptionSelected(track.id) },
        Modifier.padding(bottom = 8.dp, start = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
      ) {
        Text(
          text = track.name,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
          fontWeight = if (track.id == selectedOption) FontWeight.ExtraBold else FontWeight.Normal
        )
      }
    }
    Spacer(Modifier.height(16.dp))
  }
}
