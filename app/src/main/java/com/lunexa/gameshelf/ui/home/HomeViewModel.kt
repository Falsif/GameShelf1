package com.lunexa.gameshelf.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunexa.gameshelf.data.AppSettings
import com.lunexa.gameshelf.data.GameRepository
import com.lunexa.gameshelf.data.Profile
import com.lunexa.gameshelf.data.SettingsRepository
import com.lunexa.gameshelf.data.model.Completion
import com.lunexa.gameshelf.data.model.Game
import com.lunexa.gameshelf.data.model.GameItem
import com.lunexa.gameshelf.data.model.GameStatus
import com.lunexa.gameshelf.ui.common.formatMinutes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class AchievementKind { COLLECTION, COMPLETION, TIME, RATING, FAVORITE }

data class Achievement(val title: String, val description: String, val kind: AchievementKind)

data class RecentCompletion(val completion: Completion, val game: Game)

data class ActiveTimer(val game: Game, val start: Long)

data class HomeUiState(
    val loading: Boolean = true,
    val total: Int = 0,
    val completed: Int = 0,
    val inProgress: Int = 0,
    val wishlist: Int = 0,
    val collection: List<GameItem> = emptyList(),
    val recent: List<GameItem> = emptyList(),
    val recentCompletions: List<RecentCompletion> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val profile: Profile = Profile(),
    val timer: ActiveTimer? = null
)

class HomeViewModel(
    repository: GameRepository,
    settings: SettingsRepository
) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        repository.gameItems,
        repository.completions,
        settings.profile,
        settings.settings
    ) { items, completions, profile, appSettings ->
        build(items, completions, profile, appSettings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    private fun build(
        items: List<GameItem>,
        completions: List<Completion>,
        profile: Profile,
        s: AppSettings
    ): HomeUiState {
        val games = items.map { it.game }
        val byId = games.associateBy { it.id }
        val completedCount = games.count { it.status == GameStatus.COMPLETED.name }
        val inProgress = games.count { it.status == GameStatus.PLAYING.name || it.status == GameStatus.REPLAYING.name }
        val wish = games.count { it.inWishlist && !it.wishPurchased }
        val totalMinutes = items.sumOf { it.totalMinutes }

        val recentCompletions = completions
            .sortedByDescending { it.completedAt }
            .mapNotNull { c -> byId[c.gameId]?.let { RecentCompletion(c, it) } }
            .take(5)

        val achievements = buildList {
            val n = games.size
            listOf(100, 50, 25, 10, 1).firstOrNull { n >= it }?.let { t ->
                add(
                    if (t == 1) Achievement("Начало коллекции", "Добавлена первая игра", AchievementKind.COLLECTION)
                    else Achievement("Коллекционер $t+", "В коллекции уже $n игр", AchievementKind.COLLECTION)
                )
            }
            val c = completions.size
            listOf(100, 50, 25, 10, 5, 1).firstOrNull { c >= it }?.let { t ->
                add(
                    if (t == 1) Achievement("Первое прохождение", "Пройдена первая игра", AchievementKind.COMPLETION)
                    else Achievement("Мастер прохождений", "Завершено прохождений: $c", AchievementKind.COMPLETION)
                )
            }
            val hours = totalMinutes / 60
            listOf(1000, 500, 100, 50, 10).firstOrNull { hours >= it }?.let { t ->
                add(Achievement("$t+ часов в играх", "Всего наиграно ${formatMinutes(totalMinutes)}", AchievementKind.TIME))
            }
            val rated = games.count { it.rating > 0 }
            if (rated >= 10) add(Achievement("Критик", "Оценено игр: $rated", AchievementKind.RATING))
            val fav = games.count { it.isFavorite }
            if (fav >= 5) add(Achievement("Любимчики", "Игр в избранном: $fav", AchievementKind.FAVORITE))
        }

        val timer = if (s.timerGameId != null && s.timerStart != null) {
            byId[s.timerGameId]?.let { ActiveTimer(it, s.timerStart) }
        } else null

        return HomeUiState(
            loading = false,
            total = games.size,
            completed = completedCount,
            inProgress = inProgress,
            wishlist = wish,
            collection = items.sortedByDescending { it.game.updatedAt }.take(12),
            recent = items.sortedByDescending { it.game.addedAt }.take(5),
            recentCompletions = recentCompletions,
            achievements = achievements,
            profile = profile,
            timer = timer
        )
    }
}
