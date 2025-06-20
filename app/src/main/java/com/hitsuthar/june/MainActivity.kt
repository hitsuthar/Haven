package com.hitsuthar.june

import MovieSyncViewModel
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.moviebase.tmdb.model.TmdbMediaListItem
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.hitsuthar.june.components.MediaCard
import com.hitsuthar.june.utils.TmdbRepository
import com.hitsuthar.june.viewModels.ContentDetailViewModel
import com.hitsuthar.june.viewModels.DDLViewModel
import com.hitsuthar.june.viewModels.VideoPlayerViewModel
import android.provider.Settings
import android.util.Log
import com.hitsuthar.june.utils.SimpleIdGenerator
import com.hitsuthar.june.viewModels.HomeScreenViewModel


class MainActivity : ComponentActivity() {

  private lateinit var navController: NavHostController
  private val tmdbApiKey = "cbae4b34def0ffda695e0656c571cf33"
  private val repository = TmdbRepository(tmdbApiKey)


  private val homeScreenViewModel: HomeScreenViewModel by viewModels {
    object : ViewModelProvider.Factory {
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeScreenViewModel::class.java)) {
          val repository = repository
          @Suppress("UNCHECKED_CAST") return HomeScreenViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
      }
    }
  }
  private val contentDetailViewModel: ContentDetailViewModel by viewModels()
  private val videoPlayerViewModel: VideoPlayerViewModel by viewModels {
    object : ViewModelProvider.Factory {
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VideoPlayerViewModel::class.java)) {
          val application = this@MainActivity.application
          @Suppress("UNCHECKED_CAST")
          // Ensure VideoPlayerViewModel constructor matches this:
          return VideoPlayerViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class") // This is not an UnsupportedOperationException
      }
    }
  }
  private val ddlViewModel: DDLViewModel by viewModels()
  private val movieSyncViewModel: MovieSyncViewModel by viewModels {
    object : ViewModelProvider.Factory {
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MovieSyncViewModel::class.java)) {
          val videoPlayerViewModel = videoPlayerViewModel

          @Suppress("UNCHECKED_CAST")
          // Ensure VideoPlayerViewModel constructor matches this:
          return MovieSyncViewModel(videoPlayerViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class") // This is not an UnsupportedOperationException
      }
    }
  }


  @SuppressLint("HardwareIds")
  @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    FirebaseApp.initializeApp(this)
//    FirebaseFirestore.setLoggingEnabled(true) // For debug
    val db = FirebaseFirestore.getInstance()


    val spm = SharedPreferencesManager(this.applicationContext)
    val isFirstRun: Boolean = spm.getData("FIRST_RUN_KEY", true)
    Log.d("isFirstRun", isFirstRun.toString())

    if (isFirstRun) {
      spm.saveData(
        "USER_ID", Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
      )
      spm.saveData("PARTY_ID", SimpleIdGenerator().generateRandomId())
      spm.saveData("FIRST_RUN_KEY", false)
      spm.saveData("USER_NAME", "June")
    }
    val userID = SharedPreferencesManager(this.applicationContext).getData("USER_ID", "")
    movieSyncViewModel.checkAlreadyJoinedRoom(userID)


    setContent {
      val window = WindowCompat.getInsetsController(window, window.decorView)
      window.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//            window.hide(WindowInsetsCompat.Type.statusBars())
//            val serviceIntent = Intent(this, BinaryExecutionService::class.java)
//            ContextCompat.startForegroundService(this, serviceIntent)
//            val darkTheme = isSystemInDarkTheme()
      val darkTheme = true
      window.isAppearanceLightStatusBars = !darkTheme
//      val serviceIntent = Intent(
//        this, BinaryService::class.java
//      )
//      startService(serviceIntent)


      MaterialTheme(
        colorScheme = if (darkTheme) dynamicDarkColorScheme(LocalContext.current)
        else dynamicLightColorScheme(LocalContext.current)
      ) {
        navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        var expanded by rememberSaveable { mutableStateOf(false) }
        val topAppBarScrollBehavior =
          if (expanded) TopAppBarDefaults.pinnedScrollBehavior() else TopAppBarDefaults.enterAlwaysScrollBehavior()

        Scaffold(
          modifier = Modifier
//            .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
          , topBar = {
            when (currentRoute) {
              Screen.Home.route -> {
                var searchQuery by rememberSaveable { mutableStateOf("") }

//                val focusRequester = remember { FocusRequester() }
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
                  modifier = Modifier.fillMaxWidth()
//                    .focusRequester(focusRequester)
                ) {
                  LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp), // Add padding around content
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    items(searchResult) {
                      MediaCard(
                        it, navController, contentDetailViewModel, repository
                      )
                    }
                  }
                }

              }

              Screen.Settings.route -> TopAppBar(title = { Text("Settings") })
              else -> {}
            }
          }, bottomBar = {
            if (!expanded) {
              BottomNavigationBar(
                navController = navController,
                currentRoute = currentRoute,
                isFullScreen = videoPlayerViewModel.uiState.collectAsState().value.isFullScreen,
              )
            }


          }) { innersPadding ->
          SetupNavGraph(
            navController = navController,
            repository = repository,
            window = window,
            context = this,
            innersPadding = innersPadding,
            homeScreenViewModel = homeScreenViewModel,
            contentDetailViewModel = contentDetailViewModel,
            videoPlayerViewModel = videoPlayerViewModel,
            ddlViewModel = ddlViewModel,
            movieSyncViewModel = movieSyncViewModel,
          )
        }
      }
    }
  }

  override fun onPause() {
    super.onPause()
    videoPlayerViewModel.onHostPause()
  }

  override fun onResume() {
    super.onResume()
    videoPlayerViewModel.onHostResume()
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    // VLCVideoLayout will handle the resize automatically
  }
}

