package com.hitsuthar.june

import MovieSyncViewModel
import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.hitsuthar.june.screens.DDLScreen
import com.hitsuthar.june.screens.DetailScreen
import com.hitsuthar.june.screens.HomeScreen
import com.hitsuthar.june.screens.PartyScreen
import com.hitsuthar.june.screens.SearchScreen
import com.hitsuthar.june.screens.SettingsScreen
import com.hitsuthar.june.screens.TorrentScreen
import com.hitsuthar.june.screens.VideoPlayerScreen
import com.hitsuthar.june.utils.SimpleIdGenerator
import com.hitsuthar.june.utils.TmdbRepository
import com.hitsuthar.june.viewModels.ContentDetailViewModel
import com.hitsuthar.june.viewModels.DDLViewModel
import com.hitsuthar.june.viewModels.SelectedVideoViewModel
import com.hitsuthar.june.viewModels.VideoPlayerViewModel
import com.hitsuthar.june.viewModels.WatchPartyViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("CoroutineCreationDuringComposition", "HardwareIds")
@Composable
fun SetupNavGraph(
  navController: NavHostController,
  repository: TmdbRepository,
  window: WindowInsetsControllerCompat,
  context: Context,
  innersPadding: PaddingValues,
  selectedVideoViewModel: SelectedVideoViewModel,
  contentDetailViewModel: ContentDetailViewModel,
  watchPartyViewModel: WatchPartyViewModel,
  videoPlayerViewModel: VideoPlayerViewModel,
  ddlViewModel: DDLViewModel,
  movieSyncViewModel: MovieSyncViewModel,
  modifier:Modifier = Modifier
) {
  val coroutineScope = CoroutineScope(Dispatchers.IO)
  val spm = SharedPreferencesManager(context.applicationContext)


  val isFirstRun: Boolean = spm.getData("FIRST_RUN_KEY", true)
  Log.d("isFirstRun", isFirstRun.toString())

  if (isFirstRun) {
    coroutineScope.launch {
      spm.saveData(
        "USER_ID", Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
      )
      spm.saveData("PARTY_ID", SimpleIdGenerator().generateRandomId())
      spm.saveData("FIRST_RUN_KEY", false)
      spm.saveData("USER_NAME", "June")
    }
  }
  val userID = SharedPreferencesManager(context.applicationContext).getData("USER_ID", "")
  movieSyncViewModel.checkAlreadyJoinedRoom(userID)


  NavHost(
    navController = navController,
    startDestination = Screen.Home.route,
//    modifier = modifier
  ) {
    composable(Screen.Home.route) {
      HomeScreen(
        navController, repository, contentDetailViewModel, innersPadding
      )
    }
    composable(route = Screen.Search.route) {
      SearchScreen(
        navController,
        repository,
        contentDetailViewModel = contentDetailViewModel,
      )
    }
    composable(Screen.Detail.route) {
      DetailScreen(
        navController = navController,
        repository = repository,
        contentDetailViewModel = contentDetailViewModel,
        selectedVideoViewModel = selectedVideoViewModel,
        ddlViewModel = ddlViewModel,
        innersPadding = innersPadding,
        movieSyncViewModel = movieSyncViewModel
      )
    }
    composable(Screen.DDL.route) {
      DDLScreen(
        navController = navController,
        selectedVideo = selectedVideoViewModel,
        innersPadding = innersPadding,
        ddlViewModel = ddlViewModel,
        movieSyncViewModel = movieSyncViewModel
      )
    }
    composable(Screen.Torrent.route) {
      TorrentScreen(
        contentDetailViewModel = contentDetailViewModel,
        navController = navController,
        selectedVideo = selectedVideoViewModel,
        innersPadding = innersPadding
      )

    }
    composable(Screen.VideoPlayer.route) {
      VideoPlayerScreen(
        navController = navController,
        window = window,
        selectedVideo = selectedVideoViewModel,
        context = context,
        watchPartyViewModel = watchPartyViewModel,
        videoPlayerViewModel = videoPlayerViewModel,
        innersPadding = innersPadding,
        contentDetailViewModel = contentDetailViewModel,
        movieSyncViewModel = movieSyncViewModel
      )
    }
    composable(Screen.Settings.route) {
      SettingsScreen(innersPadding = innersPadding, context = context)
    }
    composable(Screen.Party.route) {
      PartyScreen(
        innersPadding = innersPadding,
        context = context,
        movieSyncViewModel = movieSyncViewModel,
        navController = navController,
        window = window,
        selectedVideo = selectedVideoViewModel,
        watchPartyViewModel = watchPartyViewModel,
        videoPlayerViewModel = videoPlayerViewModel,
        contentDetailViewModel = contentDetailViewModel
      )
    }
  }
}
