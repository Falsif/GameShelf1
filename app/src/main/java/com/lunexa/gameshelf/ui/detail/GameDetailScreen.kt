@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.lunexa.gameshelf.ui.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lunexa.gameshelf.data.model.Game
import com.lunexa.gameshelf.data.model.GameStatus
import com.lunexa.gameshelf.data.model.PlaySession
import com.lunexa.gameshelf.data.model.genreList
import com.lunexa.gameshelf.data.model.platformList
import com.lunexa.gameshelf.data.model.statusEnum
import com.lunexa.gameshelf.ui.AppViewModelProvider
import com.lunexa.gameshelf.ui.common.formatDate
import com.lunexa.gameshelf.ui.common.formatDateTime
import com.lunexa.gameshelf.ui.common.formatElapsed
import com.lunexa.gameshelf.ui.common.formatMinutes
import com.lunexa.gameshelf.ui.common.formatPrice
import com.lunexa.gameshelf.ui.common.rememberNow
import com.lunexa.gameshelf.ui.components.ConfirmDialog
import com.lunexa.gameshelf.ui.components.DateField
import com.lunexa.gameshelf.ui.components.EmptyState
import com.lunexa.gameshelf.ui.components.GameCover
import com.lunexa.gameshelf.ui.components.SectionCard
import com.lunexa.gameshelf.ui.components.StatusChip
import com.lunexa.gameshelf.ui.components.imageModel

