package com.hitsuthar.june.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
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
            .verticalScroll(state = rememberScrollState()).padding(innersPadding)
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
                    leadingIcon = {

                    },
                    label = {
                        if (selectedSubcategoryType == item.type) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Check",
                                Modifier.size(16.dp)
                            )
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
                EndlessLazyRow(modifier = Modifier,
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
    listState: LazyListState = rememberLazyListState(),
    items: List<T>,
    itemContent: @Composable (T) -> Unit,
    loadingItem: @Composable () -> Unit = { LoadingCard() },
    loadMore: suspend () -> Unit,

    ) {
//    val isReachingBottom by remember { derivedStateOf { listState.reachedBottom(1) } } // Increased buffer
    val isNearBottom by remember { derivedStateOf { listState.isNearBottom(10) } }
    LaunchedEffect(isNearBottom) {
        loadMore()
    }

    LazyRow(modifier = modifier, state = listState) {
        items(items) { item ->
            itemContent(item)
        }
        item { loadingItem() }
    }
}


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
            .width(160.dp)
            .padding(end = 8.dp)
            .clickable {
                contentDetailViewModel.setContentDetail(
                    contentId = content.id,
                    contentType = when (content) {
                        is TmdbMovie -> "movie"
                        is TmdbShow -> "tv"
                    },
                    repository = repository
                )
                navController.navigate(
                    Screen.Detail.route
                )
            },
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier) {
            Image(
                painter = rememberAsyncImagePainter(POSTER_BASE_URL + posterPath),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2 / 3f) // Maintain aspect ratio
            )
            Box(
                Modifier
                    .fillMaxHeight()
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Row(
                    Modifier
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
        }
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