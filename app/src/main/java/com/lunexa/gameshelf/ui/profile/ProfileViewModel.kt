package com.lunexa.gameshelf.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunexa.gameshelf.data.GameRepository
import com.lunexa.gameshelf.data.ImageStore
import com.lunexa.gameshelf.data.Profile
import com.lunexa.gameshelf.data.SettingsRepository
import com.lunexa.gameshelf.data.model.GameItem
import com.lunexa.gameshelf.data.model.GameStatus
import com.lunexa.gameshelf.data.model.genreList
import com.lunexa.gameshelf.data.model.platformList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val loading: Boolean = true,
    val profile: Profile = Profile(),
    val games: Int = 0,
    val completedGames: Int = 0,
    val completions: Int = 0,
    val playing: Int = 0,
    val favorites: Int = 0,
    val totalMinutes: Int = 0,
    val avgRating: Double? = null,
    val topPlatform: String? = null,
    val topGenre: String? = null,
    val longest: GameItem? = null,
    val knownPlatforms: List<String> = emptyList(),
    val knownGenres: List<String> = emptyList()
)

class ProfileViewModel(
    repository: GameRepository,
    private val settings: SettingsRepository,
    private val images: ImageStore
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val state: StateFlow<ProfileUiState> = combine(
        repository.gameItems, repository.completions, settings.profile
    ) { items, completions, profile ->
        val games = items.map { it.game }
        val rated = games.filter { it.rating > 0 }
        ProfileUiState(
            loading = false,
            profile = profile,
            games = games.size,
            completedGames = games.count { it.status == GameStatus.COMPLETED.name },
            completions = completions.size,
            playing = games.count { it.status == GameStatus.PLAYING.name || it.status == GameStatus.REPLAYING.name },
            favorites = games.count { it.isFavorite },
            totalMinutes = items.sumOf { it.totalMinutes },
            avgRating = if (rated.isEmpty()) null else rated.map { it.rating }.average(),
            topPlatform = games.flatMap { it.platformList }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key,
            topGenre = games.flatMap { it.genreList }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key,
            longest = items.filter { it.totalMinutes > 0 }.maxByOrNull { it.totalMinutes },
            knownPlatforms = games.flatMap { it.platformList }.distinct(),
            knownGenres = games.flatMap { it.genreList }.distinct()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState())

    fun saveProfile(nickname: String, platform: String, genre: String) {
        viewModelScope.launch {
            val p = settings.currentProfile()
            settings.saveProfile(
                p.copy(
                    nickname = nickname.trim().take(30).ifBlank { "Игрок" },
                    favoritePlatform = platform.trim().take(40),
                    favoriteGenre = genre.trim().take(40)
                )
            )
            _message.value = "Профиль сохранён"
        }
    }

    fun setAvatar(uri: Uri) {
        viewModelScope.launch {
            val path = images.importFromUri(uri, prefix = "avatar")
            if (path == null) {
                _message.value = "Не удалось загрузить изображение"
                return@launch
            }
            val p = settings.currentProfile()
            images.delete(p.avatarPath)
            settings.saveProfile(p.copy(avatarPath = path))
        }
    }

    fun removeAvatar() {
        viewModelScope.launch {
            val p = settings.currentProfile()
            images.delete(p.avatarPath)
            settings.saveProfile(p.copy(avatarPath = null))
        }
    }

    fun consumeMessage() { _message.value = null }
}
