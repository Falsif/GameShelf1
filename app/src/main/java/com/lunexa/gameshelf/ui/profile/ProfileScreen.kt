@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.lunexa.gameshelf.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lunexa.gameshelf.data.model.GameStatus
import com.lunexa.gameshelf.ui.AppViewModelProvider
import com.lunexa.gameshelf.ui.common.GenrePresets
import com.lunexa.gameshelf.ui.common.PlatformPresets
import com.lunexa.gameshelf.ui.common.RuLocale
import com.lunexa.gameshelf.ui.common.formatMinutes
import com.lunexa.gameshelf.ui.common.gamesCount
import com.lunexa.gameshelf.ui.components.Avatar
import com.lunexa.gameshelf.ui.components.GameCover
import com.lunexa.gameshelf.ui.components.SectionCard
import com.lunexa.gameshelf.ui.components.StatCard

@Composable
fun ProfileScreen(
    onOpenSettings: () -> Unit,
    onOpenGame: (Long) -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var editOpen by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.setAvatar(uri)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Профиль") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    IconButton(onClick = { editOpen = true }) { Icon(Icons.Outlined.Edit, "Редактировать профиль") }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Outlined.Settings, "Настройки") }
                }
            )
        }
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Карточка профиля
            SectionCard {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box {
                        Avatar(
                            state.profile.avatarPath, state.profile.nickname, 104.dp,
                            Modifier.clickable {
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        )
                        Box(
                            Modifier
                                .align(Alignment.BottomEnd)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.CameraAlt, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(state.profile.nickname, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${gamesCount(state.games)} в коллекции",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (state.profile.avatarPath != null) {
                        TextButton(onClick = viewModel::removeAvatar) { Text("Убрать аватар") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        InfoPill("Любимая платформа", state.profile.favoritePlatform.ifBlank { state.topPlatform ?: "—" }, Modifier.weight(1f))
                        InfoPill("Любимый жанр", state.profile.favoriteGenre.ifBlank { state.topGenre ?: "—" }, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { editOpen = true }, shape = RoundedCornerShape(12.dp)) {
                        Text("Редактировать профиль")
                    }
                }
            }

            Text("Статистика профиля", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("${state.games}", "Игр в коллекции", Icons.Filled.Inventory2, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                StatCard("${state.completions}", "Прохождений", Icons.Filled.EmojiEvents, Color(0xFFFBBF24), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("${state.completedGames}", "Пройдено игр", Icons.Filled.CheckCircle, Color(GameStatus.COMPLETED.color), Modifier.weight(1f))
                StatCard("${state.playing}", "Играю сейчас", Icons.Filled.PlayCircle, Color(GameStatus.PLAYING.color), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(formatMinutes(state.totalMinutes), "Наиграно", Icons.Filled.Schedule, Color(0xFF34D399), Modifier.weight(1f))
                StatCard(
                    state.avgRating?.let { String.format(RuLocale, "%.1f", it) } ?: "—",
                    "Средняя оценка", Icons.Filled.Star, Color(0xFFF59E0B), Modifier.weight(1f)
                )
            }
            StatCard("${state.favorites}", "Игр в избранном", Icons.Filled.Favorite, Color(0xFFF472B6), Modifier.fillMaxWidth())

            state.longest?.let { item ->
                SectionCard {
                    Text("Самая долгая игра", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onOpenGame(item.game.id) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GameCover(item.game.coverUri, item.game.title, Modifier.width(54.dp).height(72.dp), corner = 10.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.game.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(formatMinutes(item.totalMinutes), color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (editOpen) {
        EditProfileDialog(
            nickname = state.profile.nickname,
            platform = state.profile.favoritePlatform,
            genre = state.profile.favoriteGenre,
            platformSuggestions = (state.knownPlatforms + PlatformPresets).distinct().take(10),
            genreSuggestions = (state.knownGenres + GenrePresets).distinct().take(10),
            onSave = { n, p, g -> editOpen = false; viewModel.saveProfile(n, p, g) },
            onDismiss = { editOpen = false }
        )
    }
}

@Composable
private fun InfoPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun EditProfileDialog(
    nickname: String,
    platform: String,
    genre: String,
    platformSuggestions: List<String>,
    genreSuggestions: List<String>,
    onSave: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var n by rememberSaveable { mutableStateOf(nickname) }
    var p by rememberSaveable { mutableStateOf(platform) }
    var g by rememberSaveable { mutableStateOf(genre) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Профиль") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = n, onValueChange = { if (it.length <= 30) n = it },
                    label = { Text("Никнейм") }, singleLine = true,
                    isError = n.isBlank(),
                    supportingText = if (n.isBlank()) { { Text("Введите никнейм") } } else null,
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = p, onValueChange = { if (it.length <= 40) p = it },
                    label = { Text("Любимая платформа") }, singleLine = true,
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    platformSuggestions.forEach { s -> SuggestionChip(onClick = { p = s }, label = { Text(s) }) }
                }
                OutlinedTextField(
                    value = g, onValueChange = { if (it.length <= 40) g = it },
                    label = { Text("Любимый жанр") }, singleLine = true,
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    genreSuggestions.forEach { s -> SuggestionChip(onClick = { g = s }, label = { Text(s) }) }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(n, p, g) }, enabled = n.isNotBlank()) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    )
}
