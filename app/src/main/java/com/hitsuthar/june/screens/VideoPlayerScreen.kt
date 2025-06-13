package com.hitsuthar.june.screens

import MovieSyncViewModel
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
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
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.hitsuthar.june.R
import com.hitsuthar.june.SharedPreferencesManager
import com.hitsuthar.june.utils.getFormatedTime
import com.hitsuthar.june.viewModels.VideoPlayerUIState
import com.hitsuthar.june.viewModels.VideoPlayerViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import kotlin.math.abs


data class VideoPlayerActions(
  val onPlayPauseToggle: () -> Unit,
  val onStop: () -> Unit,
  val onSeek: (Long) -> Unit,
  val onAudioTrackChange: (Int) -> Unit,
  val onSubtitleTrackChange: (Int) -> Unit,
  val onToggleFullScreen: () -> Unit,
  val onVlcVideoLayoutReady: (VLCVideoLayout) -> Unit // Renamed from vlcVideoLayout
)


@SuppressLint("SourceLockedOrientationActivity", "StateFlowValueCalledInComposition")
@Composable
fun VideoPlayerScreen(
  modifier: Modifier = Modifier,
  navController: NavController,
  window: WindowInsetsControllerCompat,
  context: Context,
  innersPadding: PaddingValues,
  videoPlayerViewModel: VideoPlayerViewModel,

  ) {

  val uiState by videoPlayerViewModel.uiState.collectAsState()
  val context = LocalContext.current
  val activity = LocalActivity.current


  val userID = SharedPreferencesManager(context.applicationContext).getData("USER_ID", "")


  LaunchedEffect(uiState.isFullScreen) {
    if (uiState.isFullScreen) {
//      window.hide(WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.systemBars())
      activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    } else {
//      mediaPlayer.aspectRatio = "16:9"
//      window.show(WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.systemBars())
      activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
  }

//  if (currentRoom != null) {
//    if (!movieSyncViewModel.currentMovie.collectAsState().value?.url.isNullOrBlank()) {
//      LaunchedEffect((uiState.currentDuration / 1000) % 60) {
//        if (abs(uiState.currentDuration - movieSyncViewModel.syncState.value?.currentTime!!) > 1000) {
//          movieSyncViewModel.updatePlaybackState(
//            uiState.currentDuration, uiState.isPlaying
//          )
//        }
//      }
//      LaunchedEffect(Unit, movieSyncViewModel.currentMovie.collectAsState().value) {
//        videoPlayerViewModel.loadVideo(
//          movieSyncViewModel.currentMovie.value?.url, movieSyncViewModel.currentMovie.value?.title
//        )
//      }
//      LaunchedEffect(Unit, movieSyncViewModel.syncState.collectAsState().value) {
//        updatePlayer(movieSyncViewModel.syncState.value!!)
//      }
//    }
//  }
//  else {
//    LaunchedEffect(Unit, video.value) {
//      videoPlayerViewModel.loadVideo(
//        when (video.value) {
//          is Stream.DDL -> (video.value as Stream.DDL).ddlStream.url.replace(
//            oldValue = " ", newValue = "%20"
//          )
//
//          is Stream.Torrent -> TORRSERVER_BASE_URL.toUri().buildUpon().appendPath("stream")
//            .appendQueryParameter(
//              "link",
//              (video.value as Stream.Torrent).torrentStream.magnet
//                ?: (video.value as Stream.Torrent).torrentStream.infoHash
//            ).appendQueryParameter(
//              "index", ((video.value as Stream.Torrent).torrentStream.fileIndex).toString()
//            ).appendQueryParameter("play", null).build().toString()
//
//          else -> null
//        }, when (video.value) {
//          is Stream.DDL -> (video.value as Stream.DDL).ddlStream.name
//          is Stream.Torrent -> (video.value as Stream.Torrent).torrentStream.title
//          else -> null
//        }
//      )
//    }
//  }


  DisposableEffect(Unit) {
//    mediaPlayer.setEventListener {
//      when (it.type) {
//        MediaPlayer.Event.ESAdded -> {
//          // Fetch tracks after media is parsed
//        }
//
//        MediaPlayer.Event.Paused -> {
//          videoPlayerViewModel.pause()
//          movieSyncViewModel.updatePlaybackState(
//            videoPlayerViewModel.currentDuration.value,
//            videoPlayerViewModel.isPlaying.value
//          )
//        }
//
//        MediaPlayer.Event.Playing -> {
//          videoPlayerViewModel.play()
//          movieSyncViewModel.updatePlaybackState(
//            videoPlayerViewModel.currentDuration.value,
//            videoPlayerViewModel.isPlaying.value
//          )
//        }
//
//        MediaPlayer.Event.TimeChanged -> {
//          videoPlayerViewModel.updateCurrentDuration(mediaPlayer.time)
//          videoPlayerViewModel.updateTotalDuration(mediaPlayer.length)
//          videoPlayerViewModel.updateAudioTracks(
//            mediaPlayer.audioTracks.orEmpty().toList()
//          )
//          videoPlayerViewModel.updateSubtitleTracks(
//            mediaPlayer.spuTracks.orEmpty().toList()
//          )
//        }
//
//        MediaPlayer.Event.Buffering -> {
//          Log.d("VideoPlayerScreen", "Buffering: ${it.buffering}")
//          videoPlayerViewModel.setIsBuffering(true)
////          movieSyncViewModel.setBufferingState(true, userID)
//        }
//      }
//    }

    onDispose {
      window.show(WindowInsetsCompat.Type.navigationBars())
      activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
  }

  if (uiState.videoUrl.isNullOrBlank()) {
    Box(
      Modifier
        .fillMaxWidth()
        .padding(top = innersPadding.calculateTopPadding())
        .aspectRatio(16 / 9f)
    ) {
      Text(
        "Nothing to play :)", modifier = Modifier.align(
          Alignment.Center
        )
      )
    }
  } else {
    VideoPlayerComposable(
      modifier = modifier,
      navController = navController,
      window = window,
      playerState = uiState,
      playerActions = VideoPlayerActions(
        onPlayPauseToggle = { videoPlayerViewModel.togglePlayPause() },
        onStop = { videoPlayerViewModel.pause() },
        onSeek = { position -> videoPlayerViewModel.seekTo(position) },
        onAudioTrackChange = { trackId -> videoPlayerViewModel.selectAudioTrack(trackId) },
        onSubtitleTrackChange = { trackId -> videoPlayerViewModel.selectSubtitleTrack(trackId) },
        onToggleFullScreen = { videoPlayerViewModel.toggleFullScreen() },
        onVlcVideoLayoutReady = { videoPlayerViewModel.attachView(it) }),
      innersPadding = innersPadding,
    )
  }
}


// Helper for lifecycle events in Compose
@Composable
fun LifecycleEffect(
  onResume: () -> Unit = {},
  onPause: () -> Unit = {},
  onStop: () -> Unit = {}, // Add onStart, onStop, onDestroy as needed
  key: Any = Unit // Re-runs effect if key changes
) {
  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner, key) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> onResume()
        Lifecycle.Event.ON_PAUSE -> onPause()
        Lifecycle.Event.ON_STOP -> onStop()
        else -> {}
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
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
        text = if (isLocal) "Buffering..." else "Waiting for others...", color = Color.White
      )
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VideoPlayerComposable(
  playerState: VideoPlayerUIState,
  playerActions: VideoPlayerActions,
//  mediaPlayer: MediaPlayer, // Still needed for AndroidView
  navController: NavController,
  window: WindowInsetsControllerCompat,
  innersPadding: PaddingValues, // Could potentially be part of a UI state class
  modifier: Modifier = Modifier,
) {
  var controlsVisible by remember { mutableStateOf(true) }
  val coroutineScope = rememberCoroutineScope()
  var hideControlsJob by remember { mutableStateOf<Job?>(null) }

  // Function to show controls and reset auto-hide timer
  fun showControlsAndAutoHide() {
    controlsVisible = true
    hideControlsJob?.cancel() // Cancel any previous hide timer
    if (playerState.isPlaying) {
      hideControlsJob = coroutineScope.launch {
        delay(5000) // Delay before auto-hiding
        controlsVisible = false
      }
    }
  }

  // Call this when play/pause changes, or when controls are manually shown
  LaunchedEffect(playerState.isPlaying, controlsVisible) {
    if (controlsVisible && playerState.isPlaying) {
      showControlsAndAutoHide() // Restart timer if playing and controls are shown
    } else if (!playerState.isPlaying) {
      hideControlsJob?.cancel() // Stop auto-hide if paused
    }
  }

  LaunchedEffect(controlsVisible, playerState.isFullScreen) {
    if (playerState.isFullScreen) {
      window.hide(WindowInsetsCompat.Type.navigationBars())
      if (controlsVisible) window.show(WindowInsetsCompat.Type.statusBars())
      else window.hide(WindowInsetsCompat.Type.statusBars())
    } else window.show(WindowInsetsCompat.Type.systemBars())
  }
  LaunchedEffect(Unit) { // Initial show
    showControlsAndAutoHide()
  }

  Box(modifier = modifier
    .pointerInput(Unit) {
      detectTapGestures(onTap = {
        if (controlsVisible) {
          controlsVisible = false
          hideControlsJob?.cancel() // Cancel auto-hide when manually hiding
        } else {
          showControlsAndAutoHide()
        }
      })
    }
    .padding(top = if (!playerState.isFullScreen) innersPadding.calculateTopPadding() else 0.dp)
    .background(color = MaterialTheme.colorScheme.background)
    .then(
      if (playerState.isFullScreen) Modifier.fillMaxSize() else Modifier.aspectRatio(16 / 9f)
    )) {

    AndroidView(factory = { context ->
      VLCVideoLayout(context).apply {
        playerActions.onVlcVideoLayoutReady(this)

        keepScreenOn = true
      }
    }, modifier = modifier.fillMaxSize(), onRelease = { vlcVideoLayout ->
      // Consider if mediaPlayer.stop() or other cleanup is needed here,
      // though usually this is handled when the player itself is released.
    })
//    if (100 > playerState.bufferingProgress && playerState.bufferingProgress >= 0) {
//      CircularWavyProgressIndicator(
//        progress = { playerState.bufferingProgress / 100f },
//        modifier = Modifier
//          .size(52.dp)
//          .align(Alignment.Center)
//      )
//      LoadingIndicator(
//        modifier = Modifier
//          .size(48.dp)
//          .align(Alignment.Center)
//      )
//    }

    ControlsOverlay(
      visible = controlsVisible,
      isPlaying = playerState.isPlaying,
      currentDuration = playerState.currentDuration ?: 0L,
      totalDuration = playerState.totalDuration ?: 0L,
      audioTracks = playerState.audioTracks,
      selectedAudioTrack = playerState.selectedAudioTrackId,
      subtitleTracks = playerState.subtitleTracks,
      selectedSubtitleTrack = playerState.selectedSubtitleTrackId,
      onPlayPauseToggle = playerActions.onPlayPauseToggle,
      onStop = playerActions.onStop,
      onSeek = { playerActions.onSeek(it); showControlsAndAutoHide() },
      onAudioTrackChange = playerActions.onAudioTrackChange,
      onSubtitleTrackChange = playerActions.onSubtitleTrackChange,
      showControls = { showControlsAndAutoHide() },
      navController = navController,
      isFullScreen = playerState.isFullScreen,
      toggleFullScreen = { playerActions.onToggleFullScreen() },
      title = playerState.title,
      innersPadding = innersPadding
    )
//      isBuffering?.let {
//        BufferingIndicator(isLocal = it)
//
//      }
  }

}

@SuppressLint("UnrememberedMutableInteractionSource")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
  title: String?,
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
        start = if (isFullScreen) 32.dp else 8.dp,
        end = if (isFullScreen) 32.dp else 8.dp,
        bottom =
//          if (isFullScreen) innersPadding.calculateBottomPadding()
//          else
          8.dp
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
          onClick = { navController.popBackStack() },
          colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
              alpha = 0.5f
            )
          ),
          modifier = Modifier
            .height(48.dp)
            .width(48.dp),
          shape = CircleShape,
          content = {
            Icon(
              Icons.AutoMirrored.Filled.KeyboardArrowLeft,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
//                modifier = Modifier.padding(16.dp)
        )
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
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .height(24.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = title ?: "",
            style = if (isFullScreen) MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.ExtraLight
            ) else MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraLight),
            modifier = Modifier.weight(1f, fill = false), // Takes only needed space
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "${getFormatedTime(currentDuration)} / ${getFormatedTime(totalDuration)}",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraLight),
            modifier = Modifier
              .align(Alignment.Bottom)
              .padding(horizontal = 4.dp)
