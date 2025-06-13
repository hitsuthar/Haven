package com.hitsuthar.june.screens

import android.util.Log
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import app.moviebase.tmdb.model.TmdbEpisode
import app.moviebase.tmdb.model.TmdbMovieDetail
import app.moviebase.tmdb.model.TmdbShowDetail
import com.hitsuthar.june.Screen
import com.hitsuthar.june.components.ErrorMessage
import com.hitsuthar.june.utils.TorrServerTorrentResponse
import com.hitsuthar.june.utils.postTorrents
import com.hitsuthar.june.utils.torrentProviders.TorrentStream
import com.hitsuthar.june.utils.torrentProviders.getCloudTorrent
import com.hitsuthar.june.utils.torrentProviders.getPeerFlix
import com.hitsuthar.june.utils.torrentProviders.getRARBG
import com.hitsuthar.june.utils.torrentProviders.getTheHiddenBay
import com.hitsuthar.june.utils.torrentProviders.getTorrentCSV
import com.hitsuthar.june.utils.torrentProviders.getTorrentGalaxy
import com.hitsuthar.june.utils.torrentProviders.getTorrentio
import com.hitsuthar.june.viewModels.ContentDetail
import com.hitsuthar.june.viewModels.ContentDetailViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Response

sealed class TorrentResponseState {
    data object Loading : TorrentResponseState()
    data class Success(val data: List<TorrentStream>?) : TorrentResponseState()
    data class Error(val message: String) : TorrentResponseState()
    data object Empty : TorrentResponseState()
}

data class TorrentProvider(
    val name: String,
    val itemFetcher: suspend (TmdbMovieDetail) -> Response<List<TorrentStream>>,
    val itemFetcherTv: suspend (TmdbShowDetail, TmdbEpisode) -> Response<List<TorrentStream>>,
) {
    suspend fun fetchMoviesStreams(tmdbMovieDetail: TmdbMovieDetail): Response<List<TorrentStream>> {
        return itemFetcher(tmdbMovieDetail)
    }

    suspend fun fetchShowsStreams(
        tmdbShowDetail: TmdbShowDetail, tmdbEpisode: TmdbEpisode
    ): Response<List<TorrentStream>> {
        Log.d("fetch", tmdbShowDetail.externalIds.toString())
        return itemFetcherTv(tmdbShowDetail, tmdbEpisode)
    }
}

