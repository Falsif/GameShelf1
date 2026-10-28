package com.lunexa.gameshelf.ui.edit

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunexa.gameshelf.data.GameRepository
import com.lunexa.gameshelf.data.ImageStore
import com.lunexa.gameshelf.data.RawgClient
import com.lunexa.gameshelf.data.RawgException
import com.lunexa.gameshelf.data.RawgGame
import com.lunexa.gameshelf.data.SettingsRepository
import com.lunexa.gameshelf.data.model.Game
import com.lunexa.gameshelf.data.model.GameStatus
import com.lunexa.gameshelf.data.model.splitList
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class GameForm(
    val title: String = "",
    val description: String = "",
    val coverUri: String? = null,
    val genre: String = "",
    val developer: String = "",
    val publisher: String = "",
    val releaseDate: Long? = null,
    val platforms: List<String> = emptyList(),
    val status: GameStatus = GameStatus.WANT,
    val rating: Int = 0,
    val hours: String = "",
    val minutes: String = "",
    val startDate: Long? = null,
    val finishDate: Long? = null,
    val notes: String = "",
    val isFavorite: Boolean = false,
    val inWishlist: Boolean = false,
    val wishPrice: String = "",
    val wishBuyDate: Long? = null,
    val wishPurchased: Boolean = false,
    val wishPriority: Int = 2,
    val rawgId: Int? = null
)

sealed interface RawgState {
    data object Idle : RawgState
    data object NoKey : RawgState
    data object Loading : RawgState
    data class Results(val items: List<RawgGame>) : RawgState
    data class Error(val message: String) : RawgState
}

