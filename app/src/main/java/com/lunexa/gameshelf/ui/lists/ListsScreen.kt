@file:OptIn(ExperimentalMaterial3Api::class)

package com.lunexa.gameshelf.ui.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lunexa.gameshelf.data.model.GameItem
import com.lunexa.gameshelf.ui.AppViewModelProvider
import com.lunexa.gameshelf.ui.common.formatPrice
import com.lunexa.gameshelf.ui.common.formatShortDate
import com.lunexa.gameshelf.ui.components.ConfirmDialog
import com.lunexa.gameshelf.ui.components.EmptyState
import com.lunexa.gameshelf.ui.components.GameCover
import com.lunexa.gameshelf.ui.components.GameGridCard
import com.lunexa.gameshelf.ui.detail.priorityTitle

@Composable
fun ListsScreen(
    onOpenGame: (Long) -> Unit,
    onAddWish: () -> Unit,
    viewModel: ListsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(viewModel.initialTab.coerceIn(0, 1)) }
    var sortMenu by remember { mutableStateOf(false) }
    var toRemove by remember { mutableStateOf<GameItem?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Мои списки") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                    actions = {
                        if (tab == 1) {
                            Box {
                                IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Filled.Sort, "Сортировка") }
                                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                                    WishSort.entries.forEach { s ->
                                        DropdownMenuItem(
                                            text = { Text(s.title) },
                                            onClick = { viewModel.setSort(s); sortMenu = false },
                                            trailingIcon = { if (state.sort == s) Icon(Icons.Filled.Check, null) }
                                        )
                                    }
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Показывать купленные") },
                                        onClick = { viewModel.toggleShowPurchased(); sortMenu = false },
                                        trailingIcon = { if (state.showPurchased) Icon(Icons.Filled.Check, null) }
                                    )
                                }
                            }
                        }
                    }
                )
                TabRow(
                    selectedTabIndex = tab,
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    Tab(selected = tab == 0, onClick = { tab = 0 },
                        text = { Text("Избранное · ${state.favorites.size}") },
                        icon = { Icon(Icons.Filled.Favorite, null) })
                    Tab(selected = tab == 1, onClick = { tab = 1 },
                        text = { Text("Желаемое · ${state.wishlist.count { !it.game.wishPurchased }}") },
                        icon = { Icon(Icons.Filled.ShoppingCart, null) })
                }
            }
        },
        floatingActionButton = {
            if (tab == 1) {
                FloatingActionButton(
                    onClick = onAddWish,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp)
                ) { Icon(Icons.Filled.Add, "Добавить в желаемое") }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                tab == 0 -> FavoritesTab(state.favorites, onOpenGame)
                else -> WishlistTab(
                    state = state,
                    onOpenGame = onOpenGame,
                    onTogglePurchased = { viewModel.togglePurchased(it.game) },
                    onRemove = { toRemove = it },
                    onAdd = onAddWish
                )
            }
        }
    }

    toRemove?.let { item ->
        ConfirmDialog(
            title = "Убрать из желаемого?",
            text = "«${item.game.title}» останется в библиотеке, но пропадёт из списка желаемого.",
            confirmText = "Убрать",
            onConfirm = { viewModel.removeFromWishlist(item.game); toRemove = null },
            onDismiss = { toRemove = null }
        )
    }
}

@Composable
private fun FavoritesTab(items: List<GameItem>, onOpenGame: (Long) -> Unit) {
    if (items.isEmpty()) {
        EmptyState(
            Icons.Filled.FavoriteBorder,
            "Избранное пусто",
            "Отмечайте любимые игры сердечком на странице игры — они появятся здесь."
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items, key = { it.game.id }) { item ->
            GameGridCard(item, onClick = { onOpenGame(item.game.id) }, modifier = Modifier.animateItem())
        }
    }
}

@Composable
private fun WishlistTab(
    state: ListsUiState,
    onOpenGame: (Long) -> Unit,
    onTogglePurchased: (GameItem) -> Unit,
    onRemove: (GameItem) -> Unit,
    onAdd: () -> Unit
) {
    if (state.wishlist.isEmpty()) {
        EmptyState(
            Icons.Filled.ShoppingCart,
            if (state.purchasedCount > 0 && !state.showPurchased) "Всё куплено!" else "Список желаемого пуст",
            if (state.purchasedCount > 0 && !state.showPurchased) "Купленные игры скрыты — включите их показ в меню сортировки."
            else "Добавляйте игры, которые хотите купить или пройти, с ценой и приоритетом.",
            actionText = "Добавить игру",
            onAction = onAdd
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Сумма желаемого", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatPrice(state.wishTotalPrice), style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    "Куплено: ${state.purchasedCount}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                state.sort.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(state.wishlist, key = { it.game.id }) { item ->
            WishRow(item, onOpenGame, onTogglePurchased, onRemove, Modifier.animateItem())
        }
    }
}

@Composable
private fun WishRow(
    item: GameItem,
    onOpenGame: (Long) -> Unit,
    onTogglePurchased: (GameItem) -> Unit,
    onRemove: (GameItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val g = item.game
    var menu by remember { mutableStateOf(false) }
    val prColor = when (g.wishPriority) {
        3 -> Color(0xFFF87171)
        1 -> Color(0xFF60A5FA)
        else -> Color(0xFFFBBF24)
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { onOpenGame(g.id) }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = g.wishPurchased, onCheckedChange = { onTogglePurchased(item) })
        GameCover(g.coverUri, g.title, Modifier.width(52.dp).aspectRatio(3f / 4f), corner = 10.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                g.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (g.wishPurchased) TextDecoration.LineThrough else null,
                color = if (g.wishPurchased) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(prColor.copy(alpha = 0.18f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(priorityTitle(g.wishPriority), color = prColor, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.width(8.dp))
                Text(formatPrice(g.wishPrice), style = MaterialTheme.typography.labelLarge)
            }
            if (g.wishBuyDate != null) {
                Text(
                    "Покупка: ${formatShortDate(g.wishBuyDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Действия") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(if (g.wishPurchased) "Снять «куплено»" else "Отметить купленной") },
                    onClick = { menu = false; onTogglePurchased(item) }
                )
                DropdownMenuItem(text = { Text("Убрать из списка") }, onClick = { menu = false; onRemove(item) })
            }
        }
    }
}
