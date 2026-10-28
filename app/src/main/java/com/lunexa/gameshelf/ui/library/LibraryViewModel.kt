package com.lunexa.gameshelf.ui.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunexa.gameshelf.data.GameRepository
import com.lunexa.gameshelf.data.SettingsRepository
import com.lunexa.gameshelf.data.model.GameItem
import com.lunexa.gameshelf.data.model.GameStatus
import com.lunexa.gameshelf.data.model.genreList
import com.lunexa.gameshelf.data.model.platformList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SortOption(val title: String) {
    ADDED("Дата добавления"),
    TITLE("Название"),
    RATING("Оценка"),
    YEAR("Год выпуска"),
    TIME("Игровое время")
}

data class LibraryFilter(
    val query: String = "",
    val status: GameStatus? = null,
    val platform: String? = null,
    val genre: String? = null,
    val favoritesOnly: Boolean = false,
    val sort: SortOption = SortOption.ADDED,
    val descending: Boolean = true
) {
    val isActive: Boolean
        get() = query.isNotBlank() || status != null || platform != null || genre != null || favoritesOnly
}

data class LibraryUiState(
    val loading: Boolean = true,
    val totalInLibrary: Int = 0,
    val items: List<GameItem> = emptyList(),
    val platforms: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val statusCounts: Map<GameStatus, Int> = emptyMap(),
    val filter: LibraryFilter = LibraryFilter(),
    val grid: Boolean = true
)

class LibraryViewModel(
    savedState: SavedStateHandle,
    repository: GameRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    private val filter = MutableStateFlow(LibraryFilter(query = savedState.get<String>("q") ?: ""))

    val state: StateFlow<LibraryUiState> = combine(
        repository.gameItems, filter, settings.settings
    ) { items, f, s ->
        LibraryUiState(
            loading = false,
            totalInLibrary = items.size,
            items = applyFilter(items, f),
            platforms = items.flatMap { it.game.platformList }.distinct().sortedBy { it.lowercase() },
            genres = items.flatMap { it.game.genreList }.distinct().sortedBy { it.lowercase() },
            statusCounts = items.groupingBy { GameStatus.from(it.game.status) }.eachCount(),
            filter = f,
            grid = s.libraryGrid
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState())

    private fun applyFilter(items: List<GameItem>, f: LibraryFilter): List<GameItem> {
        val q = f.query.trim().lowercase()
        val filtered = items.filter { item ->
            val g = item.game
            (q.isEmpty() || g.title.lowercase().contains(q)) &&
                (f.status == null || g.status == f.status.name) &&
                (f.platform == null || g.platformList.any { it.equals(f.platform, ignoreCase = true) }) &&
                (f.genre == null || g.genreList.any { it.equals(f.genre, ignoreCase = true) }) &&
                (!f.favoritesOnly || g.isFavorite)
        }
        val sorted = when (f.sort) {
            SortOption.ADDED -> filtered.sortedBy { it.game.addedAt }
            SortOption.TITLE -> filtered.sortedBy { it.game.title.lowercase() }
            SortOption.RATING -> filtered.sortedBy { it.game.rating }
            SortOption.YEAR -> filtered.sortedBy { it.game.releaseDate ?: Long.MIN_VALUE }
            SortOption.TIME -> filtered.sortedBy { it.totalMinutes }
        }
        return if (f.descending) sorted.reversed() else sorted
    }

    fun setQuery(q: String) = filter.update { it.copy(query = q) }
    fun setStatus(s: GameStatus?) = filter.update { it.copy(status = s) }
    fun setPlatform(p: String?) = filter.update { it.copy(platform = p) }
    fun setGenre(g: String?) = filter.update { it.copy(genre = g) }
    fun toggleFavorites() = filter.update { it.copy(favoritesOnly = !it.favoritesOnly) }
    fun setSort(option: SortOption) = filter.update {
        if (it.sort == option) it.copy(descending = !it.descending)
        else it.copy(sort = option, descending = option != SortOption.TITLE)
    }
    fun resetFilters() = filter.update { LibraryFilter(sort = it.sort, descending = it.descending) }

    fun setGrid(grid: Boolean) {
        viewModelScope.launch { settings.setLibraryGrid(grid) }
    }
}
