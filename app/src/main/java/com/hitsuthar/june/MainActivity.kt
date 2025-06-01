package com.hitsuthar.june

import MovieSyncViewModel
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.moviebase.tmdb.model.TmdbMediaListItem
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.hitsuthar.june.screens.MediaCard
import com.hitsuthar.june.utils.TmdbRepository
import com.hitsuthar.june.viewModels.ContentDetailViewModel
import com.hitsuthar.june.viewModels.DDLViewModel
import com.hitsuthar.june.viewModels.SelectedVideoViewModel
import com.hitsuthar.june.viewModels.VideoPlayerViewModel
import com.hitsuthar.june.viewModels.WatchPartyViewModel

class MainActivity : ComponentActivity() {

  private lateinit var navController: NavHostController
  private val tmdbApiKey = "cbae4b34def0ffda695e0656c571cf33"

  @OptIn(ExperimentalMaterial3Api::class)
  @SuppressLint("CommitPrefEdits")
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    FirebaseApp.initializeApp(this)
//    FirebaseFirestore.setLoggingEnabled(true) // For debug
    val db = FirebaseFirestore.getInstance()

    setContent {
      val window = WindowCompat.getInsetsController(window, window.decorView)
      window.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//            window.hide(WindowInsetsCompat.Type.statusBars())
//            val serviceIntent = Intent(this, BinaryExecutionService::class.java)
//            ContextCompat.startForegroundService(this, serviceIntent)
//            val darkTheme = isSystemInDarkTheme()
      val darkTheme = true
      window.isAppearanceLightStatusBars = !darkTheme
      val serviceIntent = Intent(
        this, BinaryService::class.java
      )
      startService(serviceIntent)

      val selectedVideoViewModel: SelectedVideoViewModel = viewModel()
      val contentDetailViewModel: ContentDetailViewModel = viewModel()
      val watchPartyViewModel: WatchPartyViewModel = viewModel()
      val videoPlayerViewModel: VideoPlayerViewModel = viewModel()
      val ddlViewModel: DDLViewModel = viewModel()
      val movieSyncViewModel: MovieSyncViewModel = viewModel()
      val repository = TmdbRepository(tmdbApiKey)

      MaterialTheme(
        colorScheme = if (darkTheme) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(
          LocalContext.current
        )
      ) {
        navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        Scaffold(
          topBar = {
            when (currentRoute) {
              Screen.Home.route -> {
                var searchQuery by rememberSaveable { mutableStateOf("") }
                var expanded by rememberSaveable { mutableStateOf(false) }
                val focusRequester = remember { FocusRequester() }
                var searchResult by remember { mutableStateOf<List<TmdbMediaListItem>>(emptyList()) }
                LaunchedEffect(searchQuery) {
                  searchResult = repository.search(searchQuery).sortedByDescending { it.popularity }
                }
                SearchBar(
                  inputField = {
                    SearchBarDefaults.InputField(
                      query = searchQuery,
                      onQueryChange = { updatedQuery: String -> searchQuery = updatedQuery },
                      placeholder = { Text("Search") },
                      onSearch = { },
                      expanded = expanded,
                      onExpandedChange = { expanded = it },
                    )
                  },
                  expanded = expanded,
                  onExpandedChange = { expanded = it },
                  modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                ) {
                  LazyVerticalGrid(
//                    modifier = Modifier.padding(paddingValue),
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp), // Add padding around content
//            horizontalArrangement = Arrangement.spacedBy(8.dp), // Spacing between items in a row
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.Center
                  ) {
                    items(searchResult) {
                      MediaCard(
                        it,
                        navController,
                        contentDetailViewModel = contentDetailViewModel,
                        repository = repository
                      )
                    }
                  }
                }
              }


//              Screen.Party.route -> TopAppBar(title = { Text("Party") })
              Screen.Settings.route -> TopAppBar(title = { Text("Settings") })
              else -> {}
            }
          },
          bottomBar = {
              BottomNavigationBar(
                navController = navController,
                currentRoute = currentRoute,
                isFullScreen = videoPlayerViewModel.isFullScreen.collectAsState().value,
              )

          }
        ) { innersPadding ->
          SetupNavGraph(
            navController = navController,
            repository = repository,
            window = window,
            context = this,
            innersPadding = innersPadding,
            selectedVideoViewModel = selectedVideoViewModel,
            contentDetailViewModel = contentDetailViewModel,
            watchPartyViewModel = watchPartyViewModel,
            videoPlayerViewModel = videoPlayerViewModel,
            ddlViewModel = ddlViewModel,
            movieSyncViewModel = movieSyncViewModel
          )
        }
      }
    }
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    // VLCVideoLayout will handle the resize automatically
  }
}

