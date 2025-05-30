package com.hitsuthar.june.screens

import MovieSyncViewModel
import android.annotation.SuppressLint
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.moviebase.tmdb.model.TmdbEpisode
import app.moviebase.tmdb.model.TmdbFileImage
import app.moviebase.tmdb.model.TmdbSeason
import coil.compose.rememberAsyncImagePainter
import com.hitsuthar.june.BACKDROP_BASE_URL
import com.hitsuthar.june.LOGO_BASE_URL
import com.hitsuthar.june.Screen
import com.hitsuthar.june.components.ErrorMessage
import com.hitsuthar.june.components.LoadingIndicator
import com.hitsuthar.june.utils.TmdbRepository
import com.hitsuthar.june.utils.getFormattedDate
import com.hitsuthar.june.viewModels.ContentDetail
import com.hitsuthar.june.viewModels.ContentDetailViewModel
import com.hitsuthar.june.viewModels.DDLViewModel
import com.hitsuthar.june.viewModels.SelectedVideoViewModel
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes


@Composable
fun DetailScreen(
  navController: NavController,
  repository: TmdbRepository,
  contentDetailViewModel: ContentDetailViewModel,
  selectedVideoViewModel: SelectedVideoViewModel,
  ddlViewModel: DDLViewModel,
  innersPadding: PaddingValues,
  movieSyncViewModel: MovieSyncViewModel,

  ) {
  val contentDetail by contentDetailViewModel.contentDetail.collectAsState()

  when (contentDetail) {
    ContentDetail.Loading -> LoadingIndicator()
    is ContentDetail.Movie -> {
      MovieDetailContent(
        contentDetail as ContentDetail.Movie,
        navController = navController,
        selectedVideoViewModel = selectedVideoViewModel,
        ddlViewModel = ddlViewModel,
        innersPadding = innersPadding,
        movieSyncViewModel = movieSyncViewModel
      )
      LaunchedEffect(contentDetail) {
        contentDetail.let { ddlViewModel.fetchAllProviders(it) }
      }
    }

    is ContentDetail.Show -> {
      ShowDetailContent(
        contentDetail as ContentDetail.Show,
        navController = navController,
        repository = repository,
        contentDetailViewModel = contentDetailViewModel,
        ddlViewModel = ddlViewModel,
        innersPadding = innersPadding

      )
    }

    is ContentDetail.Episode -> {
      BackHandler {
        contentDetailViewModel.setContentDetail(
          (contentDetail as ContentDetail.Episode).contentId,
          contentType = "tv",
          repository = repository
        )
        navController.popBackStack()
      }
      EpisodeDetailContent(
        contentDetail as ContentDetail.Episode,
        navController = navController,
        selectedVideoViewModel = selectedVideoViewModel,
        ddlViewModel = ddlViewModel,
        innersPadding = innersPadding,
        movieSyncViewModel = movieSyncViewModel
      )
    }
    ContentDetail.Error -> ErrorMessage("Error Loading Content\n:(")
  }
}

@Composable
fun EpisodeDetailContent(
  detail: ContentDetail.Episode,
  navController: NavController,
  selectedVideoViewModel: SelectedVideoViewModel,
  ddlViewModel: DDLViewModel,
  innersPadding: PaddingValues,
  movieSyncViewModel: MovieSyncViewModel,

  ) {
  Box(
    Modifier
      .background(color = MaterialTheme.colorScheme.background)
      .fillMaxSize()
  ) {
    Image(
      painter = rememberAsyncImagePainter("$BACKDROP_BASE_URL${detail.tmdbEpisodeDetail.backdropPath}"),
      contentDescription = "back drop image",
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(16 / 9f)
    )
    LazyColumn {
      item { BackdropSection(detail) }
      item {
        DDLScreen(
          navController = navController,
          selectedVideo = selectedVideoViewModel,
          innersPadding = PaddingValues(0.dp),
          ddlViewModel = ddlViewModel,
          movieSyncViewModel = movieSyncViewModel
        )
      }
      item {
        Spacer(Modifier.height(innersPadding.calculateBottomPadding()))
      }
    }
  }
}

sealed class MediaContent {
  abstract val streams: List<DDLStream>

  data class Movie(
    override val streams: List<DDLStream>
  ) : MediaContent()

  data class TvSeries(
    val seasons: List<Season>
  ) : MediaContent() {
    override val streams: List<DDLStream>
      get() = seasons.flatMap { it.episodes.flatMap { it1 -> it1.streams } }

    data class Season(
      val number: Int,
      val episodes: List<Episode>
    )

    data class Episode(
      val number: Int,
      val title: String? = null,
      val streams: List<DDLStream>
    )
  }
}

