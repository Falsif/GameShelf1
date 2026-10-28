@file:OptIn(ExperimentalMaterial3Api::class)

package com.lunexa.gameshelf.ui.stats

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.lunexa.gameshelf.ui.common.RuLocale
import com.lunexa.gameshelf.ui.common.formatHoursShort
import com.lunexa.gameshelf.ui.common.formatMinutes
import com.lunexa.gameshelf.ui.components.EmptyState
import com.lunexa.gameshelf.ui.components.GameCover
import com.lunexa.gameshelf.ui.components.SectionCard
import com.lunexa.gameshelf.ui.components.StatCard
import java.util.Calendar

@Composable
fun StatsScreen(
    onOpenGame: (Long) -> Unit,
    viewModel: StatsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accent = MaterialTheme.colorScheme.primary
    val accent2 = MaterialTheme.colorScheme.secondary

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Статистика") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.total == 0 -> Box(Modifier.fillMaxSize().padding(padding)) {
                EmptyState(
                    Icons.Filled.BarChart,
                    "Пока нечего считать",
                    "Добавьте игры в библиотеку — здесь появятся диаграммы по жанрам, платформам, времени и прохождениям."
                )
            }
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Сводка
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard("${state.total}", "Всего игр", Icons.Filled.Inventory2, accent, Modifier.weight(1f))
                            StatCard("${state.completedPercent}%", "Пройдено", Icons.Filled.CheckCircle,
                                Color(GameStatus.COMPLETED.color), Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard(
                                state.avgRating?.let { String.format(RuLocale, "%.1f", it) } ?: "—",
                                "Средняя оценка", Icons.Filled.Star, Color(0xFFF59E0B), Modifier.weight(1f)
                            )
                            StatCard(formatHoursShort(state.totalMinutes), "Игровое время", Icons.Filled.Schedule,
                                Color(GameStatus.PLAYING.color), Modifier.weight(1f))
                        }
                    }
                }
                // Статусы
                item {
                    SectionCard {
                        Text("Статусы коллекции", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DonutChart(
                                slices = state.statusCounts.map { (st, n) -> Slice(st.title, n, Color(st.color)) },
                                centerValue = "${state.completedPercent}%",
                                centerLabel = "пройдено",
                                size = 140.dp
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                state.statusCounts.forEach { (st, n) -> LegendItem(Color(st.color), st.title, "$n") }
                            }
                        }
                    }
                }
                // Прохождения по месяцам
                item {
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Пройдено по месяцам", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = viewModel::prevYear) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Предыдущий год")
                            }
                            Text("${state.year}", style = MaterialTheme.typography.titleSmall)
                            IconButton(onClick = viewModel::nextYear) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Следующий год")
                            }
                        }
                        val cal = Calendar.getInstance()
                        MonthBars(
                            values = state.completionsByMonth,
                            color = Color(GameStatus.COMPLETED.color),
                            highlightMonth = if (state.year == cal.get(Calendar.YEAR)) cal.get(Calendar.MONTH) else null
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MiniStat("${state.completionsThisMonth}", "в этом месяце", Modifier.weight(1f))
                            MiniStat("${state.completionsThisYear}", "в этом году", Modifier.weight(1f))
                            MiniStat("${state.completionsTotal}", "всего", Modifier.weight(1f))
                        }
                    }
                }
                // Игровое время по месяцам
                if (state.sessionsCount > 0) {
                    item {
                        SectionCard {
                            Text("Время сессий за ${state.year} (часы)", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            MonthBars(values = state.playByMonth.map { it / 60 }, color = accent)
                            Text(
                                "Всего сессий: ${state.sessionsCount} · общее время ${formatMinutes(state.totalMinutes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
                // Жанры
                item {
                    SectionCard {
                        Text("Любимые жанры", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        if (state.genres.isEmpty()) NoData("Укажите жанры у игр")
                        else HorizontalBars(state.genres, accent)
                    }
                }
                // Платформы
                item {
                    SectionCard {
                        Text("Популярные платформы", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        if (state.platforms.isEmpty()) NoData("Укажите платформы у игр")
                        else HorizontalBars(state.platforms, accent2)
                    }
                }
                // Топ по времени
                item {
                    SectionCard {
                        Text("Больше всего часов", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        if (state.topByTime.isEmpty()) {
                            NoData("Добавьте игровое время или сессии")
                        } else {
                            val max = state.topByTime.first().totalMinutes.coerceAtLeast(1)
                            state.topByTime.forEachIndexed { i, item ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onOpenGame(item.game.id) }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${i + 1}", style = MaterialTheme.typography.titleMedium, color = accent,
                                        modifier = Modifier.width(22.dp))
                                    GameCover(item.game.coverUri, item.game.title, Modifier.size(40.dp), corner = 8.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(item.game.title, style = MaterialTheme.typography.titleSmall, maxLines = 1,
                                            overflow = TextOverflow.Ellipsis)
                                        Spacer(Modifier.height(4.dp))
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(50))
                                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                        ) {
                                            Box(
                                                Modifier
                                                    .fillMaxWidth(item.totalMinutes.toFloat() / max)
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(50))
                                                    .background(accent)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(formatHoursShort(item.totalMinutes), style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
                // Лучшие по оценке
                if (state.topRated.isNotEmpty()) {
                    item {
                        SectionCard {
                            Text("Лучшие по вашей оценке", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            state.topRated.forEach { item ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onOpenGame(item.game.id) }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    GameCover(item.game.coverUri, item.game.title, Modifier.size(40.dp), corner = 8.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Text(item.game.title, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Icon(Icons.Filled.Star, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                                    Text(" ${item.game.rating}", style = MaterialTheme.typography.titleSmall)
                                }
                            }
                            Text(
                                "Оценено игр: ${state.ratedCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NoData(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
