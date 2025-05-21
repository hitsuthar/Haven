package com.hitsuthar.june

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
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
                            Screen.Home.route -> TopAppBar(title = { Text("Home") })
                            Screen.Party.route -> TopAppBar(title = { Text("Party") })
                            Screen.Settings.route -> TopAppBar(title = { Text("Settings") })
                            else -> {}
                        }
                    },
                    bottomBar = {
                        BottomNavigationBar(
                            navController = navController,
                            currentRoute = currentRoute,
                            isFullScreen = videoPlayerViewModel.isFullScreen.collectAsState().value
                        )
                    }
                ) { innersPadding ->
                    SetupNavGraph(
                        navController = navController,
                        repository = TmdbRepository(tmdbApiKey),
                        window = window,
                        context = this,
                        innersPadding = innersPadding,
                        selectedVideoViewModel = selectedVideoViewModel,
                        contentDetailViewModel = contentDetailViewModel,
                        watchPartyViewModel = watchPartyViewModel,
                        videoPlayerViewModel = videoPlayerViewModel,
                        ddlViewModel = ddlViewModel
                    )
                }
            }
        }
    }
}