data class DDLStream(
  val name: String, // e.g., "480p", "720p", "1080p"
  val url: String,      // e.g., "http://example.com/download1"
  val size: String? = null
)

@SuppressLint("MutableCollectionMutableState")
@Composable
fun MovieDetailContent(
  detail: ContentDetail.Movie,
  navController: NavController,
  selectedVideoViewModel: SelectedVideoViewModel,
  ddlViewModel: DDLViewModel,
  innersPadding: PaddingValues,
  movieSyncViewModel: MovieSyncViewModel,

  ) {
  val logo = if (detail.tmdbMovieDetail.images?.logos.isNullOrEmpty()) {
    null
  } else {
    detail.tmdbMovieDetail.images?.logos?.find { it.iso639 == "en" }
      ?: detail.tmdbMovieDetail.images?.logos?.first()
  }

  Log.d("detail", detail.tmdbMovieDetail.toString())
  Box(
    Modifier
      .background(color = MaterialTheme.colorScheme.background)
      .fillMaxSize()
  ) {
    Image(
      painter = rememberAsyncImagePainter("$BACKDROP_BASE_URL${detail.tmdbMovieDetail.backdropPath}"),
      contentDescription = "back drop image",
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(16 / 9f)
    )
    LazyColumn {
      item { BackdropSection(detail, logo) }
      item {
        DDLScreen(
          navController = navController,
          selectedVideo = selectedVideoViewModel,
          innersPadding = PaddingValues(0.dp),
          ddlViewModel = ddlViewModel,
          movieSyncViewModel = movieSyncViewModel
        )
      }
//            item {
//                Column(Modifier.fillMaxWidth()) {
//                    OutlinedButton(
//                        onClick = { navController.navigate(route = Screen.DDL.route) },
//                        modifier = Modifier.align(Alignment.CenterHorizontally),
//                        shape = RoundedCornerShape(16.dp)
//                    ) {
//                        Text("Select DDL manually", style = MaterialTheme.typography.bodyMedium)
//                    }
//                    OutlinedButton(
//                        onClick = { navController.navigate(route = Screen.Torrent.route) },
//                        modifier = Modifier.align(Alignment.CenterHorizontally),
//                        shape = RoundedCornerShape(16.dp)
//                    ) {
//                        Text("Select torrent manually", style = MaterialTheme.typography.bodyMedium)
//                    }
//                }
//            }
//            item { TorrentList(detail, navController, selectedVideo) }
      item {
        Spacer(Modifier.height(innersPadding.calculateBottomPadding()))
      }
    }
  }
}