//              .width( 100.dp)
              .wrapContentWidth(),

            )
        }
        Box(Modifier.height(16.dp)) {
          LinearWavyProgressIndicator(
            progress = { currentDuration.toFloat() / totalDuration.toFloat() },
            modifier = Modifier
              .fillMaxWidth()
              .align(Alignment.Center),
            amplitude = { if (isPlaying) 1f else 0f },
            stroke = Stroke(width = 3f),
            trackStroke = Stroke(width = 3f)
          )
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
//                  activeTrackColor = MaterialTheme.colorScheme.onSecondaryContainer,
//                  inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer.copy(
//                    alpha = 0.5f
//                  )
                  activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent
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
    options.forEach { track ->
      HorizontalDivider()
      ListItem(
        leadingContent = {
          RadioButton(
            selected = track.id == selectedOption, onClick = { onOptionSelected(track.id) })
        },
        colors = ListItemDefaults.colors().copy(containerColor = Color.Transparent),
        headlineContent = {
          Text(
            text = track.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = if (track.id == selectedOption) FontWeight.ExtraBold else FontWeight.Normal
          )
        },
        modifier = Modifier.clickable { onOptionSelected(track.id) })
      HorizontalDivider()

    }
    Spacer(Modifier.height(16.dp))
  }
}
