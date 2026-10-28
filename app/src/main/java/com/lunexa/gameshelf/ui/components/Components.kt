@file:OptIn(ExperimentalMaterial3Api::class)

package com.lunexa.gameshelf.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lunexa.gameshelf.data.model.GameItem
import com.lunexa.gameshelf.data.model.GameStatus
import com.lunexa.gameshelf.data.model.genreList
import com.lunexa.gameshelf.data.model.platformList
import com.lunexa.gameshelf.data.model.statusEnum
import com.lunexa.gameshelf.ui.common.formatHoursShort
import com.lunexa.gameshelf.ui.common.formatShortDate
import com.lunexa.gameshelf.ui.common.localToPicker
import com.lunexa.gameshelf.ui.common.pickerToLocal
import java.io.File

/** Модель для Coil: локальный путь превращаем в File. */
fun imageModel(path: String?): Any? = when {
    path == null -> null
    path.startsWith("/") -> File(path)
    else -> path
}

/** Обложка игры с красивой заглушкой, если картинки нет или она не загрузилась. */
@Composable
fun GameCover(
    path: String?,
    title: String,
    modifier: Modifier = Modifier,
    corner: Dp = 14.dp
) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier
            .clip(RoundedCornerShape(corner))
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.55f), MaterialTheme.colorScheme.surfaceContainerHighest)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.Filled.SportsEsports, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                title.take(24),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (path != null) {
            AsyncImage(
                model = imageModel(path),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun StatusChip(status: GameStatus, modifier: Modifier = Modifier, compact: Boolean = false) {
    val color = Color(status.color)
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 3.dp else 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(
            status.title,
            color = color,
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
fun RatingBadge(rating: Int, modifier: Modifier = Modifier) {
    if (rating <= 0) return
    Row(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Star, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(3.dp))
        Text("$rating", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/** Карточка игры для сетки. */
@Composable
fun GameGridCard(item: GameItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val game = item.game
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box {
            GameCover(
                game.coverUri, game.title,
                Modifier.fillMaxWidth().aspectRatio(3f / 4f),
                corner = 18.dp
            )
            RatingBadge(game.rating, Modifier.align(Alignment.TopEnd).padding(8.dp))
            if (game.isFavorite) {
                Icon(
                    Icons.Filled.Favorite, "Избранное",
                    tint = Color(0xFFF472B6),
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp).size(20.dp)
                )
            }
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(game.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val meta = listOfNotNull(game.platformList.firstOrNull(), game.genreList.firstOrNull()).joinToString(" · ")
            Text(
                meta.ifEmpty { "Без платформы" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            StatusChip(game.statusEnum, compact = true)
        }
    }
}

/** Строка игры для списка. */
@Composable
fun GameListRow(item: GameItem, onClick: () -> Unit, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    val game = item.game
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            GameCover(game.coverUri, game.title, Modifier.width(62.dp).aspectRatio(3f / 4f), corner = 12.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        game.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (game.isFavorite) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.Favorite, null, tint = Color(0xFFF472B6), modifier = Modifier.size(14.dp))
                    }
                }
                val meta = listOfNotNull(
                    game.platformList.firstOrNull(),
                    game.genreList.firstOrNull()
                ).joinToString(" · ")
                Text(
                    meta.ifEmpty { "Без платформы" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(game.statusEnum, compact = true)
                    if (game.rating > 0) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.Star, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                        Text(" ${game.rating}/10", style = MaterialTheme.typography.labelMedium)
                    }
                    if (item.totalMinutes > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            formatHoursShort(item.totalMinutes),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (trailing != null) trailing()
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(18.dp))
            Button(onClick = onAction, shape = RoundedCornerShape(14.dp)) { Text(actionText) }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    dismissText: String = "Отмена"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (destructive) ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ) else ButtonDefaults.buttonColors()
            ) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissText) } },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    )
}

/** Карточка с числом для сводок. */
@Composable
fun StatCard(
    value: String,
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val base = modifier
        .shadow(4.dp, RoundedCornerShape(18.dp), clip = false)
        .clip(RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surfaceContainer)
    Column(
        (if (onClick != null) base.clickable(onClick = onClick) else base).padding(14.dp)
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.height(10.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, maxLines = 1)
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Карточка-контейнер для блоков на экранах. */
@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
fun Avatar(path: String?, nickname: String, size: Dp, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.5f))))
            .border(2.dp, accent.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val letter = nickname.trim().firstOrNull()?.uppercaseChar()
        if (letter != null) {
            Text(letter.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp)
        } else {
            Icon(Icons.Filled.Person, null, tint = Color.White)
        }
        if (path != null) {
            AsyncImage(
                model = imageModel(path),
                contentDescription = "Аватар",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** Логотип GameShelf. */
@Composable
fun LogoMark(size: Dp = 34.dp) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3.4f))
            .background(Brush.linearGradient(listOf(accent, MaterialTheme.colorScheme.secondary))),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.SportsEsports, null, tint = Color.White, modifier = Modifier.size(size * 0.6f))
    }
}

/** Поле даты с выбором через календарь. */
@Composable
fun DateField(
    label: String,
    value: Long?,
    onChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supporting: String? = null
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = if (value != null) formatShortDate(value) else "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text("Не указана") },
            isError = isError,
            supportingText = supporting?.let { { Text(it) } },
            trailingIcon = {
                if (value != null) {
                    IconButton(onClick = { onChange(null) }) { Icon(Icons.Outlined.Close, "Очистить") }
                } else {
                    Icon(Icons.Outlined.CalendarMonth, null)
                }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        // Прозрачный слой перехватывает нажатие (кроме кнопки очистки справа)
        Box(
            Modifier
                .matchParentSize()
                .padding(end = if (value != null) 52.dp else 0.dp)
                .clickable { open = true }
        )
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = value?.let(::localToPicker))
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(pickerToLocal(it)) }
                    open = false
                }) { Text("Готово") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Отмена") } }
        ) {
            DatePicker(state = state, title = { Text(label, Modifier.padding(start = 24.dp, top = 16.dp)) })
        }
    }
}