val providers = listOf(
    TorrentProvider("Torrentio",
        { tmdbMovieDetail -> getTorrentio("movie", imdbId = tmdbMovieDetail.imdbId) },
        { tmdbShowDetail, tmdbEpisode ->
            getTorrentio(
                "show",
                imdbId = tmdbShowDetail.externalIds?.imdbId + ":" + tmdbEpisode.seasonNumber + ":" + tmdbEpisode.episodeNumber
            )
        }),
    TorrentProvider("TorrentCSV",
        { tmdbMovieDetail -> getTorrentCSV(tmdbMovieDetail.title) },
        { tmdbShowDetail, tmdbEpisode ->
            getTorrentCSV(buildString {
                append(tmdbShowDetail.name)
                append(" ")
                if (tmdbEpisode.seasonNumber < 10) append("S0" + tmdbEpisode.seasonNumber)
                else append("S" + tmdbEpisode.seasonNumber)
                if (tmdbEpisode.episodeNumber < 10) append("E0" + tmdbEpisode.episodeNumber)
                else append("E" + tmdbEpisode.episodeNumber)
            })
        }),
    TorrentProvider("TheHiddenBay",
        { tmdbMovieDetail -> getTheHiddenBay(tmdbMovieDetail.title) },
        { tmdbShowDetail, tmdbEpisode ->
            getTheHiddenBay(buildString {
                append(tmdbShowDetail.name)
                append(" ")
                if (tmdbEpisode.seasonNumber < 10) append("S0" + tmdbEpisode.seasonNumber)
                else append("S" + tmdbEpisode.seasonNumber)
                if (tmdbEpisode.episodeNumber < 10) append("E0" + tmdbEpisode.episodeNumber)
                else append("E" + tmdbEpisode.episodeNumber)
            })
        }),
    TorrentProvider("PeerFlix",
        { tmdbMovieDetail -> getPeerFlix("movie", imdbId = tmdbMovieDetail.imdbId) },
        { tmdbShowDetail, tmdbEpisode ->
            getPeerFlix(
                "show",
                imdbId = tmdbShowDetail.externalIds?.imdbId + ":" + tmdbEpisode.seasonNumber + ":" + tmdbEpisode.episodeNumber
            )
        }),
    TorrentProvider("TorrentGalaxy",
        { tmdbMovieDetail -> getTorrentGalaxy(tmdbMovieDetail.title) },
        { tmdbShowDetail, tmdbEpisode ->
            getTorrentGalaxy(buildString {
                append(tmdbShowDetail.name)
                append(" ")
                if (tmdbEpisode.seasonNumber < 10) append("S0" + tmdbEpisode.seasonNumber)
                else append("S" + tmdbEpisode.seasonNumber)
                if (tmdbEpisode.episodeNumber < 10) append("E0" + tmdbEpisode.episodeNumber)
                else append("E" + tmdbEpisode.episodeNumber)
            })
        }),
    TorrentProvider("RARBG",
        { tmdbMovieDetail -> getRARBG(tmdbMovieDetail.title) },
        { tmdbShowDetail, tmdbEpisode ->
            getRARBG(buildString {
                append(tmdbShowDetail.name)
                append(" ")
                if (tmdbEpisode.seasonNumber < 10) append("S0" + tmdbEpisode.seasonNumber)
                else append("S" + tmdbEpisode.seasonNumber)
                if (tmdbEpisode.episodeNumber < 10) append("E0" + tmdbEpisode.episodeNumber)
                else append("E" + tmdbEpisode.episodeNumber)
            })
        }),
    TorrentProvider("Cloud Torrent",
        { tmdbMovieDetail -> getCloudTorrent(tmdbMovieDetail.title, 5) },
        { tmdbShowDetail, tmdbEpisode ->
            getCloudTorrent(buildString {
                append(tmdbShowDetail.name)
                append(" ")
                if (tmdbEpisode.seasonNumber < 10) append("S0" + tmdbEpisode.seasonNumber)
                else append("S" + tmdbEpisode.seasonNumber)
                if (tmdbEpisode.episodeNumber < 10) append("E0" + tmdbEpisode.episodeNumber)
                else append("E" + tmdbEpisode.episodeNumber)
            }, 8)
        }),
)


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TorrentScreen(
    contentDetailViewModel: ContentDetailViewModel,
    navController: NavController,
    innersPadding: PaddingValues
) {
    val coroutineScope = rememberCoroutineScope()
    val contentDetail by contentDetailViewModel.contentDetail.collectAsState()

    var showBottomSheet by rememberSaveable { mutableStateOf(false) }

    var selected by rememberSaveable { mutableStateOf(providers.first().name) }
    val moviesTorrents = remember { mutableStateOf<List<TorrentStream>>(emptyList()) }
    val torrentResponse: MutableState<Response<List<TorrentStream>>?> = remember {
        mutableStateOf(null) // Initialize with null or a default empty response
    }
    val torrentResponseState: MutableState<TorrentResponseState> = remember {
        mutableStateOf(TorrentResponseState.Loading)
    }

    if (contentDetail is ContentDetail.Movie) {
        LaunchedEffect(selected) {
            coroutineScope.launch {
                torrentResponseState.value = TorrentResponseState.Loading
                torrentResponse.value = providers.find { it.name == selected }
                    ?.fetchMoviesStreams((contentDetail as ContentDetail.Movie).tmdbMovieDetail)
                if (torrentResponse.value?.isSuccessful == true) {
                    if (torrentResponse.value!!.body()?.isEmpty() == true) {
                        torrentResponseState.value = TorrentResponseState.Empty
                    } else torrentResponseState.value =
                        TorrentResponseState.Success(torrentResponse.value!!.body())
                } else torrentResponseState.value =
                    TorrentResponseState.Error("Error fetching torrents")
            }
        }
    } else if (contentDetail is ContentDetail.Show) {
        LaunchedEffect(selected) {
            coroutineScope.launch {
                torrentResponseState.value = TorrentResponseState.Loading
                torrentResponse.value = providers.find { it.name == selected }?.fetchShowsStreams(
                    (contentDetail as ContentDetail.Show).tmdbShowDetail,
                    (contentDetail as ContentDetail.Show).tmdbEpisode!!
                )
                if (torrentResponse.value?.isSuccessful == true) {
                    if (torrentResponse.value!!.body()?.isEmpty() == true) {
                        torrentResponseState.value = TorrentResponseState.Empty
                    } else torrentResponseState.value =
                        TorrentResponseState.Success(torrentResponse.value!!.body())
                } else torrentResponseState.value =
                    TorrentResponseState.Error("Error fetching torrents")
            }
        }
    }
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
                    "Torrent Provider: ",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                OutlinedButton(
                    onClick = { showBottomSheet = true }, shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        selected, style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(onDismissRequest = { showBottomSheet = false }) {
                providers.forEach { provider ->
                    Box(Modifier
                        .clickable {
                            selected = provider.name
                            moviesTorrents.value = emptyList()
                            coroutineScope.launch {
                                if (contentDetail is ContentDetail.Movie) torrentResponse.value =
                                    provider.fetchMoviesStreams((contentDetail as ContentDetail.Movie).tmdbMovieDetail)
                                else if (contentDetail is ContentDetail.Show) torrentResponse.value =
                                    provider.fetchShowsStreams(
                                        (contentDetail as ContentDetail.Show).tmdbShowDetail,
                                        (contentDetail as ContentDetail.Show).tmdbEpisode!!
                                    )

                            }
                            showBottomSheet = false
                        }
                        .padding(16.dp)
                        .fillMaxWidth()) {
                        Text(provider.name, style = MaterialTheme.typography.bodyLarge)
                    }
                    Box(
                        Modifier
                            .height(1.dp)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                    )
                }
            }
        }
        when (val state = torrentResponseState.value) {
            is TorrentResponseState.Loading -> LoadingIndicator(Modifier.size(50.dp).fillMaxSize().align(Alignment.CenterHorizontally))
            is TorrentResponseState.Success -> {
                LazyColumn {
                    items(state.data!!) {
                        TorrentButton(it, navController = navController)
                    }
                    item { Spacer(Modifier.height(innersPadding.calculateBottomPadding())) }
                }
            }

            is TorrentResponseState.Error -> ErrorMessage(message = state.message)
            is TorrentResponseState.Empty -> ErrorMessage(message = "Error fetching torrents")
        }
    }
}

