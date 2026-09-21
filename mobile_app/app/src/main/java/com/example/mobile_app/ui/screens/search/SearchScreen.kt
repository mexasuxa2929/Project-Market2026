package com.example.mobile_app.ui.screens.search

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.mobile_app.data.model.Product
import com.example.mobile_app.ui.components.ProductCard
import com.example.mobile_app.ui.theme.Background
import com.example.mobile_app.ui.theme.Primary
import com.example.mobile_app.ui.theme.TextSecondary
import com.example.mobile_app.ui.viewmodel.SearchViewModel
import com.example.mobile_app.util.tr

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
    favoritedIds: Set<String> = emptySet(),
    onFavoriteToggle: (String, Boolean) -> Unit = { _, _ -> },
    onAddToCart: ((Product) -> Unit)? = null,
    searchViewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory)
) {
    val results      = searchViewModel.results.collectAsLazyPagingItems()
    val query        by searchViewModel.query.collectAsState()
    val recent       by searchViewModel.recentSearches.collectAsState()

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val trendingChips = listOf(
        "iPhone", tr("search_trending_1"), tr("search_trending_2"),
        tr("search_trending_3"), tr("search_trending_4"), "Samsung"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // ── Top Search Bar ────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = tr("search_back"),
                        tint = Color(0xFF1F2937)
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Background)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = tr("search_placeholder"),
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = searchViewModel::onQueryChange,
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                color = Color(0xFF1F2937)
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { searchViewModel.search(query) }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    }
                }

                IconButton(onClick = { /* mic action */ }) {
                    Icon(imageVector = Icons.Default.Mic,
                        contentDescription = tr("search_voice"),
                        tint = Primary
                    )
                }
            }
        }

        when {
            // ── So'rov kiritilmagan: so'nggi qidiruvlar + trendlar ─────────
            query.isBlank() -> SuggestionList(
                query = query,
                recent = recent,
                trendingChips = trendingChips,
                onClearAll = searchViewModel::clearRecent,
                onRemoveRecent = searchViewModel::removeRecent,
                onQueryClick = searchViewModel::search
            )

            // ── Natijalar (Paging 3, offline kesh bilan) ──────────────────
            else -> {
                val refresh = results.loadState.refresh
                when {
                    refresh is LoadState.Loading && results.itemCount == 0 -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Primary, strokeWidth = 2.dp)
                            Spacer(Modifier.height(12.dp))
                            Text(tr("search_loading"), fontSize = 13.sp, color = TextSecondary)
                        }
                    }

                    refresh is LoadState.Error && results.itemCount == 0 -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                (refresh.error as? Throwable)?.message ?: tr("catalog_error"),
                                color = Color(0xFFEF4444), fontSize = 14.sp
                            )
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = { results.retry() },
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(2.dp, Primary)
                            ) { Text(tr("catalog_retry"), color = Primary) }
                        }
                    }

                    results.itemCount == 0 -> EmptyResults()

                    else -> ResultGrid(
                        items = results,
                        onProductClick = onProductClick,
                        favoritedIds = favoritedIds,
                        onFavoriteToggle = onFavoriteToggle,
                        onAddToCart = onAddToCart
                    )
                }
            }
        }
    }
}

// ─── Suggestion list (idle) ──────────────────────────────────────────────────

@Composable
private fun SuggestionList(
    query: String,
    recent: List<String>,
    trendingChips: List<String>,
    onClearAll: () -> Unit,
    onRemoveRecent: (Int) -> Unit,
    onQueryClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Recent Searches
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("search_recent_title"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    if (recent.isNotEmpty()) {
                        Text(
                            text = tr("search_clear_all"),
                            fontSize = 13.sp,
                            color = Primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { onClearAll() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (recent.isEmpty()) {
                    Text(
                        text = tr("search_history_empty"),
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                recent.forEachIndexed { index, search ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onQueryClick(search) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = search,
                            fontSize = 14.sp,
                            color = Color(0xFF374151),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onRemoveRecent(index) }
                        )
                    }
                    if (index < recent.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFE5E7EB))
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Trending chips
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = tr("search_trending_title"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    trendingChips.forEach { chip ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Primary.copy(alpha = 0.12f),
                                            Color(0xFF818CF8).copy(alpha = 0.12f)
                                        )
                                    )
                                )
                                .clickable { onQueryClick(chip) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = chip,
                                fontSize = 13.sp,
                                color = Primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Result grid ─────────────────────────────────────────────────────────────

@Composable
private fun ResultGrid(
    items: LazyPagingItems<Product>,
    onProductClick: (String) -> Unit,
    favoritedIds: Set<String> = emptySet(),
    onFavoriteToggle: (String, Boolean) -> Unit = { _, _ -> },
    onAddToCart: ((Product) -> Unit)? = null
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            count = items.itemCount,
            key = { index -> items[index]?.id ?: index },
            contentType = { "product" }
        ) { index ->
            val product = items[index] ?: return@items
            val clickModifier = remember(product) {
                Modifier.fillMaxWidth().clickable { onProductClick(product.id) }
            }
            val toggleFavorite = remember(product, favoritedIds) {
                { onFavoriteToggle(product.id, product.id in favoritedIds) }
            }
            ProductCard(
                product = product,
                modifier = clickModifier,
                isGrid = true,
                isFavorited = product.id in favoritedIds,
                onFavoriteToggle = toggleFavorite,
                onAddToCart = onAddToCart
            )
        }

        when (val append = items.loadState.append) {
            is LoadState.Loading -> item(key = "load_more_loading", span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                    CircularProgressIndicator(color = Primary, strokeWidth = 2.dp)
                }
                Spacer(Modifier.height(16.dp))
            }
            is LoadState.Error -> item(key = "load_more_error", span = { GridItemSpan(maxLineSpan) }) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick  = { items.retry() },
                    shape    = RoundedCornerShape(12.dp),
                    border   = androidx.compose.foundation.BorderStroke(2.dp, Primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                ) {
                    Text(
                        tr("catalog_retry"),
                        color      = Primary,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp,
                        modifier   = Modifier.padding(vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
            else -> Unit
        }
    }
}

// ─── Empty results ───────────────────────────────────────────────────────────

@Composable
private fun EmptyResults() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🔍", fontSize = 44.sp)
        Spacer(Modifier.height(16.dp))
        Text(tr("search_no_results_title"), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
        Spacer(Modifier.height(6.dp))
        Text(tr("search_no_results_subtitle"), fontSize = 13.sp, color = TextSecondary)
    }
}