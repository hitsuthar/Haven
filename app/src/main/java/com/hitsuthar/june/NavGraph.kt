package com.hitsuthar.june

import MovieSyncViewModel
import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.hitsuthar.june.utils.TmdbRepository
import com.hitsuthar.june.viewModels.ContentDetailViewModel
import com.hitsuthar.june.viewModels.DDLViewModel
import com.hitsuthar.june.viewModels.HomeScreenViewModel
import com.hitsuthar.june.viewModels.VideoPlayerViewModel

@SuppressLint("CoroutineCreationDuringComposition", "HardwareIds")
@Composable
fun SetupNavGraph(
  navController: NavHostController,
  repository: TmdbRepository,
  window: WindowInsetsControllerCompat,
  context: Context,
  innersPadding: PaddingValues,
  homeScreenViewModel: HomeScreenViewModel,
  contentDetailViewModel: ContentDetailViewModel,
  videoPlayerViewModel: VideoPlayerViewModel,
  ddlViewModel: DDLViewModel,
  movieSyncViewModel: MovieSyncViewModel,
  modifier:Modifier = Modifier
) {

  val currentRoom by movieSyncViewModel.currentRoom.collectAsState()


  NavHost(
    navController = navController,
    startDestination = if (currentRoom != null) Screen.Party.route else Screen.Home.route,
//    modifier = modifier
  ) {
    composable(Screen.Home.route) {
      HomeScreen(
        navController, repository, homeScreenViewModel = homeScreenViewModel , contentDetailViewModel, innersPadding
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
        ddlViewModel = ddlViewModel,
        innersPadding = innersPadding,
        movieSyncViewModel = movieSyncViewModel,
        videoPlayerViewModel
      )
    }
    composable(Screen.DDL.route) {
      DDLScreen(
        navController = navController,
        innersPadding = innersPadding,
        ddlViewModel = ddlViewModel,
        movieSyncViewModel = movieSyncViewModel,
        videoPlayerViewModel = videoPlayerViewModel
      )
    }
    composable(Screen.Torrent.route) {
      TorrentScreen(
        contentDetailViewModel = contentDetailViewModel,
        navController = navController,
        innersPadding = innersPadding
      )

    }
    composable(Screen.VideoPlayer.route) {
      VideoPlayerScreen(
        navController = navController,
        window = window,
        context = context,
        videoPlayerViewModel = videoPlayerViewModel,
        innersPadding = innersPadding,
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
        videoPlayerViewModel = videoPlayerViewModel,
      )
    }
  }
}