@Composable
fun TorrentDialog(
    onDismiss: () -> Unit,
    item: TorrentStream,
    navController: NavController,
) {
    var response by remember { mutableStateOf<TorrServerTorrentResponse?>(null) }
    LaunchedEffect(key1 = Unit) {
        while (true) {
            response = postTorrents("add", item.magnet ?: item.infoHash)
            Log.d("TorrentDialog", "Response: ${item.magnet ?: item.infoHash}")
            delay(1000)
        }
    }
    Dialog(onDismissRequest = { onDismiss() }) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
        ) {
            Column(
                Modifier.padding(8.dp)
            ) {
                when (response?.stat) {
                    0 -> {
                        Row(Modifier.align(Alignment.CenterHorizontally)) {
                            CircularProgressIndicator(
                                color = Color.White, modifier = Modifier
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Adding torrent...",
                                Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }

                    1 -> {
                        Row(Modifier.align(Alignment.CenterHorizontally)) {
                            CircularProgressIndicator(
                                color = Color.White, modifier = Modifier
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Torrent getting info.",
                                Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }

                    3 -> {
                        Text(response!!.title)
                        Row {
                            Text("Peers: ", fontWeight = FontWeight.Bold)
                            Text(text = "${response!!.activePeers}/${response!!.totalPeers}")
                            Spacer(Modifier.width(8.dp))
                            Text("Seeds: ", fontWeight = FontWeight.Bold)
                            Text(response!!.connectedSeeders.toString())
                        }
                        val validSuffixes = listOf(
                            ".mkv", ".mp4", ".avi", ".webm", ".mov", ".flv", ".3gp"
                        )
                        // Filter valid video files
                        val videoFiles = response!!.fileStats.filter { fileStat ->
                            fileStat.path?.let { path ->
                                validSuffixes.any { suffix ->
                                    path.endsWith(
                                        suffix,
                                        ignoreCase = true
                                    )
                                }
                            } ?: false
                        }

                        if (videoFiles.size == 1) {
                            val singleFile = videoFiles[0]
                            Log.d("magnetlink", "Magnet: ${item.magnet}, File: ${singleFile.path}")
//                            selectedVideo.setSelectedVideo(
//                                video = Stream.Torrent(
//                                    TorrentStream(
//                                        magnet = item.magnet ?: item.infoHash,
//                                        fileIndex = singleFile.id
//                                    )
//                                )
//                            )
                            navController.navigate(Screen.VideoPlayer.route)
                            onDismiss()
                        }
                        LazyColumn {
                            items(videoFiles) {
                                Button(
                                    onClick = {
                                        Log.d("magnetlink", item.magnet.toString())
//                                        selectedVideo.setSelectedVideo(
//                                            video = Stream.Torrent(
//                                                TorrentStream(
//                                                    magnet = item.magnet ?: item.infoHash,
//                                                    fileIndex = it.id
//                                                )
//                                            )
//                                        )
                                        navController.navigate(route = Screen.VideoPlayer.route)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(
                                        vertical = 4.dp,
                                        horizontal = 8.dp
                                    ),
                                    modifier = Modifier.padding(top = 8.dp),
                                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondaryContainer),
                                ) {
                                    Text(
                                        it.path.toString(),
                                        fontWeight = FontWeight.Light,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        Row(Modifier.align(Alignment.CenterHorizontally)) {
                            CircularProgressIndicator(
                                color = Color.White, modifier = Modifier
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "No response yet", Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TorrentButton(
    item: TorrentStream, navController: NavController
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    if (showDialog) {
        TorrentDialog(
            item = item,
            onDismiss = { showDialog = false },
            navController = navController
        )
    }

    Box(Modifier.background(color = MaterialTheme.colorScheme.background)) {
        Button(
            onClick = {
                if (item.fileIndex == null) {
                    showDialog = true
                } else {
//                    selectedVideo.setSelectedVideo(Stream.Torrent(item))
                    navController.navigate(route = Screen.VideoPlayer.route)
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
                    append(item.title ?: item.name)

                    if (item.seeders != null) {
                        append("\n").append("⬆️").append(item.seeders)
                    }
                    if (item.peers != null) {
                        append("  ").append("⬇️").append(item.peers)
                    }
                    if (item.size != null) {
                        append("\n").append("💾").append(item.size)
                    }
                },
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Light
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}