@Composable
fun ShowDetailContent(
  detail: ContentDetail.Show,
  navController: NavController,
  repository: TmdbRepository,
  contentDetailViewModel: ContentDetailViewModel,
  ddlViewModel: DDLViewModel,
  innersPadding: PaddingValues
) {
  val logo = detail.tmdbShowDetail.images?.logos?.find { it.iso639 == "en" }
    ?: detail.tmdbShowDetail.images?.logos?.first()
  val seasons = detail.tmdbShowDetail.seasons.filter { season ->
    !season.name.contains("special", true)
  }
  var selectedSeason by remember { mutableStateOf<TmdbSeason?>(null) }
  var tabIndex by rememberSaveable { mutableIntStateOf(0) }
  var episodes by rememberSaveable { mutableStateOf<List<TmdbEpisode>>(emptyList()) }
  val coroutineScope = rememberCoroutineScope()


  LaunchedEffect(key1 = tabIndex, key2 = seasons) {
    if (seasons.isNotEmpty()) {
      selectedSeason = seasons[tabIndex]
      Log.d("Show ID: ", detail.tmdbShowDetail.id.toString())
      Log.d("selected season: ", selectedSeason.toString())
      episodes = repository.getShowSeasonDetail(
        showId = detail.tmdbShowDetail.id,
        seasonNumber = selectedSeason!!.seasonNumber,
      ).episodes!!
    }
  }
  Box(
    Modifier
      .background(color = MaterialTheme.colorScheme.background)
      .fillMaxSize()
  ) {
    Image(
      painter = rememberAsyncImagePainter("$BACKDROP_BASE_URL${detail.tmdbShowDetail.backdropPath}"),
      contentDescription = "back drop image",
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(16 / 9f)
    )
    LazyColumn {
      item {
        BackdropSection(detail, logo)
      }
      if (seasons.isNotEmpty()) {
        item {
          ScrollableTabRow(
            selectedTabIndex = tabIndex,
            edgePadding = 16.dp,
            divider = {},
            contentColor = Color.White,
            indicator = { tabPositions ->
//                            TabRowDefaults.SecondaryIndicator(height = 10.dp, )
              TabRowDefaults.PrimaryIndicator(
                height = 32.dp,
                color = Color.Transparent,
                modifier = Modifier
                  .tabIndicatorOffset(tabPositions[tabIndex])
                  .fillMaxSize()
                  .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(16.dp)
                  )
              )
            },
            modifier = Modifier.height(32.dp),
          ) {
            seasons.forEachIndexed { index, season ->
              Tab(
                selected = tabIndex == index,
                selectedContentColor = MaterialTheme.colorScheme.onBackground,
                unselectedContentColor = MaterialTheme.colorScheme.onBackground,
                onClick = {
                  tabIndex = index
                },
                modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                text = {
                  Text(
                    text = season.name, style = MaterialTheme.typography.bodySmall
                  )
                })
            }
          }
        }
      }

      if (episodes.isNotEmpty()) {
        items(episodes) {


          Box(
            Modifier
              .background(color = MaterialTheme.colorScheme.background)
              .padding(top = 16.dp, start = 16.dp, end = 16.dp)
          ) {
            Button(
              onClick = {

                contentDetailViewModel.setContentDetail(
                  contentType = "episode",
                  repository = repository,
                  tmdbEpisode = it,
                  contentId = detail.tmdbShowDetail.id,
                  episodeId = detail.tmdbEpisode?.id,
                )

                coroutineScope.launch {
                  contentDetailViewModel.setContentDetail(
                    contentType = "episode",
                    repository = repository,
                    tmdbEpisode = it,
                    contentId = detail.tmdbShowDetail.id,
                    episodeId = detail.tmdbEpisode?.id,
                  )
                  detail.tmdbEpisode = it
                  ddlViewModel.fetchAllProviders(
                    contentDetail = ContentDetail.Show(
                      detail.tmdbShowDetail, detail.tmdbEpisode
                    )
                  )
                  navController.navigate(
                    Screen.Detail.route
                  )
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth(),
              contentPadding = PaddingValues(8.dp),

              ) {
              Row(
                Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 8.dp)
              ) {
                Text(
                  text = if (it.episodeNumber < 10) "Ep:0${it.episodeNumber}  "
                  else "Ep:${it.episodeNumber}  ", style = TextStyle(
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                  )
                )
                Text(
                  it.name!!,
                  style = TextStyle(color = MaterialTheme.colorScheme.onSecondaryContainer)
                )
              }
            }

          }
        }
      }
      item {
        Spacer(Modifier.height(innersPadding.calculateBottomPadding()))
      }
    }
  }
}


@Composable
fun BackdropSection(detail: ContentDetail, logoImage: TmdbFileImage? = null) {
  Box(
    Modifier.height((512 + 24).dp)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight()
        .background(
          brush = Brush.verticalGradient(
            listOf(
              Color.Transparent,
//                            MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
              MaterialTheme.colorScheme.background.copy(alpha = 1f)
            ), startY = 0f, endY = (512 + 64).toFloat()
          )
        )
    )
    BottomInfoSection(detail, logoImage)
  }
}

