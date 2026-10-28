@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.lunexa.gameshelf.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lunexa.gameshelf.data.model.GameStatus
import com.lunexa.gameshelf.ui.AppViewModelProvider
import com.lunexa.gameshelf.ui.common.gamesCount
import com.lunexa.gameshelf.ui.components.EmptyState
import com.lunexa.gameshelf.ui.components.GameGridCard
import com.lunexa.gameshelf.ui.components.GameListRow

@Composable
fun LibraryScreen(
    onOpenGame: (Long) -> Unit,
    onAddGame: () -> Unit,
    viewModel: LibraryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sortMenu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Библиотека") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    IconButton(onClick = { viewModel.setGrid(!state.grid) }) {
                        Icon(
                            if (state.grid) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                            if (state.grid) "Показать списком" else "Показать сеткой"
                        )
                    }
                    Box {
                        IconButton(onClick = { sortMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, "Сортировка")
                        }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            Text(
                                "Сортировка",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SortOption.entries.forEach { opt ->
                                val selected = state.filter.sort == opt
                                DropdownMenuItem(
                                    text = { Text(opt.title) },
                                    onClick = { viewModel.setSort(opt) },
                                    leadingIcon = {
                                        if (selected) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
                                        else Spacer(Modifier.size(24.dp))
                                    },
                                    trailingIcon = {
                                        if (selected) Icon(
                                            if (state.filter.descending) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                                            if (state.filter.descending) "По убыванию" else "По возрастанию"
                                        )
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(text = { Text("Готово") }, onClick = { sortMenu = false })
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddGame,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp)
            ) { Icon(Icons.Filled.Add, "Добавить игру") }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Поиск
            OutlinedTextField(
                value = state.filter.query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text("Название игры") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = {
                    if (state.filter.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) { Icon(Icons.Outlined.Close, "Очистить") }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            Spacer(Modifier.size(10.dp))
            // Фильтры по статусу
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.filter.status == null,
                    onClick = { viewModel.setStatus(null) },
                    label = { Text("Все · ${state.totalInLibrary}") },
                    shape = RoundedCornerShape(12.dp)
                )
                GameStatus.entries.forEach { st ->
                    val color = Color(st.color)
                    FilterChip(
                        selected = state.filter.status == st,
                        onClick = { viewModel.setStatus(if (state.filter.status == st) null else st) },
                        label = { Text("${st.title} · ${state.statusCounts[st] ?: 0}") },
                        leadingIcon = { Box(Modifier.size(8.dp).clip(CircleShape).background(color)) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.22f),
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
            Spacer(Modifier.size(6.dp))
            // Платформа, жанр, избранное
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DropdownFilter("Платформа", state.filter.platform, state.platforms, viewModel::setPlatform)
                DropdownFilter("Жанр", state.filter.genre, state.genres, viewModel::setGenre)
                FilterChip(
                    selected = state.filter.favoritesOnly,
                    onClick = viewModel::toggleFavorites,
                    label = { Text("Избранное") },
                    leadingIcon = { Icon(Icons.Filled.Favorite, null, Modifier.size(16.dp)) },
                    shape = RoundedCornerShape(12.dp)
                )
                if (state.filter.isActive) {
                    FilterChip(
                        selected = false,
                        onClick = viewModel::resetFilters,
                        label = { Text("Сбросить") },
                        leadingIcon = { Icon(Icons.Outlined.Close, null, Modifier.size(16.dp)) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
            Text(
                "Найдено: ${gamesCount(state.items.size)} · ${state.filter.sort.title.lowercase()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.totalInLibrary == 0 -> EmptyState(
                    icon = Icons.Filled.VideoLibrary,
                    title = "Библиотека пуста",
                    text = "Добавьте свои игры, чтобы отслеживать прохождение, время и оценки.",
                    actionText = "Добавить игру",
                    onAction = onAddGame
                )
                state.items.isEmpty() -> EmptyState(
                    icon = Icons.Filled.SearchOff,
                    title = "Ничего не найдено",
                    text = "Попробуйте изменить запрос или фильтры.",
                    actionText = "Сбросить фильтры",
                    onAction = viewModel::resetFilters
                )
                state.grid -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.items, key = { it.game.id }) { item ->
                        GameGridCard(item, onClick = { onOpenGame(item.game.id) }, modifier = Modifier.animateItem())
                    }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.items, key = { it.game.id }) { item ->
                        GameListRow(item, onClick = { onOpenGame(item.game.id) }, modifier = Modifier.animateItem())
                    }
                }
            }
        }
    }
}

@Composable
private fun DropdownFilter(
    label: String,
    selected: String?,
    options: List<String>,
    onSelect: (String?) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected != null,
            onClick = { open = true },
            label = { Text(selected ?: label) },
            trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, null, Modifier.size(18.dp)) },
            shape = RoundedCornerShape(12.dp)
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Все") }, onClick = { onSelect(null); open = false })
            if (options.isEmpty()) {
                DropdownMenuItem(text = { Text("Нет вариантов") }, onClick = { open = false }, enabled = false)
            }
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = { onSelect(opt); open = false },
                    trailingIcon = { if (opt == selected) Icon(Icons.Filled.Check, null) }
                )
            }
        }
    }
    Spacer(Modifier.width(0.dp))
}
