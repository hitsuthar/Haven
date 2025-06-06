package com.hitsuthar.june.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.moviebase.tmdb.model.TmdbMediaListItem
import com.hitsuthar.june.components.MediaCard
import com.hitsuthar.june.utils.TmdbRepository
import com.hitsuthar.june.viewModels.ContentDetailViewModel

// Data classes
data class Category(
  val name: String, val subcategories: List<Subcategory>
)

data class Subcategory(
  val type: String,
  val itemFetcher: suspend (Int) -> List<TmdbMediaListItem>, // Fetch function with page number as parameter
  var data: PagingData = PagingData()
) {
  suspend fun loadMore() {
    val newItems = itemFetcher.invoke(data.currentPage + 1)
    data = data.copy(items = data.items + newItems, currentPage = data.currentPage + 1)
  }
}

data class PagingData(
  val items: List<TmdbMediaListItem> = emptyList(), val currentPage: Int = 0
)

@Composable
fun HomeScreen(
  navController: NavController,
  repository: TmdbRepository,
  contentDetailViewModel: ContentDetailViewModel,
  innersPadding: PaddingValues,
) {
  val categories = listOf(
    Category(
      "Popular", listOf(
        Subcategory("Movies", { page -> repository.getPopularMovies(page) }),
        Subcategory("Shows", { page -> repository.getPopularShows(page) })
      )
    ), Category(
      "Top Rated", listOf(
        Subcategory("Movies", { page -> repository.getTopRatedMovies(page) }),
        Subcategory("Shows", { page -> repository.getTopRatedShows(page) })
      )
    ), Category(
      "Streaming Now", listOf(
        Subcategory("Netflix", { page -> repository.getOnStreamingNetflix(page) }),
        Subcategory("Prime Video", { page -> repository.getOnStreamingPrimeVideo(page) }),
        Subcategory("Apple TV", { page -> repository.getOnStreamingAppleTV(page) }),
        Subcategory("Disney+", { page -> repository.getOnStreamingDisneyPlus(page) }),
      )
    )
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(state = rememberScrollState())
      .padding(innersPadding)
  ) {
    categories.forEach { entry ->
      SubCategory(
        entry.name,
        entry.subcategories,
        navController,
        repository = repository,
        contentDetailViewModel = contentDetailViewModel
      )
    }
  }
}


@Composable
fun SubCategory(
  title: String,
  subcategories: List<Subcategory>,
  navController: NavController,
  repository: TmdbRepository,
  contentDetailViewModel: ContentDetailViewModel
) {
  val subcategoriesMap = subcategories.associateBy { it.type }
  var selectedSubcategoryType by rememberSaveable { mutableStateOf(subcategories.first().type) }
  val selectedSubcategory = subcategoriesMap[selectedSubcategoryType]
  val itemsState = remember { mutableStateOf<List<TmdbMediaListItem>>(emptyList()) }
  LaunchedEffect(selectedSubcategoryType) {
    itemsState.value = emptyList()
    selectedSubcategory?.data =
      selectedSubcategory?.data?.copy(currentPage = 0, items = emptyList()) ?: PagingData()

    selectedSubcategory?.loadMore()
    itemsState.value = selectedSubcategory?.data?.items ?: emptyList()
  }
  Column(modifier = Modifier.padding(8.dp)) {

    Text( // Category title
      text = title,
      style = MaterialTheme.typography.headlineSmall,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(bottom = 4.dp)
    )
    LazyRow(
      modifier = Modifier
        .padding(bottom = 8.dp)
        .height(24.dp)
    ) {
      items(subcategories) { item ->
        FilterChip(
          modifier = Modifier,
          onClick = { selectedSubcategoryType = item.type },
          leadingIcon = {},
          label = {
            if (selectedSubcategoryType == item.type) {
              Icon(Icons.Default.Check, contentDescription = "Check", Modifier.size(16.dp))
              Spacer(Modifier.width(8.dp))
            }
            Text(item.type)
          },
          selected = selectedSubcategoryType == item.type
        )
        Spacer(modifier = Modifier.width(8.dp))
      }
    }

    selectedSubcategory?.let { subcategory ->
      Box(Modifier.clip(RoundedCornerShape(8.dp))) {
        EndlessLazyRow(
          modifier = Modifier,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          items = itemsState.value,
          itemContent = {
            MediaCard(
              it,
              navController = navController,
              contentDetailViewModel = contentDetailViewModel,
              repository = repository
            )
          },
          loadMore = {
            subcategory.loadMore()
            itemsState.value = subcategory.data.items
          })
      }
    }
  }
}

internal fun LazyListState.isNearBottom(buffer: Int = 1): Boolean {
  val lastVisibleItem = this.layoutInfo.visibleItemsInfo.lastOrNull()
  return lastVisibleItem != null && lastVisibleItem.index >= this.layoutInfo.totalItemsCount - buffer
}

@Composable
internal fun <T> EndlessLazyRow(
  modifier: Modifier = Modifier,
  horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
  listState: LazyListState = rememberLazyListState(),
  items: List<T>,
  itemContent: @Composable (T) -> Unit,
  loadingItem: @Composable () -> Unit = { LoadingCard() },
  loadMore: suspend () -> Unit
) {
//    val isReachingBottom by remember { derivedStateOf { listState.reachedBottom(1) } } // Increased buffer
  val isNearBottom by remember { derivedStateOf { listState.isNearBottom(10) } }
  LaunchedEffect(isNearBottom) {
    loadMore()
  }

  LazyRow(modifier = modifier, state = listState, horizontalArrangement = horizontalArrangement) {
    items(items) { item ->
      itemContent(item)
    }
    item { loadingItem() }
  }
}


@Composable
fun LoadingCard() {
  Box(
    modifier = Modifier
      .width(160.dp)
      .padding(end = 12.dp)
      .aspectRatio(2 / 3f)
  ) {
    CircularProgressIndicator(
      modifier = Modifier
        .width(64.dp)
        .align(Alignment.Center),
    )
  }
}