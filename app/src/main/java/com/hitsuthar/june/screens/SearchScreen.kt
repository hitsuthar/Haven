package com.hitsuthar.june.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.moviebase.tmdb.model.TmdbMediaListItem
import com.hitsuthar.june.components.MediaCard
import com.hitsuthar.june.utils.TmdbRepository
import com.hitsuthar.june.viewModels.ContentDetailViewModel

@SuppressLint("UnrememberedMutableInteractionSource")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    repository: TmdbRepository,
    contentDetailViewModel: ContentDetailViewModel
) {
    data class SearchState(
        val query: String = "", val results: List<TmdbMediaListItem> = emptyList()
    )

    var searchState by remember { mutableStateOf(SearchState()) }
    val debouncedSearchQuery by remember { derivedStateOf { searchState.query } }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchResult by remember { mutableStateOf<List<TmdbMediaListItem>>(emptyList()) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(searchQuery) {
        searchResult = repository.search(searchQuery).sortedByDescending { it.popularity }
    }
    Scaffold(topBar = {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchQuery,
                    onQueryChange = { updatedQuery: String -> searchQuery = updatedQuery },
                    placeholder = { Text("Search") },
                    onSearch = { expanded = false },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                )
            },
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
        ) { }
//        CenterAlignedTopAppBar(title = {
//            LaunchedEffect(Unit) { focusRequester.requestFocus() }
//            BasicTextField(
//                value = searchQuery,
//                onValueChange = { updatedQuery: String -> searchQuery = updatedQuery },
//                singleLine = true,
//                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
//                textStyle = TextStyle(
//                    color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 16.sp
//                ),
//                decorationBox = { innerTextField ->
//                    TextFieldDefaults.DecorationBox(
//                        value = searchQuery,
//                        innerTextField = innerTextField,
//                        enabled = true,
//                        singleLine = true,
//                        visualTransformation = VisualTransformation.None,
//                        interactionSource = MutableInteractionSource(),
//                        placeholder = { Text("Search") },
//                        trailingIcon = {
//                            IconButton(onClick = {
//                                if (searchQuery.isNotEmpty()) {
//                                    searchQuery = ""
//                                } else {
//                                    navController.popBackStack()
//                                }
//                            }) {
//                                Icon(Icons.Default.Clear, contentDescription = "Clear")
//                            }
//                        },
//                        contentPadding = PaddingValues(horizontal = 16.dp),
//                        container = {
//                            TextFieldDefaults.Container(
//                                enabled = true,
//                                isError = false,
//                                interactionSource = MutableInteractionSource(),
//                                modifier = Modifier,
//                                colors = TextFieldDefaults.colors(
////                                    focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
////                                    unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
////                                    cursorColor = MaterialTheme.colorScheme.onPrimaryContainer,
//                                    focusedIndicatorColor = Color.Transparent,
//                                    unfocusedIndicatorColor = Color.Transparent
//                                ),
//                                shape = RoundedCornerShape(8.dp),
//                                focusedIndicatorLineThickness = 0.dp,
//                                unfocusedIndicatorLineThickness = 0.dp,
//                            )
//                        },
//
//                        )
//                },
//                modifier = Modifier
//                    .focusRequester(focusRequester)
//                    .height(48.dp)
//                    .fillMaxWidth()
//            )
//        })
    }, content = { paddingValue ->
        LazyVerticalGrid(
            modifier = Modifier.padding(paddingValue),
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
    })

}
