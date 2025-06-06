package com.hitsuthar.june.components

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.moviebase.tmdb.model.TmdbMediaListItem
import app.moviebase.tmdb.model.TmdbMovie
import app.moviebase.tmdb.model.TmdbShow
import coil.compose.rememberAsyncImagePainter
import com.hitsuthar.june.POSTER_BASE_URL
import com.hitsuthar.june.Screen
import com.hitsuthar.june.utils.TmdbRepository
import com.hitsuthar.june.viewModels.ContentDetailViewModel

@SuppressLint("DefaultLocale")
@Composable
fun MediaCard(
  content: TmdbMediaListItem,
  navController: NavController,
  contentDetailViewModel: ContentDetailViewModel,
  repository: TmdbRepository
) {
  val (title, posterPath) = when (content) {
    is TmdbMovie -> content.title to content.posterPath
    is TmdbShow -> content.name to content.posterPath
    else -> "" to ""
  }
  ElevatedCard(
    modifier = Modifier
      .width(130.dp)
      .aspectRatio(2 / 3f)
      .clickable {
        contentDetailViewModel.setContentDetail(
          contentId = content.id, contentType = when (content) {
            is TmdbMovie -> "movie"
            is TmdbShow -> "tv"
          }, repository = repository
        )
        navController.navigate(
          Screen.Detail.route
        )
      }, shape = RoundedCornerShape(8.dp)
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      Image(
        painter = rememberAsyncImagePainter(POSTER_BASE_URL + posterPath),
        contentDescription = title,
        contentScale = ContentScale.Crop,
//        modifier = Modifier.aspectRatio(2 / 3f)
//          .fillMaxWidth()
        // Maintain aspect ratio
      )
      Column(
        verticalArrangement = Arrangement.SpaceBetween,
//        horizontalAlignment = Alignment.End,
        modifier = Modifier
          .fillMaxSize()
          .padding(4.dp)
      ) {
        Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.fillMaxWidth()) {
          Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
              .background(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                shape = RoundedCornerShape(4.dp)
              )
              .padding(horizontal = 6.dp, vertical = 2.dp),
          ) {
            Icon(
              Icons.Default.Star,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              contentDescription = "Rating",
              modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
              text = when (content) {
                is TmdbMovie -> String.format("%.1f", content.voteAverage)
                is TmdbShow -> String.format("%.1f", content.voteAverage)
                else -> ""
              },
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier
            )
          }
        }
        Text(
          modifier = Modifier
            .background(
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
              shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),

          text = title,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Light
        )
      }
    }
  }
}