@SuppressLint("DefaultLocale")
@Composable
fun BottomInfoSection(detail: ContentDetail, logoImage: TmdbFileImage?) {
  Box(
    modifier = Modifier.fillMaxSize()
  ) {
    Column(
      Modifier.align(Alignment.BottomCenter)
//                .padding(top = (128).dp)
    ) {
      Box(Modifier.align(Alignment.CenterHorizontally)) {
        if (logoImage?.filePath.isNullOrEmpty()) {
          if (detail is ContentDetail.Movie) {
            Text(
              detail.tmdbMovieDetail.title, style = MaterialTheme.typography.headlineLarge.copy(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
              ), modifier = Modifier.padding(horizontal = 16.dp)
            )
          } else if (detail is ContentDetail.Show) {
            Text(
              detail.tmdbShowDetail.name, style = MaterialTheme.typography.headlineLarge.copy(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
              ), modifier = Modifier.padding(horizontal = 16.dp)
            )
          } else if (detail is ContentDetail.Episode) {
            detail.tmdbEpisodeDetail.name?.let {
              Text(
                it, style = MaterialTheme.typography.headlineLarge.copy(
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center
                ), modifier = Modifier.padding(horizontal = 16.dp)
              )
            }
          }
        } else {
          Image(
            painter = rememberAsyncImagePainter("$LOGO_BASE_URL${logoImage?.filePath}"),
            contentDescription = "logo",
          )
        }
      }

      Row(
        Modifier
          .align(Alignment.CenterHorizontally)
          .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)

      ) {
        if (detail is ContentDetail.Movie) {
          Text(
            text = getFormattedDate(detail.tmdbMovieDetail.releaseDate), style = TextStyle(
              color = MaterialTheme.colorScheme.onBackground,
              fontWeight = FontWeight.Normal,
              fontSize = 16.sp
            )
          )
        } else if (detail is ContentDetail.Show) {
          Text(
            text = getFormattedDate(detail.tmdbShowDetail.firstAirDate), style = TextStyle(
              color = MaterialTheme.colorScheme.onBackground,
              fontWeight = FontWeight.Normal,
              fontSize = 16.sp
            )
          )
        } else if (detail is ContentDetail.Episode) {
          Text(
            text = getFormattedDate(detail.tmdbEpisodeDetail.airDate), style = TextStyle(
              color = MaterialTheme.colorScheme.onBackground,
              fontWeight = FontWeight.Normal,
              fontSize = 16.sp
            )
          )
        }

        Text(
          "•",
          style = TextStyle(
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp
          ),
        )
        if (detail is ContentDetail.Movie) {
          Text(
            detail.tmdbMovieDetail.runtime?.minutes.toString(), style = TextStyle(
              color = MaterialTheme.colorScheme.onBackground,
              fontWeight = FontWeight.Normal,
              fontSize = 16.sp
            )
          )
        } else if (detail is ContentDetail.Show) {
          Text(
            detail.tmdbShowDetail.numberOfEpisodes.toString() + " Episodes", style = TextStyle(
              color = MaterialTheme.colorScheme.onBackground,
              fontWeight = FontWeight.Normal,
              fontSize = 16.sp
            )
          )
        } else if (detail is ContentDetail.Episode) {
          Text(
            "Ep " + detail.tmdbEpisodeDetail.episodeNumber, style = TextStyle(
              color = MaterialTheme.colorScheme.onBackground,
              fontWeight = FontWeight.Normal,
              fontSize = 16.sp
            )
          )
        }
        Text(
          "•",
          style = TextStyle(
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp
          ),
        )
        if (detail is ContentDetail.Movie) {
          Text(
            String.format("%.1f", detail.tmdbMovieDetail.voteAverage),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Normal,
          )
        } else if (detail is ContentDetail.Show) {
          Text(
            String.format("%.1f", detail.tmdbShowDetail.voteAverage),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Normal,
          )
        } else if (detail is ContentDetail.Episode) {
          Text(
            String.format("%.1f", detail.tmdbEpisodeDetail.voteAverage),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Normal,
          )
        }


      }
      if (detail is ContentDetail.Movie) {
        Text(
          detail.tmdbMovieDetail.overview, style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Light

          ), modifier = Modifier.padding(start = 24.dp, top = 8.dp, end = 24.dp)
        )
      } else if (detail is ContentDetail.Show) {
        Text(
          detail.tmdbShowDetail.overview, style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Light

          ), modifier = Modifier.padding(start = 24.dp, top = 8.dp, end = 24.dp)
        )
      } else if (detail is ContentDetail.Episode) {
        Text(
          detail.tmdbEpisodeDetail.overview, style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Light

          ), modifier = Modifier.padding(start = 24.dp, top = 8.dp, end = 24.dp)
        )
      }
      if (detail is ContentDetail.Movie) {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(top = 16.dp)
        ) {
          items(detail.tmdbMovieDetail.genres) { genre ->
            Text(
              genre.name, style = MaterialTheme.typography.bodyMedium.copy(

                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Light

              ), modifier = Modifier
                .background(
                  color = MaterialTheme.colorScheme.secondaryContainer,
                  shape = RoundedCornerShape(32.dp)
                )
                .padding(vertical = 4.dp, horizontal = 8.dp)
            )
          }
        }
      } else if (detail is ContentDetail.Show) {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(top = 16.dp, start = 8.dp, end = 8.dp)
            .clip(RoundedCornerShape(16.dp))
        ) {
          items(detail.tmdbShowDetail.genres) { genre ->
            Text(
              genre.name, style = MaterialTheme.typography.bodyMedium.copy(

                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Light

              ), modifier = Modifier
                .background(
                  color = MaterialTheme.colorScheme.secondaryContainer,
                  shape = RoundedCornerShape(32.dp)
                )
                .padding(vertical = 4.dp, horizontal = 8.dp)
            )
          }

        }
      } else if (detail is ContentDetail.Episode) {
      }
      Spacer(Modifier.height(24.dp))
    }
  }
}