@Composable
fun GameDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: GameDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    var confirmDelete by remember { mutableStateOf(false) }
    var statusDialog by remember { mutableStateOf(false) }
    var ratingDialog by remember { mutableStateOf(false) }
    var notesDialog by remember { mutableStateOf(false) }
    var sessionDialog by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<PlaySession?>(null) }
    var replayConfirm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar, Modifier.navigationBarsPadding()) }
    ) { padding ->
        val game = state.game
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            game == null -> Column(Modifier.fillMaxSize().statusBarsPadding()) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
                EmptyState(Icons.Outlined.SearchOff, "Игра не найдена", "Возможно, она была удалена.", actionText = "Назад", onAction = onBack)
            }
            else -> Box(Modifier.fillMaxSize().padding(padding)) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp)
                ) {
                    Header(game, state.completions.size)
                    Column(
                        Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ActionsRow(
                            game = game,
                            onStatus = { statusDialog = true },
                            onRating = { ratingDialog = true },
                            onReplay = { replayConfirm = true }
                        )
                        TimeCard(
                            state = state,
                            onStart = viewModel::startTimer,
                            onStop = viewModel::stopTimer,
                            onCancel = viewModel::cancelTimer,
                            onAddSession = { sessionDialog = true },
                            onDeleteSession = { sessionToDelete = it }
                        )
                        if (game.inWishlist) WishCard(game, viewModel::togglePurchased)
                        InfoCard(game, state.completions.size)
                        if (game.description.isNotBlank()) DescriptionCard(game.description)
                        NotesCard(game.notes, onEdit = { notesDialog = true })
                        if (state.completions.isNotEmpty()) {
                            SectionCard {
                                Text("История прохождений", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(8.dp))
                                state.completions.sortedByDescending { it.completedAt }.forEachIndexed { i, c ->
                                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.EmojiEvents, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Прохождение №${state.completions.size - i}", Modifier.weight(1f))
                                        Text(formatDate(c.completedAt), color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = { confirmDelete = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text("Удалить игру", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                TopButtons(
                    game = game,
                    onBack = onBack,
                    onEdit = { onEdit(game.id) },
                    onFavorite = viewModel::toggleFavorite,
                    onWishlist = viewModel::toggleWishlist,
                    onDelete = { confirmDelete = true }
                )
            }
        }
    }

    val game = state.game
    if (game != null) {
        if (confirmDelete) {
            ConfirmDialog(
                title = "Удалить игру?",
                text = "«${game.title}» будет удалена вместе с историей сессий и прохождений. Это действие нельзя отменить.",
                confirmText = "Удалить",
                destructive = true,
                onConfirm = { confirmDelete = false; viewModel.delete(onBack) },
                onDismiss = { confirmDelete = false }
            )
        }
        if (replayConfirm) {
            ConfirmDialog(
                title = "Начать заново?",
                text = "Статус сменится на «Перепрохожу», дата начала обновится. История прошлых прохождений сохранится.",
                confirmText = "Начать",
                onConfirm = { replayConfirm = false; viewModel.startReplay() },
                onDismiss = { replayConfirm = false }
            )
        }
        if (statusDialog) {
            StatusDialog(game.statusEnum, onSelect = { statusDialog = false; viewModel.setStatus(it) }, onDismiss = { statusDialog = false })
        }
        if (ratingDialog) {
            RatingDialog(game.rating, onSave = { ratingDialog = false; viewModel.setRating(it) }, onDismiss = { ratingDialog = false })
        }
        if (notesDialog) {
            NotesDialog(game.notes, onSave = { notesDialog = false; viewModel.saveNotes(it) }, onDismiss = { notesDialog = false })
        }
        if (sessionDialog) {
            AddSessionDialog(
                onSave = { minutes, date, note -> sessionDialog = false; viewModel.addSession(minutes, date, note) },
                onDismiss = { sessionDialog = false }
            )
        }
        sessionToDelete?.let { s ->
            ConfirmDialog(
                title = "Удалить сессию?",
                text = "${formatMinutes(s.durationMinutes)} от ${formatDate(s.startedAt)} будут вычтены из игрового времени.",
                confirmText = "Удалить",
                destructive = true,
                onConfirm = { sessionToDelete = null; viewModel.deleteSession(s) },
                onDismiss = { sessionToDelete = null }
            )
        }
    }
}

@Composable
private fun Header(game: Game, completions: Int) {
    val bg = MaterialTheme.colorScheme.background
    Box(Modifier.fillMaxWidth().height(380.dp)) {
        if (game.coverUri != null) {
            AsyncImage(
                model = imageModel(game.coverUri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.45f
            )
        } else {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)))
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(bg.copy(alpha = 0.35f), bg.copy(alpha = 0.75f), bg))
            )
        )
        Row(
            Modifier.align(Alignment.BottomStart).padding(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            GameCover(
                game.coverUri, game.title,
                Modifier.width(132.dp).height(178.dp).shadow(12.dp, RoundedCornerShape(16.dp)),
                corner = 16.dp
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(game.title, style = MaterialTheme.typography.headlineSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                val meta = listOfNotNull(
                    game.developer.takeIf { it.isNotBlank() },
                    game.releaseDate?.let { formatDate(it).takeLast(4) }
                ).joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(meta, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(8.dp))
                StatusChip(game.statusEnum)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (game.rating > 0) {
                        Icon(Icons.Filled.Star, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(20.dp))
                        Text(" ${game.rating}/10", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(12.dp))
                    }
                    if (completions > 0) {
                        Icon(Icons.Filled.EmojiEvents, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                        Text(" ×$completions", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopButtons(
    game: Game,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onFavorite: () -> Unit,
    onWishlist: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    val btnColors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.35f), contentColor = Color.White)
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, colors = btnColors) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onFavorite, colors = btnColors) {
            Icon(
                if (game.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                if (game.isFavorite) "Убрать из избранного" else "В избранное",
                tint = if (game.isFavorite) Color(0xFFF472B6) else Color.White
            )
        }
        Spacer(Modifier.width(6.dp))
        IconButton(onClick = onEdit, colors = btnColors) { Icon(Icons.Filled.Edit, "Редактировать") }
        Spacer(Modifier.width(6.dp))
        Box {
            IconButton(onClick = { menu = true }, colors = btnColors) { Icon(Icons.Filled.MoreVert, "Ещё") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(if (game.inWishlist) "Убрать из желаемого" else "В список желаемого") },
                    leadingIcon = { Icon(Icons.Filled.ShoppingCart, null) },
                    onClick = { menu = false; onWishlist() }
                )
                DropdownMenuItem(
                    text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { menu = false; onDelete() }
                )
            }
        }
    }
}

@Composable
private fun ActionsRow(game: Game, onStatus: () -> Unit, onRating: () -> Unit, onReplay: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(onClick = onStatus, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Filled.SwapHoriz, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Статус", maxLines = 1)
        }
        FilledTonalButton(onClick = onRating, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
            Icon(if (game.rating > 0) Icons.Filled.Star else Icons.Filled.StarBorder, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (game.rating > 0) "${game.rating}/10" else "Оценить", maxLines = 1)
        }
        FilledTonalButton(onClick = onReplay, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Filled.Replay, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Заново", maxLines = 1)
        }
    }
}

@Composable
private fun TimeCard(
    state: DetailUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onCancel: () -> Unit,
    onAddSession: () -> Unit,
    onDeleteSession: (PlaySession) -> Unit
) {
    val game = state.game ?: return
    var showAll by remember { mutableStateOf(false) }
    SectionCard(Modifier.animateContentSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Игровое время", style = MaterialTheme.typography.titleMedium)
                Text(
                    formatMinutes(state.totalMinutes),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Вручную: ${formatMinutes(game.manualMinutes)} · сессии: ${formatMinutes(state.sessionMinutes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        val start = state.timerStart
        if (start != null) {
            val now = rememberNow()
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                Spacer(Modifier.width(8.dp))
                Text(formatElapsed(now - start), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onCancel) { Icon(Icons.Outlined.Close, "Сбросить таймер") }
                Button(onClick = onStop, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.Stop, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Стоп")
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStart, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Таймер", maxLines = 1)
                }
                OutlinedButton(onClick = onAddSession, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Сессия", maxLines = 1)
                }
            }
            if (state.otherTimerRunning) {
                Text(
                    "Сейчас идёт таймер другой игры — при запуске он будет остановлен и сохранён.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        if (state.sessions.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))
            Text("История сессий (${state.sessions.size})", style = MaterialTheme.typography.titleSmall)
            val list = if (showAll) state.sessions else state.sessions.take(5)
            list.forEach { s ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(formatMinutes(s.durationMinutes), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            formatDateTime(s.startedAt) + if (s.note.isNotBlank()) " · ${s.note}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { onDeleteSession(s) }) {
                        Icon(Icons.Outlined.Delete, "Удалить сессию", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (state.sessions.size > 5) {
                TextButton(onClick = { showAll = !showAll }) {
                    Text(if (showAll) "Свернуть" else "Показать все")
                }
            }
            if (start == null) {
                TextButton(onClick = onAddSession) { Text("+ Добавить сессию вручную") }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(130.dp))
        Text(value, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun InfoCard(game: Game, completions: Int) {
    SectionCard {
        Text("Информация", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        InfoRow("Жанр", game.genreList.joinToString(", ").ifEmpty { "—" })
        InfoRow("Разработчик", game.developer.ifBlank { "—" })
        InfoRow("Издатель", game.publisher.ifBlank { "—" })
        InfoRow("Дата выпуска", formatDate(game.releaseDate))
        InfoRow("Начало игры", formatDate(game.startDate))
        InfoRow("Завершение", formatDate(game.finishDate))
        InfoRow("Прохождений", "$completions")
        InfoRow("Добавлена", formatDate(game.addedAt))
        if (game.platformList.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text("Платформы", color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                game.platformList.forEach { SuggestionChip(onClick = {}, label = { Text(it) }) }
            }
        }
    }
}

@Composable
private fun DescriptionCard(text: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    SectionCard(Modifier.animateContentSize()) {
        Text("Описание", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (expanded) Int.MAX_VALUE else 5,
            overflow = TextOverflow.Ellipsis
        )
        if (text.length > 240) {
            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Свернуть" else "Читать полностью") }
        }
    }
}

@Composable
private fun NotesCard(notes: String, onEdit: () -> Unit) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Личные заметки", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onEdit) { Icon(Icons.Outlined.EditNote, "Редактировать заметки") }
        }
        Text(
            notes.ifBlank { "Здесь можно записать впечатления, секреты или прогресс." },
            style = MaterialTheme.typography.bodyMedium,
            color = if (notes.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.clickable(onClick = onEdit)
        )
    }
}

@Composable
private fun WishCard(game: Game, onTogglePurchased: () -> Unit) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.ShoppingCart, null, tint = Color(GameStatus.WANT.color))
            Spacer(Modifier.width(8.dp))
            Text("Список желаемого", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(4.dp))
        InfoRow("Желаемая цена", formatPrice(game.wishPrice))
        InfoRow("Дата покупки", formatDate(game.wishBuyDate))
        InfoRow("Приоритет", priorityTitle(game.wishPriority))
        InfoRow("Статус", if (game.wishPurchased) "Куплено ✓" else "Не куплено")
        Spacer(Modifier.height(6.dp))
        if (game.wishPurchased) {
            OutlinedButton(onClick = onTogglePurchased, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Снять отметку «Куплено»")
            }
        } else {
            Button(onClick = onTogglePurchased, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Отметить как купленную")
            }
        }
    }
}

fun priorityTitle(p: Int) = when (p) {
    3 -> "Высокий"
    1 -> "Низкий"
    else -> "Средний"
}

@Composable
private fun StatusDialog(current: GameStatus, onSelect: (GameStatus) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Статус прохождения") },
        text = {
            Column {
                GameStatus.entries.forEach { st ->
                    val selected = st == current
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) Color(st.color).copy(alpha = 0.16f) else Color.Transparent)
                            .clickable { onSelect(st) }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(Color(st.color)))
                        Spacer(Modifier.width(12.dp))
                        Text(st.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        if (selected) Text("текущий", style = MaterialTheme.typography.labelSmall, color = Color(st.color))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    )
}

@Composable
private fun RatingDialog(current: Int, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableIntStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ваша оценка") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (value == 0) "Без оценки" else "$value / 10",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    (1..10).forEach { i ->
                        Icon(
                            if (i <= value) Icons.Filled.Star else Icons.Filled.StarBorder,
                            "$i",
                            tint = if (i <= value) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp).clickable { value = i }
                        )
                    }
                }
                Slider(
                    value = value.toFloat(),
                    onValueChange = { value = it.toInt() },
                    valueRange = 0f..10f,
                    steps = 9
                )
            }
        },
        confirmButton = { Button(onClick = { onSave(value) }) { Text("Сохранить") } },
        dismissButton = {
            Row {
                if (current > 0) TextButton(onClick = { onSave(0) }) { Text("Сбросить") }
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    )
}

@Composable
private fun NotesDialog(current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Личные заметки") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 5000) text = it },
                minLines = 5,
                maxLines = 12,
                shape = RoundedCornerShape(14.dp),
                supportingText = { Text("${text.length}/5000") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { Button(onClick = { onSave(text) }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    )
}

@Composable
private fun AddSessionDialog(onSave: (Int, Long, String) -> Unit, onDismiss: () -> Unit) {
    var hours by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf<Long?>(System.currentTimeMillis()) }
    var note by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая игровая сессия") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hours,
                        onValueChange = { v -> if (v.all { it.isDigit() } && v.length <= 4) hours = v },
                        label = { Text("Часы") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = { v -> if (v.all { it.isDigit() } && v.length <= 2) minutes = v },
                        label = { Text("Минуты") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
                DateField("Дата", date, { date = it })
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 200) note = it },
                    label = { Text("Комментарий (необязательно)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val h = hours.toIntOrNull() ?: 0
                val m = minutes.toIntOrNull() ?: 0
                when {
                    m > 59 -> error = "Минут должно быть от 0 до 59"
                    h * 60 + m <= 0 -> error = "Укажите длительность сессии"
                    date == null -> error = "Укажите дату"
                    else -> onSave(h * 60 + m, date!!, note)
                }
            }) { Text("Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    )
}
