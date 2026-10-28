package com.lunexa.gameshelf.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunexa.gameshelf.data.GameRepository
import com.lunexa.gameshelf.data.SettingsRepository
import com.lunexa.gameshelf.data.model.Completion
import com.lunexa.gameshelf.data.model.Game
import com.lunexa.gameshelf.data.model.GameStatus
import com.lunexa.gameshelf.data.model.PlaySession
import com.lunexa.gameshelf.ui.common.formatMinutes
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(
    val loading: Boolean = true,
    val game: Game? = null,
    val sessions: List<PlaySession> = emptyList(),
    val completions: List<Completion> = emptyList(),
    val sessionMinutes: Int = 0,
    val timerStart: Long? = null,
    val otherTimerRunning: Boolean = false
) {
    val totalMinutes: Int get() = (game?.manualMinutes ?: 0) + sessionMinutes
}

class GameDetailViewModel(
    savedState: SavedStateHandle,
    private val repository: GameRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    private val gameId: Long = checkNotNull(savedState.get<Long>("id"))

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    val state: StateFlow<DetailUiState> = combine(
        repository.observeGame(gameId),
        repository.observeSessions(gameId),
        repository.completions,
        settings.settings
    ) { game, sessions, completions, s ->
        DetailUiState(
            loading = false,
            game = game,
            sessions = sessions,
            completions = completions.filter { it.gameId == gameId },
            sessionMinutes = sessions.sumOf { it.durationMinutes },
            timerStart = if (s.timerGameId == gameId) s.timerStart else null,
            otherTimerRunning = s.timerGameId != null && s.timerGameId != gameId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetailUiState())

    private fun current(): Game? = state.value.game

    private fun launchSafe(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                _messages.tryEmit("Ошибка: ${e.message ?: "не удалось сохранить"}")
            }
        }
    }

    fun setStatus(status: GameStatus) = launchSafe {
        val g = current() ?: return@launchSafe
        repository.changeStatus(g, status)
        if (status == GameStatus.COMPLETED && g.status != GameStatus.COMPLETED.name) {
            _messages.tryEmit("Поздравляем с прохождением «${g.title}»!")
        }
    }

    fun setRating(rating: Int) = launchSafe {
        val g = current() ?: return@launchSafe
        repository.update(g.copy(rating = rating.coerceIn(0, 10)))
    }

    fun toggleFavorite() = launchSafe {
        val g = current() ?: return@launchSafe
        repository.update(g.copy(isFavorite = !g.isFavorite))
        _messages.tryEmit(if (g.isFavorite) "Убрано из избранного" else "Добавлено в избранное")
    }

    fun toggleWishlist() = launchSafe {
        val g = current() ?: return@launchSafe
        repository.update(g.copy(inWishlist = !g.inWishlist))
        _messages.tryEmit(if (g.inWishlist) "Убрано из списка желаемого" else "Добавлено в список желаемого")
    }

    fun togglePurchased() = launchSafe {
        val g = current() ?: return@launchSafe
        repository.update(g.copy(wishPurchased = !g.wishPurchased))
    }

    fun startReplay() = launchSafe {
        val g = current() ?: return@launchSafe
        repository.startReplay(g)
        _messages.tryEmit("Начато повторное прохождение")
    }

    fun saveNotes(notes: String) = launchSafe {
        val g = current() ?: return@launchSafe
        repository.update(g.copy(notes = notes.trim()))
        _messages.tryEmit("Заметки сохранены")
    }

    fun delete(onDone: () -> Unit) = launchSafe {
        val g = current() ?: return@launchSafe
        if (state.value.timerStart != null) settings.clearTimer()
        repository.deleteGame(g)
        onDone()
    }

    fun addSession(minutes: Int, date: Long, note: String) = launchSafe {
        if (minutes <= 0) return@launchSafe
        repository.addSession(PlaySession(gameId = gameId, startedAt = date, durationMinutes = minutes, note = note.trim()))
        _messages.tryEmit("Сессия добавлена: ${formatMinutes(minutes)}")
    }

    fun deleteSession(session: PlaySession) = launchSafe {
        repository.deleteSession(session)
        _messages.tryEmit("Сессия удалена")
    }

    // ---------- Таймер ----------

    fun startTimer() = launchSafe {
        val s = settings.currentSettings()
        val otherId = s.timerGameId
        val otherStart = s.timerStart
        if (otherId != null && otherStart != null && otherId != gameId) {
            // Завершаем таймер другой игры, чтобы время не потерялось
            val minutes = ((System.currentTimeMillis() - otherStart) / 60000).toInt()
            val other = repository.getGame(otherId)
            if (other != null && minutes > 0) {
                repository.addSession(PlaySession(gameId = otherId, startedAt = otherStart, durationMinutes = minutes, note = "Таймер"))
                _messages.tryEmit("Сессия «${other.title}» сохранена: ${formatMinutes(minutes)}")
            }
        }
        settings.startTimer(gameId, System.currentTimeMillis())
        val g = current()
        if (g != null && g.status == GameStatus.WANT.name) repository.changeStatus(g, GameStatus.PLAYING)
    }

    fun stopTimer() = launchSafe {
        val start = state.value.timerStart ?: return@launchSafe
        settings.clearTimer()
        val minutes = ((System.currentTimeMillis() - start) / 60000).toInt()
        if (minutes < 1) {
            _messages.tryEmit("Сессия короче минуты не сохранена")
        } else {
            repository.addSession(PlaySession(gameId = gameId, startedAt = start, durationMinutes = minutes, note = "Таймер"))
            _messages.tryEmit("Сессия сохранена: ${formatMinutes(minutes)}")
        }
    }

    fun cancelTimer() = launchSafe {
        settings.clearTimer()
        _messages.tryEmit("Таймер сброшен")
    }
}
