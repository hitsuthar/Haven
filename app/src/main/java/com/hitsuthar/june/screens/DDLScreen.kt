package com.hitsuthar.june.screens

import MovieSyncViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hitsuthar.june.Screen
import com.hitsuthar.june.components.ErrorMessage
import com.hitsuthar.june.utils.dDLProviders.DDLProviders
import com.hitsuthar.june.viewModels.DDLState
import com.hitsuthar.june.viewModels.DDLViewModel
import com.hitsuthar.june.viewModels.VideoPlayerViewModel


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DDLScreen(
  navController: NavController,
  innersPadding: PaddingValues,
  ddlViewModel: DDLViewModel,
  movieSyncViewModel: MovieSyncViewModel,
  videoPlayerViewModel: VideoPlayerViewModel
) {
  var showBottomSheet by rememberSaveable { mutableStateOf(false) }
  val selectedProvider by ddlViewModel.selectedProvider.collectAsState()
  val currentStreams by ddlViewModel.currentStreams.collectAsState()
  val ddlState by ddlViewModel.state.collectAsState()
  Column(
    modifier = Modifier.padding(
      top = innersPadding.calculateTopPadding(),
    )
  ) {
    Box(
      Modifier
        .background(color = MaterialTheme.colorScheme.background)
        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        .fillMaxWidth()
    ) {
      Row(
        horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          "Select Provider: ",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.align(Alignment.CenterVertically)
        )
        OutlinedButton(
          onClick = { showBottomSheet = true }, shape = RoundedCornerShape(16.dp)
        ) {
          Text(
            selectedProvider ?: "Select", style = MaterialTheme.typography.bodyLarge
          )
        }
      }
    }

    if (showBottomSheet) {
      ModalBottomSheet(onDismissRequest = { showBottomSheet = false }) {
        when (ddlState) {
          DDLState.Loading -> {
            // Show loading providers
            Column {
              Text(
                "Loading providers...",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp)
              )
              DDLProviders.forEach { provider ->
                ProviderItem(
                  provider = provider.name, isLoading = true, onClick = {})
                HorizontalDivider()

              }
            }
          }

          is DDLState.PartialSuccess -> {
            val state = ddlState as DDLState.PartialSuccess
            Column {
              // Available providers section
              if (state.availableProviders.isNotEmpty()) {
                Text(
                  "Available:",
                  style = MaterialTheme.typography.labelMedium,
                  modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                )
                state.availableProviders.forEach { provider ->
                  ProviderItem(
                    provider = provider, isSelected = provider == selectedProvider, onClick = {
                      ddlViewModel.selectProvider(provider)
                      showBottomSheet = false
                    })
                  HorizontalDivider()
                }
              }
              // Loading providers
              if (state.loadingProviders.isNotEmpty()) {
                Text(
                  "Loading:",
                  style = MaterialTheme.typography.labelMedium,
                  modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                )
                state.loadingProviders.forEach { provider ->
                  ProviderItem(
                    provider = provider, isLoading = true, onClick = {})
                  HorizontalDivider()
                }
              }
              // Failed providers
              if (state.failedProviders.isNotEmpty()) {
                Text(
                  "Failed:",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.error,
                  modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                )
                state.failedProviders.forEach { (provider, error) ->
                  ProviderItem(
                    provider = provider, isError = true, errorMessage = error, onClick = {})
                  HorizontalDivider()
                }
              }
            }
          }

          is DDLState.Error -> {
            // Show error state
            Column(Modifier.padding(16.dp)) {
              Text(
                "Failed to load providers",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
              )
              Text(
                (ddlState as DDLState.Error).message, style = MaterialTheme.typography.bodyMedium
              )
            }
          }
        }
      }
    }

    // Content area
    when {
      currentStreams?.streams?.isNotEmpty() == true -> Column {
        currentStreams!!.streams.forEach { stream ->
          DDLButton(
            item = stream,
            navController = navController,
            movieSyncViewModel = movieSyncViewModel,
            videoPlayerViewModel = videoPlayerViewModel
          )
        }
        Spacer(Modifier.height(innersPadding.calculateBottomPadding()))
      }

      selectedProvider != null -> ErrorMessage(message = "No streams found for $selectedProvider")
      ddlState is DDLState.Error -> ErrorMessage(message = (ddlState as DDLState.Error).message)
      else -> LoadingIndicator(
        Modifier
          .size(50.dp)
          .fillMaxSize()
          .align(Alignment.CenterHorizontally)
      )
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProviderItem(
  provider: String,
  isSelected: Boolean = false,
  isLoading: Boolean = false,
  isError: Boolean = false,
  errorMessage: String? = null,
  onClick: () -> Unit
) {
  Box(
    Modifier
      .clickable(enabled = !isLoading && !isError, onClick = onClick)
      .padding(16.dp)
      .fillMaxWidth()
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          provider,
          color = if (isError) MaterialTheme.colorScheme.error else TextStyle.Default.color
        )
//                if (isError && errorMessage != null) {
//                    Text(
//                        "($errorMessage)",
//                        style = MaterialTheme.typography.labelSmall,
//                        color = MaterialTheme.colorScheme.error,
//                        modifier = Modifier
//                            .padding(top = 4.dp)
//                            .fillMaxWidth()
//                    )
//                }
      }

      when {
        isSelected -> Icon(Icons.Default.Check, contentDescription = "Selected")
        isLoading -> LoadingIndicator(
          Modifier
            .size(32.dp)
            .fillMaxSize()
            .align(Alignment.CenterVertically)
        )

        isError -> Icon(
          Icons.Default.Close, contentDescription = "Error", tint = MaterialTheme.colorScheme.error
        )
      }
    }


  }
  HorizontalDivider()
}

@Composable
fun DDLButton(
  item: DDLStream,
  navController: NavController,
  movieSyncViewModel: MovieSyncViewModel,
  videoPlayerViewModel: VideoPlayerViewModel

) {
  val currentRoom by movieSyncViewModel.currentRoom.collectAsState()

  Box(Modifier.background(color = MaterialTheme.colorScheme.background)) {
    Button(
      onClick = {
        if (currentRoom == null) {
//          selectedVideo.setSelectedVideo(Stream.DDL(item))
          videoPlayerViewModel.loadVideo(item.url, item.name)
          navController.navigate(route = Screen.VideoPlayer.route)
        } else {
          movieSyncViewModel.updateCurrentMovie(
            MovieSyncViewModel.Movie(
              item.name, item.url.replace(
                oldValue = " ", newValue = "%20"
              )
            )
          )
          navController.navigate(route = Screen.Party.route)
        }
      },
      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp, horizontal = 16.dp),
      contentPadding = PaddingValues(8.dp)
    ) {
      Icon(
        Icons.Default.PlayArrow,
        contentDescription = "Play",
        tint = MaterialTheme.colorScheme.onSecondaryContainer,
      )
      Spacer(Modifier.width(8.dp))
      Text(
        text = buildString {
          append(item.name)
          if (item.size != null) {
            append("\n").append("💾").append(item.size)
          }
        },
        style = MaterialTheme.typography.bodyMedium.copy(
          color = MaterialTheme.colorScheme.onSecondaryContainer,
          fontWeight = FontWeight.Light
        ),
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}