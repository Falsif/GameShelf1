@file:OptIn(ExperimentalMaterial3Api::class)

package com.lunexa.gameshelf.ui.home

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lunexa.gameshelf.data.model.GameStatus
import com.lunexa.gameshelf.ui.AppViewModelProvider
import com.lunexa.gameshelf.ui.common.formatDate
import com.lunexa.gameshelf.ui.common.formatElapsed
import com.lunexa.gameshelf.ui.common.rememberNow
import com.lunexa.gameshelf.ui.components.Avatar
import com.lunexa.gameshelf.ui.components.EmptyState
import com.lunexa.gameshelf.ui.components.GameCover
import com.lunexa.gameshelf.ui.components.GameListRow
import com.lunexa.gameshelf.ui.components.LogoMark
import com.lunexa.gameshelf.ui.components.RatingBadge
import com.lunexa.gameshelf.ui.components.SectionCard
import com.lunexa.gameshelf.ui.components.SectionHeader
import com.lunexa.gameshelf.ui.components.StatCard

@Composable
fun HomeScreen(
    onOpenGame: (Long) -> Unit,
    onAddGame: () -> Unit,
    onSearch: (String) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenWishlist: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddGame,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Добавить игру") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp)
            )
        }
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // --- Шапка ---
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LogoMark()
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("GameShelf", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Привет, ${state.profile.nickname}!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Outlined.Settings, "Настройки") }
                    Avatar(
                        state.profile.avatarPath, state.profile.nickname, 38.dp,
                        Modifier.clickable(onClick = onOpenProfile)
                    )
                }
            }
            // --- Поиск ---
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Поиск по библиотеке…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch(query.trim()) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            // --- Активный таймер ---
            state.timer?.let { timer ->
                item { TimerBanner(timer, onClick = { onOpenGame(timer.game.id) }) }
            }
            // --- Сводка ---
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("${state.total}", "Всего игр", Icons.Filled.Inventory2, MaterialTheme.colorScheme.primary,
                            Modifier.weight(1f), onClick = onOpenLibrary)
                        StatCard("${state.completed}", "Пройдено", Icons.Filled.CheckCircle, Color(GameStatus.COMPLETED.color),
                            Modifier.weight(1f), onClick = onOpenLibrary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("${state.inProgress}", "В процессе", Icons.Filled.PlayCircle, Color(GameStatus.PLAYING.color),
                            Modifier.weight(1f), onClick = onOpenLibrary)
                        StatCard("${state.wishlist}", "Желаемое", Icons.Filled.ShoppingCart, Color(GameStatus.WANT.color),
                            Modifier.weight(1f), onClick = onOpenWishlist)
                    }
                }
            }

            if (state.total == 0) {
                item {
                    SectionCard {
                        EmptyState(
                            icon = Icons.Filled.AutoStories,
                            title = "Ваша полка пока пуста",
                            text = "Добавьте первую игру вручную или найдите её в онлайн-каталоге.",
                            actionText = "Добавить игру",
                            onAction = onAddGame
                        )
                    }
                }
            } else {
                // --- Моя коллекция ---
                item {
                    SectionHeader("Моя коллекция", action = "Все", onAction = onOpenLibrary)
                    Spacer(Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.collection, key = { it.game.id }) { item ->
                            Column(
                                Modifier
                                    .width(118.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onOpenGame(item.game.id) }
                            ) {
                                Box {
                                    GameCover(item.game.coverUri, item.game.title, Modifier.width(118.dp).height(158.dp), corner = 16.dp)
                                    RatingBadge(item.game.rating, Modifier.align(Alignment.TopEnd).padding(6.dp))
                                    Box(
                                        Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(8.dp)
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(GameStatus.from(item.game.status).color))
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    item.game.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                // --- Недавно добавленные ---
                item { SectionHeader("Недавно добавленные") }
                items(state.recent, key = { "recent_${it.game.id}" }) { item ->
                    GameListRow(item, onClick = { onOpenGame(item.game.id) })
                }
                // --- Достижения ---
                item {
                    SectionHeader("Последние достижения")
                    Spacer(Modifier.height(8.dp))
                    AchievementsBlock(state, onOpenGame)
                }
            }
        }
    }
}

@Composable
private fun TimerBanner(timer: ActiveTimer, onClick: () -> Unit) {
    val now = rememberNow()
    val accent = MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.6f))))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Timer, null, tint = Color.White)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Идёт игровая сессия", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
            Text(timer.game.title, color = Color.White, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(formatElapsed(now - timer.start), color = Color.White, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun AchievementsBlock(state: HomeUiState, onOpenGame: (Long) -> Unit) {
    if (state.achievements.isEmpty() && state.recentCompletions.isEmpty()) {
        SectionCard {
            Text(
                "Пройдите первую игру — здесь появятся ваши достижения.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AnimatedVisibility(state.achievements.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.achievements) { a ->
                    val (icon, color) = when (a.kind) {
                        AchievementKind.COLLECTION -> Icons.Filled.Inventory2 to MaterialTheme.colorScheme.primary
                        AchievementKind.COMPLETION -> Icons.Filled.EmojiEvents to Color(0xFFFBBF24)
                        AchievementKind.TIME -> Icons.Filled.Schedule to Color(0xFF34D399)
                        AchievementKind.RATING -> Icons.Filled.Star to Color(0xFFF59E0B)
                        AchievementKind.FAVORITE -> Icons.Filled.Favorite to Color(0xFFF472B6)
                    }
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(12.dp)
                            .width(210.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(icon, null, tint = color) }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(a.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                a.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
        state.recentCompletions.forEach { rc ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { onOpenGame(rc.game.id) }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GameCover(rc.game.coverUri, rc.game.title, Modifier.size(44.dp), corner = 10.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Пройдено: ${rc.game.title}", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(formatDate(rc.completion.completedAt), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Filled.EmojiEvents, null, tint = Color(0xFFFBBF24))
            }
        }
    }
}
