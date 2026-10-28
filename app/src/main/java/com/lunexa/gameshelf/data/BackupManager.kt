package com.lunexa.gameshelf.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.lunexa.gameshelf.data.db.AppDatabase
import com.lunexa.gameshelf.data.model.Completion
import com.lunexa.gameshelf.data.model.Game
import com.lunexa.gameshelf.data.model.GameStatus
import com.lunexa.gameshelf.data.model.PlaySession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class BackupException(message: String) : Exception(message)

data class BackupSummary(val games: Int, val sessions: Int, val completions: Int)

/**
 * Резервное копирование в JSON.
 * Восстановление безопасное: файл полностью разбирается и проверяется до изменения базы,
 * перед заменой данных создаётся автоматическая копия, а замена идёт одной транзакцией.
 */
class BackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val images: ImageStore,
    private val settings: SettingsRepository
) {
    private val dao = db.gameDao()

    private val autoBackupFile: File
        get() = File(File(context.filesDir, "backups").apply { mkdirs() }, "auto_backup.json")

    fun autoBackupTime(): Long? = autoBackupFile.takeIf { it.exists() }?.lastModified()

    // ---------- Экспорт ----------

    suspend fun buildJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "GameShelf")
        root.put("developer", "LUNEXA")
        root.put("format", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val games = JSONArray()
        dao.getAllGames().forEach { g -> games.put(gameToJson(g)) }
        root.put("games", games)

        val sessions = JSONArray()
        dao.getAllSessions().forEach { s ->
            sessions.put(JSONObject().apply {
                put("id", s.id); put("gameId", s.gameId); put("startedAt", s.startedAt)
                put("durationMinutes", s.durationMinutes); put("note", s.note)
            })
        }
        root.put("sessions", sessions)

        val completions = JSONArray()
        dao.getAllCompletions().forEach { c ->
            completions.put(JSONObject().apply {
                put("id", c.id); put("gameId", c.gameId); put("completedAt", c.completedAt)
            })
        }
        root.put("completions", completions)

        val p = settings.currentProfile()
        root.put("profile", JSONObject().apply {
            put("nickname", p.nickname)
            put("favoritePlatform", p.favoritePlatform)
            put("favoriteGenre", p.favoriteGenre)
            images.toBase64(p.avatarPath)?.let { put("avatarBase64", it) }
        })
        root.toString()
    }

    suspend fun exportTo(uri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val json = buildJson()
        val out = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw BackupException("Не удалось открыть файл для записи")
        out.use { it.write(json.toByteArray(Charsets.UTF_8)) }
        summaryOf(JSONObject(json))
    }

    /** Сохраняет текущие данные во внутреннюю автоматическую копию. */
    suspend fun writeAutoBackup() = withContext(Dispatchers.IO) {
        if (dao.getAllGames().isEmpty()) return@withContext
        autoBackupFile.writeText(buildJson(), Charsets.UTF_8)
    }

    // ---------- Импорт ----------

    suspend fun importFrom(uri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use {
            it.readBytes().toString(Charsets.UTF_8)
        } ?: throw BackupException("Не удалось прочитать файл")
        restoreJson(text)
    }

    suspend fun restoreAutoBackup(): BackupSummary = withContext(Dispatchers.IO) {
        if (!autoBackupFile.exists()) throw BackupException("Автоматическая копия не найдена")
        val text = autoBackupFile.readText(Charsets.UTF_8)
        restoreJson(text, makeSafetyCopy = false)
    }

    private suspend fun restoreJson(text: String, makeSafetyCopy: Boolean = true): BackupSummary {
        val parsed = parse(text) // бросает BackupException, база ещё не тронута
        if (makeSafetyCopy) writeAutoBackup()

        // Обложки сохраняем в файлы до транзакции
        val games = parsed.games.map { (game, coverB64) ->
            if (coverB64 != null) game.copy(coverUri = images.fromBase64(coverB64)) else game
        }
        db.withTransaction {
            dao.clearCompletions()
            dao.clearSessions()
            dao.clearGames()
            dao.insertGames(games)
            dao.insertSessions(parsed.sessions)
            dao.insertCompletions(parsed.completions)
        }
        parsed.profile?.let { p ->
            val old = settings.currentProfile()
            val avatar = parsed.avatarB64?.let { images.fromBase64(it, "avatar") }
            settings.saveProfile(p.copy(avatarPath = avatar ?: old.avatarPath))
        }
        cleanupOrphans()
        return BackupSummary(games.size, parsed.sessions.size, parsed.completions.size)
    }

    /** Полный сброс пользовательских данных (с предварительной автокопией). */
    suspend fun resetAll() = withContext(Dispatchers.IO) {
        writeAutoBackup()
        db.withTransaction {
            dao.clearCompletions()
            dao.clearSessions()
            dao.clearGames()
        }
        settings.saveProfile(Profile())
        settings.clearTimer()
        cleanupOrphans()
    }

    /** Удаляет изображения, которые больше нигде не используются. */
    suspend fun cleanupOrphans() = withContext(Dispatchers.IO) {
        val keep = mutableSetOf<String>()
        dao.getAllGames().forEach { g -> g.coverUri?.let { keep += it } }
        settings.currentProfile().avatarPath?.let { keep += it }
        images.cleanup(keep)
    }

    // ---------- Разбор ----------

    private class Parsed(
        val games: List<Pair<Game, String?>>,
        val sessions: List<PlaySession>,
        val completions: List<Completion>,
        val profile: Profile?,
        val avatarB64: String?
    )

    private fun parse(text: String): Parsed {
        val root = try {
            JSONObject(text)
        } catch (e: Exception) {
            throw BackupException("Файл не является корректным JSON")
        }
        if (root.optString("app") != "GameShelf") {
            throw BackupException("Это не резервная копия GameShelf")
        }
        val gamesArr = root.optJSONArray("games") ?: throw BackupException("В копии нет списка игр")
        try {
            val games = (0 until gamesArr.length()).map { i -> jsonToGame(gamesArr.getJSONObject(i)) }
            val ids = games.map { it.first.id }.toSet()
            if (ids.size != games.size) throw BackupException("В копии повторяются идентификаторы игр")

            val sArr = root.optJSONArray("sessions") ?: JSONArray()
            val sessions = (0 until sArr.length()).map { i ->
                val o = sArr.getJSONObject(i)
                PlaySession(
                    id = o.getLong("id"),
                    gameId = o.getLong("gameId"),
                    startedAt = o.getLong("startedAt"),
                    durationMinutes = o.getInt("durationMinutes").coerceAtLeast(0),
                    note = o.optString("note", "")
                )
            }.filter { it.gameId in ids }

            val cArr = root.optJSONArray("completions") ?: JSONArray()
            val completions = (0 until cArr.length()).map { i ->
                val o = cArr.getJSONObject(i)
                Completion(id = o.getLong("id"), gameId = o.getLong("gameId"), completedAt = o.getLong("completedAt"))
            }.filter { it.gameId in ids }

            val pObj = root.optJSONObject("profile")
            val profile = pObj?.let {
                Profile(
                    nickname = it.optString("nickname", "Игрок"),
                    favoritePlatform = it.optString("favoritePlatform", ""),
                    favoriteGenre = it.optString("favoriteGenre", "")
                )
            }
            val avatar = pObj?.optString("avatarBase64")?.takeIf { it.isNotEmpty() }
            return Parsed(games, sessions, completions, profile, avatar)
        } catch (e: BackupException) {
            throw e
        } catch (e: Exception) {
            throw BackupException("Файл повреждён: ${e.message ?: "ошибка формата"}")
        }
    }

    private fun gameToJson(g: Game): JSONObject = JSONObject().apply {
        put("id", g.id)
        put("title", g.title)
        put("description", g.description)
        // Локальные обложки встраиваем в файл, ссылки из интернета сохраняем как есть
        val b64 = images.toBase64(g.coverUri)
        if (b64 != null) put("coverBase64", b64) else g.coverUri?.let { put("coverUri", it) }
        put("genre", g.genre)
        put("developer", g.developer)
        put("publisher", g.publisher)
        g.releaseDate?.let { put("releaseDate", it) }
        put("platforms", g.platforms)
        put("status", g.status)
        put("rating", g.rating)
        put("manualMinutes", g.manualMinutes)
        g.startDate?.let { put("startDate", it) }
        g.finishDate?.let { put("finishDate", it) }
        put("notes", g.notes)
        put("isFavorite", g.isFavorite)
        put("inWishlist", g.inWishlist)
        g.wishPrice?.let { put("wishPrice", it) }
        g.wishBuyDate?.let { put("wishBuyDate", it) }
        put("wishPurchased", g.wishPurchased)
        put("wishPriority", g.wishPriority)
        g.rawgId?.let { put("rawgId", it) }
        put("addedAt", g.addedAt)
        put("updatedAt", g.updatedAt)
    }

    private fun jsonToGame(o: JSONObject): Pair<Game, String?> {
        val title = o.getString("title").trim()
        if (title.isEmpty()) throw BackupException("В копии есть игра без названия")
        fun optLongOrNull(key: String): Long? = if (o.has(key) && !o.isNull(key)) o.getLong(key) else null
        val game = Game(
            id = o.getLong("id"),
            title = title,
            description = o.optString("description", ""),
            coverUri = if (o.has("coverUri") && !o.isNull("coverUri")) o.getString("coverUri") else null,
            genre = o.optString("genre", ""),
            developer = o.optString("developer", ""),
            publisher = o.optString("publisher", ""),
            releaseDate = optLongOrNull("releaseDate"),
            platforms = o.optString("platforms", ""),
            status = GameStatus.from(o.optString("status")).name,
            rating = o.optInt("rating", 0).coerceIn(0, 10),
            manualMinutes = o.optInt("manualMinutes", 0).coerceAtLeast(0),
            startDate = optLongOrNull("startDate"),
            finishDate = optLongOrNull("finishDate"),
            notes = o.optString("notes", ""),
            isFavorite = o.optBoolean("isFavorite", false),
            inWishlist = o.optBoolean("inWishlist", false),
            wishPrice = if (o.has("wishPrice") && !o.isNull("wishPrice")) o.getDouble("wishPrice") else null,
            wishBuyDate = optLongOrNull("wishBuyDate"),
            wishPurchased = o.optBoolean("wishPurchased", false),
            wishPriority = o.optInt("wishPriority", 2).coerceIn(1, 3),
            rawgId = if (o.has("rawgId") && !o.isNull("rawgId")) o.getInt("rawgId") else null,
            addedAt = o.optLong("addedAt", System.currentTimeMillis()),
            updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
        )
        val b64 = if (o.has("coverBase64")) o.optString("coverBase64").takeIf { it.isNotEmpty() } else null
        return game to b64
    }

    private fun summaryOf(root: JSONObject) = BackupSummary(
        root.optJSONArray("games")?.length() ?: 0,
        root.optJSONArray("sessions")?.length() ?: 0,
        root.optJSONArray("completions")?.length() ?: 0
    )
}
