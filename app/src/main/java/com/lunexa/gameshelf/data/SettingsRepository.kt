package com.lunexa.gameshelf.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lunexa.gameshelf.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accent: Long = 0xFF8B5CF6,
    val libraryGrid: Boolean = true,
    val rawgKey: String = "",
    val timerGameId: Long? = null,
    val timerStart: Long? = null
)

data class Profile(
    val nickname: String = "Игрок",
    val avatarPath: String? = null,
    val favoritePlatform: String = "",
    val favoriteGenre: String = ""
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val ACCENT = longPreferencesKey("accent")
        val GRID = booleanPreferencesKey("library_grid")
        val RAWG = stringPreferencesKey("rawg_key")
        val TIMER_GAME = longPreferencesKey("timer_game")
        val TIMER_START = longPreferencesKey("timer_start")
        val NICK = stringPreferencesKey("nickname")
        val AVATAR = stringPreferencesKey("avatar")
        val FAV_PLATFORM = stringPreferencesKey("fav_platform")
        val FAV_GENRE = stringPreferencesKey("fav_genre")
    }

    private val prefs: Flow<Preferences> = context.dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val settings: Flow<AppSettings> = prefs.map { p ->
        AppSettings(
            themeMode = ThemeMode.entries.firstOrNull { it.name == p[Keys.THEME] } ?: ThemeMode.DARK,
            accent = p[Keys.ACCENT] ?: 0xFF8B5CF6,
            libraryGrid = p[Keys.GRID] ?: true,
            rawgKey = p[Keys.RAWG] ?: "",
            timerGameId = p[Keys.TIMER_GAME],
            timerStart = p[Keys.TIMER_START]
        )
    }

    val profile: Flow<Profile> = prefs.map { p ->
        Profile(
            nickname = p[Keys.NICK] ?: "Игрок",
            avatarPath = p[Keys.AVATAR],
            favoritePlatform = p[Keys.FAV_PLATFORM] ?: "",
            favoriteGenre = p[Keys.FAV_GENRE] ?: ""
        )
    }

    suspend fun currentSettings(): AppSettings = settings.first()
    suspend fun currentProfile(): Profile = profile.first()

    suspend fun setTheme(mode: ThemeMode) = context.dataStore.edit { it[Keys.THEME] = mode.name }
    suspend fun setAccent(color: Long) = context.dataStore.edit { it[Keys.ACCENT] = color }
    suspend fun setLibraryGrid(grid: Boolean) = context.dataStore.edit { it[Keys.GRID] = grid }
    suspend fun setRawgKey(key: String) = context.dataStore.edit { it[Keys.RAWG] = key.trim() }

    suspend fun startTimer(gameId: Long, start: Long) = context.dataStore.edit {
        it[Keys.TIMER_GAME] = gameId
        it[Keys.TIMER_START] = start
    }

    suspend fun clearTimer() = context.dataStore.edit {
        it.remove(Keys.TIMER_GAME)
        it.remove(Keys.TIMER_START)
    }

    suspend fun saveProfile(profile: Profile) = context.dataStore.edit {
        it[Keys.NICK] = profile.nickname.ifBlank { "Игрок" }
        if (profile.avatarPath != null) it[Keys.AVATAR] = profile.avatarPath else it.remove(Keys.AVATAR)
        it[Keys.FAV_PLATFORM] = profile.favoritePlatform
        it[Keys.FAV_GENRE] = profile.favoriteGenre
    }
}
