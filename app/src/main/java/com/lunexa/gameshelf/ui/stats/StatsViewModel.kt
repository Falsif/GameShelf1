package com.lunexa.gameshelf.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunexa.gameshelf.data.GameRepository
import com.lunexa.gameshelf.data.model.Completion
import com.lunexa.gameshelf.data.model.GameItem
import com.lunexa.gameshelf.data.model.GameStatus
import com.lunexa.gameshelf.data.model.PlaySession
import com.lunexa.gameshelf.data.model.genreList
import com.lunexa.gameshelf.data.model.platformList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.util.Calendar

data class StatsUiState(
    val loading: Boolean = true,
    val total: Int = 0,
    val completedNow: Int = 0,
    val completedPercent: Int = 0,
    val avgRating: Double? = null,
    val ratedCount: Int = 0,
    val totalMinutes: Int = 0,
    val statusCounts: List<Pair<GameStatus, Int>> = emptyList(),
    val genres: List<Pair<String, Int>> = emptyList(),
    val platforms: List<Pair<String, Int>> = emptyList(),
    val year: Int = Calendar.getInstance().get(Calendar.YEAR),
    val completionsByMonth: List<Int> = List(12) { 0 },
    val completionsThisMonth: Int = 0,
    val completionsThisYear: Int = 0,
    val completionsTotal: Int = 0,
    val playByMonth: List<Int> = List(12) { 0 },
    val topByTime: List<GameItem> = emptyList(),
    val topRated: List<GameItem> = emptyList(),
    val sessionsCount: Int = 0
)

class StatsViewModel(repository: GameRepository) : ViewModel() {

    private val selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))

    val state: StateFlow<StatsUiState> = combine(
        repository.gameItems,
        repository.completions,
        repository.sessions,
        selectedYear
    ) { items, completions, sessions, year ->
        build(items, completions, sessions, year)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    fun prevYear() = selectedYear.update { it - 1 }
    fun nextYear() = selectedYear.update { it + 1 }

    private fun build(
        items: List<GameItem>,
        completions: List<Completion>,
        sessions: List<PlaySession>,
        year: Int
    ): StatsUiState {
        val games = items.map { it.game }
        val total = games.size
        val completedNow = games.count { it.status == GameStatus.COMPLETED.name }
        val rated = games.filter { it.rating > 0 }
        val cal = Calendar.getInstance()
        val nowYear = cal.get(Calendar.YEAR)
        val nowMonth = cal.get(Calendar.MONTH)

        val byMonth = IntArray(12)
        var thisMonth = 0
        var thisYear = 0
        completions.forEach { c ->
            cal.timeInMillis = c.completedAt
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH)
            if (y == year) byMonth[m]++
            if (y == nowYear) {
                thisYear++
                if (m == nowMonth) thisMonth++
            }
        }

        val playMonth = IntArray(12)
        sessions.forEach { s ->
            cal.timeInMillis = s.startedAt
            if (cal.get(Calendar.YEAR) == year) playMonth[cal.get(Calendar.MONTH)] += s.durationMinutes
        }

        return StatsUiState(
            loading = false,
            total = total,
            completedNow = completedNow,
            completedPercent = if (total == 0) 0 else (completedNow * 100 / total),
            avgRating = if (rated.isEmpty()) null else rated.map { it.rating }.average(),
            ratedCount = rated.size,
            totalMinutes = items.sumOf { it.totalMinutes },
            statusCounts = GameStatus.entries.map { st -> st to games.count { it.status == st.name } },
            genres = games.flatMap { it.genreList }
                .groupingBy { it }.eachCount().toList().sortedByDescending { it.second }.take(6),
            platforms = games.flatMap { it.platformList }
                .groupingBy { it }.eachCount().toList().sortedByDescending { it.second }.take(6),
            year = year,
            completionsByMonth = byMonth.toList(),
            completionsThisMonth = thisMonth,
            completionsThisYear = thisYear,
            completionsTotal = completions.size,
            playByMonth = playMonth.toList(),
            topByTime = items.filter { it.totalMinutes > 0 }.sortedByDescending { it.totalMinutes }.take(5),
            topRated = items.filter { it.game.rating > 0 }.sortedByDescending { it.game.rating }.take(5),
            sessionsCount = sessions.size
        )
    }
}