class EditGameViewModel(
    savedState: SavedStateHandle,
    private val repository: GameRepository,
    private val images: ImageStore,
    private val rawg: RawgClient,
    private val settings: SettingsRepository
) : ViewModel() {

    private val gameId: Long = savedState.get<Long>("id") ?: -1L
    val isNew: Boolean = gameId <= 0L

    private var original: Game? = null
    private var initialForm = GameForm()

    var loading by mutableStateOf(!isNew)
        private set
    var form by mutableStateOf(GameForm(inWishlist = savedState.get<Boolean>("wish") == true))
        private set
    var errors by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var saving by mutableStateOf(false)
        private set
    var coverLoading by mutableStateOf(false)
        private set
    var askDuplicate by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    // Онлайн-поиск
    var rawgQuery by mutableStateOf("")
    var rawgState by mutableStateOf<RawgState>(RawgState.Idle)
        private set
    var rawgApplying by mutableStateOf(false)
        private set
    private var searchJob: Job? = null

    val isDirty: Boolean get() = form != initialForm

    init {
        initialForm = form
        if (!isNew) {
            viewModelScope.launch {
                val g = repository.getGame(gameId)
                if (g != null) {
                    original = g
                    form = g.toForm()
                    initialForm = form
                } else {
                    message = "Игра не найдена"
                }
                loading = false
            }
        }
    }

    fun update(transform: (GameForm) -> GameForm) {
        form = transform(form)
        if (errors.isNotEmpty()) errors = validate(form, onlyExisting = errors.keys)
    }

    fun consumeMessage() { message = null }

    fun pickCover(uri: Uri) {
        viewModelScope.launch {
            coverLoading = true
            val path = images.importFromUri(uri)
            coverLoading = false
            if (path != null) {
                update { it.copy(coverUri = path) }
            } else {
                message = "Не удалось загрузить изображение"
            }
        }
    }

    fun removeCover() = update { it.copy(coverUri = null) }

    fun togglePlatform(p: String) = update {
        val list = if (p in it.platforms) it.platforms - p else it.platforms + p
        it.copy(platforms = list)
    }

    fun addCustomPlatform(p: String) {
        val name = p.trim().replace(",", " ")
        if (name.isEmpty()) return
        update { if (it.platforms.any { x -> x.equals(name, true) }) it else it.copy(platforms = it.platforms + name) }
    }

    fun addGenre(g: String) = update {
        val list = splitList(it.genre)
        if (list.any { x -> x.equals(g, true) }) it.copy(genre = (list.filterNot { x -> x.equals(g, true) }).joinToString(", "))
        else it.copy(genre = (list + g).joinToString(", "))
    }

    // ---------- Сохранение ----------

    private fun validate(f: GameForm, onlyExisting: Set<String>? = null): Map<String, String> {
        val e = mutableMapOf<String, String>()
        if (f.title.isBlank()) e["title"] = "Введите название игры"
        else if (f.title.length > 150) e["title"] = "Слишком длинное название"
        val h = if (f.hours.isBlank()) 0 else f.hours.toIntOrNull()
        if (h == null || h < 0 || h > 100000) e["hours"] = "Некорректно"
        val m = if (f.minutes.isBlank()) 0 else f.minutes.toIntOrNull()
        if (m == null || m < 0 || m > 59) e["minutes"] = "0–59"
        if (f.inWishlist && f.wishPrice.isNotBlank()) {
            val price = f.wishPrice.replace(',', '.').replace(" ", "").toDoubleOrNull()
            if (price == null || price < 0) e["price"] = "Введите число"
        }
        if (f.startDate != null && f.finishDate != null && f.finishDate < f.startDate) {
            e["finish"] = "Раньше даты начала"
        }
        return if (onlyExisting == null) e else e.filterKeys { it in onlyExisting }
    }

    fun save(force: Boolean = false, onSaved: (Long, Boolean) -> Unit) {
        if (saving) return
        val f = form.copy(title = form.title.trim())
        val e = validate(f)
        errors = e
        if (e.isNotEmpty()) {
            message = "Проверьте поля формы"
            return
        }
        viewModelScope.launch {
            saving = true
            try {
                if (!force && repository.hasDuplicate(f.title, if (isNew) -1L else gameId)) {
                    askDuplicate = true
                    return@launch
                }
                val game = f.toGame(original)
                val id = repository.saveGame(original, game)
                initialForm = form
                onSaved(id, isNew)
            } catch (ex: Exception) {
                message = "Не удалось сохранить: ${ex.message ?: "ошибка базы данных"}"
            } finally {
                saving = false
            }
        }
    }

    fun dismissDuplicate() { askDuplicate = false }

    // ---------- RAWG ----------

    fun searchRawg() {
        val q = rawgQuery.trim()
        if (q.length < 2) {
            rawgState = RawgState.Error("Введите хотя бы 2 символа")
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val key = settings.currentSettings().rawgKey
            if (key.isBlank()) {
                rawgState = RawgState.NoKey
                return@launch
            }
            rawgState = RawgState.Loading
            rawgState = try {
                val list = rawg.search(q, key)
                if (list.isEmpty()) RawgState.Error("Ничего не найдено. Попробуйте другой запрос.") else RawgState.Results(list)
            } catch (e: RawgException) {
                RawgState.Error(e.message ?: "Ошибка поиска")
            } catch (e: Exception) {
                RawgState.Error("Ошибка: ${e.message}")
            }
        }
    }

    fun checkRawgKey() {
        viewModelScope.launch {
            if (settings.currentSettings().rawgKey.isBlank()) rawgState = RawgState.NoKey
            else if (rawgState == RawgState.NoKey) rawgState = RawgState.Idle
        }
    }

    /** Заполняет форму данными из каталога и скачивает обложку для офлайн-доступа. */
    fun applyRawg(item: RawgGame, onDone: () -> Unit) {
        viewModelScope.launch {
            rawgApplying = true
            try {
                val key = settings.currentSettings().rawgKey
                val details = runCatching { rawg.details(item.id, key) }.getOrNull()
                val imageUrl = details?.imageUrl ?: item.imageUrl
                val cover = imageUrl?.let { images.importFromUrl(it) ?: it }
                val platforms = (details?.platforms ?: item.platforms).map { it.replace(",", " ") }
                val genres = details?.genres ?: item.genres
                update {
                    it.copy(
                        title = details?.name ?: item.name,
                        description = details?.description?.take(5000) ?: it.description,
                        coverUri = cover ?: it.coverUri,
                        genre = genres.joinToString(", ").ifEmpty { it.genre },
                        developer = details?.developers?.joinToString(", ")?.ifEmpty { null } ?: it.developer,
                        publisher = details?.publishers?.joinToString(", ")?.ifEmpty { null } ?: it.publisher,
                        releaseDate = RawgClient.parseDate(details?.released ?: item.released) ?: it.releaseDate,
                        platforms = if (platforms.isNotEmpty()) platforms else it.platforms,
                        rawgId = item.id
                    )
                }
                message = if (details == null) "Заполнены основные данные (подробности недоступны)" else "Данные игры заполнены"
                onDone()
            } finally {
                rawgApplying = false
            }
        }
    }

    // ---------- Преобразования ----------

    private fun Game.toForm() = GameForm(
        title = title,
        description = description,
        coverUri = coverUri,
        genre = genre,
        developer = developer,
        publisher = publisher,
        releaseDate = releaseDate,
        platforms = splitList(platforms),
        status = GameStatus.from(status),
        rating = rating,
        hours = if (manualMinutes > 0) (manualMinutes / 60).toString() else "",
        minutes = if (manualMinutes % 60 > 0) (manualMinutes % 60).toString() else "",
        startDate = startDate,
        finishDate = finishDate,
        notes = notes,
        isFavorite = isFavorite,
        inWishlist = inWishlist,
        wishPrice = wishPrice?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "",
        wishBuyDate = wishBuyDate,
        wishPurchased = wishPurchased,
        wishPriority = wishPriority,
        rawgId = rawgId
    )

    private fun GameForm.toGame(base: Game?): Game {
        val minutesTotal = (hours.toIntOrNull() ?: 0) * 60 + (minutes.toIntOrNull() ?: 0)
        val price = wishPrice.replace(',', '.').replace(" ", "").toDoubleOrNull()
        val b = base ?: Game(title = title)
        return b.copy(
            title = title,
            description = description.trim(),
            coverUri = coverUri,
            genre = splitList(genre).joinToString(", "),
            developer = developer.trim(),
            publisher = publisher.trim(),
            releaseDate = releaseDate,
            platforms = platforms.joinToString(", "),
            status = status.name,
            rating = rating,
            manualMinutes = minutesTotal,
            startDate = startDate,
            finishDate = finishDate,
            notes = notes.trim(),
            isFavorite = isFavorite,
            inWishlist = inWishlist,
            wishPrice = if (inWishlist) price else b.wishPrice,
            wishBuyDate = wishBuyDate,
            wishPurchased = wishPurchased,
            wishPriority = wishPriority,
            rawgId = rawgId
        )
    }
}
