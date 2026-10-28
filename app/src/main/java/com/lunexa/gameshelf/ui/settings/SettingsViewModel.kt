package com.lunexa.gameshelf.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunexa.gameshelf.AppContainer
import com.lunexa.gameshelf.data.AppSettings
import com.lunexa.gameshelf.data.BackupException
import com.lunexa.gameshelf.data.RawgException
import com.lunexa.gameshelf.ui.common.gamesCount
import com.lunexa.gameshelf.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val c: AppContainer) : ViewModel() {

    val settings: StateFlow<AppSettings?> = c.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val gamesTotal: StateFlow<Int> = c.repository.games.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _autoBackup = MutableStateFlow(c.backup.autoBackupTime())
    val autoBackup: StateFlow<Long?> = _autoBackup.asStateFlow()

    fun consumeMessage() { _message.value = null }

    fun setTheme(mode: ThemeMode) { viewModelScope.launch { c.settings.setTheme(mode) } }
    fun setAccent(color: Long) { viewModelScope.launch { c.settings.setAccent(color) } }
    fun setGrid(grid: Boolean) { viewModelScope.launch { c.settings.setLibraryGrid(grid) } }

    fun saveRawgKey(key: String) {
        viewModelScope.launch {
            c.settings.setRawgKey(key)
            _message.value = if (key.isBlank()) "API-ключ удалён" else "API-ключ сохранён"
        }
    }

    fun testRawgKey(key: String) = runBusy {
        if (key.isBlank()) {
            _message.value = "Сначала введите API-ключ"
            return@runBusy
        }
        try {
            c.rawg.search("portal", key.trim())
            c.settings.setRawgKey(key)
            _message.value = "Ключ работает ✓ и сохранён"
        } catch (e: RawgException) {
            _message.value = e.message
        }
    }

    fun exportBackup(uri: Uri) = runBusy {
        val s = c.backup.exportTo(uri)
        _message.value = "Экспортировано: ${gamesCount(s.games)}, сессий ${s.sessions}"
    }

    fun importBackup(uri: Uri) = runBusy {
        val s = c.backup.importFrom(uri)
        _autoBackup.value = c.backup.autoBackupTime()
        _message.value = "Восстановлено: ${gamesCount(s.games)}, сессий ${s.sessions}"
    }

    fun restoreAuto() = runBusy {
        val s = c.backup.restoreAutoBackup()
        _message.value = "Автокопия восстановлена: ${gamesCount(s.games)}"
    }

    fun resetAll() = runBusy {
        c.backup.resetAll()
        _autoBackup.value = c.backup.autoBackupTime()
        _message.value = "Данные сброшены. Автокопия сохранена — их можно вернуть."
    }

    private fun runBusy(block: suspend () -> Unit) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            try {
                block()
            } catch (e: BackupException) {
                _message.value = e.message
            } catch (e: Exception) {
                _message.value = "Ошибка: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                _busy.value = false
            }
        }
    }
}
