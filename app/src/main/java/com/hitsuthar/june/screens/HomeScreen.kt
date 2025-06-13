package com.hitsuthar.june.screens

import android.util.Log
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.helper.widget.Carousel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import app.moviebase.tmdb.model.TmdbMediaListItem
import com.hitsuthar.june.components.MediaCard
import com.hitsuthar.june.utils.TmdbRepository
import com.hitsuthar.june.viewModels.ContentDetailViewModel
import com.hitsuthar.june.viewModels.HomeScreenViewModel
import kotlinx.coroutines.delay

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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
  navController: NavController,
  repository: TmdbRepository,
  homeScreenViewModel: HomeScreenViewModel,
  contentDetailViewModel: ContentDetailViewModel,
  innersPadding: PaddingValues,
) {
  val categories by homeScreenViewModel.categories.collectAsState()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(state = rememberScrollState())
      .padding(innersPadding)
  ) {
    Log.d("HomeScreen", categories.toString())

    if (homeScreenViewModel.subcategoryPagingData.isEmpty()) {
      Log.d("HomeScreen", categories.toString())
      Column(modifier = Modifier.fillMaxSize()) {
        var openDnsInfoDialog by remember { mutableStateOf(false) }
        LoadingIndicator(
          Modifier
            .size(64.dp)
            .align(Alignment.CenterHorizontally)
        ) // Show loading if categories are being fetched initially
        LaunchedEffect(Unit) {
          delay(5000)
          openDnsInfoDialog = true
        }

        if (openDnsInfoDialog) {
          AlertDialog(
            onDismissRequest = { openDnsInfoDialog = false },
            title = { Text("DNS Suggestion") },
            text = { Text("If you're experiencing persistent loading issues, consider changing your DNS to 'one.one.one.one' or 'dns.adguard.com'.") },
            confirmButton = {
              TextButton(onClick = { openDnsInfoDialog = false }) {
                Text("OK")
              }
            }
          )
        }
      }
    } else {
      categories.forEach { category ->
        // Get the selected subcategory type for this category from the ViewModel
        val selectedSubcategoryType = homeScreenViewModel.selectedSubcategoryTypes[category.name]
          ?: category.subcategories.firstOrNull()?.type // Default to first if not yet set

        // Find the actual Subcategory object
        val selectedSubcategory = category.subcategories.find { it.type == selectedSubcategoryType }

        // Get the PagingData for the selected subcategory
        val pagingData = selectedSubcategory?.let {
          homeScreenViewModel.subcategoryPagingData[homeScreenViewModel.subcategoryKey(
            category, it
          )]
        } ?: PagingData() // Default to empty if not found

        val isLoadingMore = selectedSubcategory?.let {
          homeScreenViewModel.isLoadingMore[homeScreenViewModel.subcategoryKey(category, it)]
        } ?: false
        SubCategory(
          category = category,
          selectedSubcategoryType = selectedSubcategoryType ?: "",
          currentItems = pagingData.items,
          isLoadingMore = isLoadingMore,
          onSubcategorySelected = { newSubType ->
            category.subcategories.find { it.type == newSubType }?.let { newSelected ->
              homeScreenViewModel.onSubcategorySelected(category, newSelected)
            }
          },
          onLoadMore = {
            selectedSubcategory?.let { // Ensure selectedSubcategory is not null
              homeScreenViewModel.loadMoreItems(category, it)
            }
          },
          navController = navController,
          contentDetailViewModel = contentDetailViewModel,
          repository = repository // MediaCard might still need it, or pass data via ContentDetailViewModel
        )

      }
    }
  }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubCategory(
  category: Category,
  selectedSubcategoryType: String,
  currentItems: List<TmdbMediaListItem>,
  isLoadingMore: Boolean, // To show loading indicator in EndlessLazyRow
  onSubcategorySelected: (String) -> Unit,
  onLoadMore: () -> Unit,
  navController: NavController,
  repository: TmdbRepository, // Pass through if MediaCard needs it
  contentDetailViewModel: ContentDetailViewModel
) {

  Column(modifier = Modifier.padding(8.dp)) {

    Text( // Category title
      text = category.name,
      style = MaterialTheme.typography.headlineSmall,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(bottom = 4.dp)
    )
    LazyRow(
      modifier = Modifier
        .padding(bottom = 8.dp)
        .height(24.dp)
    ) {
      items(category.subcategories) { item ->
        FilterChip(
          modifier = Modifier,
          onClick = { onSubcategorySelected(item.type) },
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

    // Only show EndlessLazyRow if there's a selected subcategory and items OR it's loading
    if (selectedSubcategoryType.isNotEmpty()) {
      Box(Modifier.clip(RoundedCornerShape(8.dp))) { // Consider if clipping is needed here or in EndlessLazyRow
        EndlessLazyRow(
          items = currentItems, isLoading = isLoadingMore, // Pass loading state
          // endOfListReached = pagingData.endOfListReached, // If you implement this
          itemContent = { item, itemModifier -> // itemModifier from EndlessLazyRow
            MediaCard(
              content = item, // Assuming MediaCard takes TmdbMediaListItem
              modifier = itemModifier,
              navController = navController,
              contentDetailViewModel = contentDetailViewModel,
              repository = repository,
              showBackDrop = true,
            )
          }, loadMore = onLoadMore
        )
      }
    } else if (isLoadingMore) { // Show loading if no items but it's trying to load
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(200.dp), contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator()
      }
    }
  }
}

internal fun LazyListState.isNearBottom(buffer: Int = 1): Boolean {
  val lastVisibleItem = this.layoutInfo.visibleItemsInfo.lastOrNull()
  return lastVisibleItem != null && lastVisibleItem.index >= this.layoutInfo.totalItemsCount - buffer
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> EndlessLazyRow(
  horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
  listState: LazyListState = rememberLazyListState(),
  items: List<T>,
  itemContent: @Composable (T, Modifier) -> Unit,
  loadingItem: @Composable () -> Unit = { LoadingCard() },
  isLoading: Boolean,
  loadMoreThreshold: Int = 5, // How many items from end to trigger loadMore
  loadMore: suspend () -> Unit
) {

  val shouldLoadMore by remember {
    derivedStateOf {
      val layoutInfo = listState.layoutInfo
      val totalItems = layoutInfo.totalItemsCount
      if (totalItems == 0 || isLoading /*|| endOfListReached*/) return@derivedStateOf false

      val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
      lastVisibleItemIndex >= totalItems - 1 - loadMoreThreshold
    }
  }
  LaunchedEffect(shouldLoadMore) {
    if (shouldLoadMore && !isLoading /*&& !endOfListReached*/) {
      loadMore()
    }
  }

//    val isReachingBottom by remember { derivedStateOf { listState.reachedBottom(1) } } // Increased buffer
//  val isNearBottom by remember { derivedStateOf { listState.isNearBottom(10) } }
//  LaunchedEffect(isNearBottom) {
//    loadMore()
//  }

  if (items.isEmpty()) {
    Log.d("HomeScreen", "Items are empty")
    loadingItem()
  } else HorizontalMultiBrowseCarousel(
    state = rememberCarouselState { items.count() },
    preferredItemWidth = 384.dp,
    modifier = Modifier
      .fillMaxWidth()
      .wrapContentHeight()
      .padding(top = 16.dp, bottom = 16.dp),
    itemSpacing = 8.dp,
//    contentPadding = PaddingValues(horizontal = 16.dp)
  ) { i ->
    itemContent(items[i], Modifier.maskClip(MaterialTheme.shapes.medium))

  }
//  LazyRow(modifier = modifier, state = listState, horizontalArrangement = horizontalArrangement) {
//    items(items) { item ->
//      itemContent(item)
//    }
//    item { loadingItem() }
//  }
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