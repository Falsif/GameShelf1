package com.lunexa.gameshelf.data

import com.lunexa.gameshelf.data.db.AppDatabase
import com.lunexa.gameshelf.data.model.Completion
import com.lunexa.gameshelf.data.model.Game
import com.lunexa.gameshelf.data.model.GameItem
import com.lunexa.gameshelf.data.model.GameStatus
import com.lunexa.gameshelf.data.model.PlaySession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GameRepository(
    private val db: AppDatabase,
    private val images: ImageStore
) {
    private val dao = db.gameDao()

    val games: Flow<List<Game>> = dao.observeGames()
    val sessions: Flow<List<PlaySession>> = dao.observeAllSessions()
    val completions: Flow<List<Completion>> = dao.observeAllCompletions()

    /** Все игры с подсчитанным временем и количеством прохождений. */
    val gameItems: Flow<List<GameItem>> = combine(games, sessions, completions) { g, s, c ->
        val minutes = s.groupBy { it.gameId }.mapValues { e -> e.value.sumOf { it.durationMinutes } }
        val compCount = c.groupingBy { it.gameId }.eachCount()
        g.map { GameItem(it, it.manualMinutes + (minutes[it.id] ?: 0), compCount[it.id] ?: 0) }
    }

    fun observeGame(id: Long): Flow<Game?> = dao.observeGame(id)
    fun observeSessions(gameId: Long): Flow<List<PlaySession>> = dao.observeSessions(gameId)
    suspend fun getGame(id: Long): Game? = dao.getGame(id)

    /** Есть ли уже игра с таким названием (без учёта регистра). */
    suspend fun hasDuplicate(title: String, excludeId: Long): Boolean {
        val norm = title.trim().lowercase()
        return dao.getAllGames().any { it.id != excludeId && it.title.trim().lowercase() == norm }
    }

    /**
     * Сохраняет игру. При переходе в статус «Пройдено» фиксирует дату завершения
     * и добавляет запись о прохождении.
     */
    suspend fun saveGame(old: Game?, new: Game): Long {
        val now = System.currentTimeMillis()
        var game = new.copy(updatedAt = now)
        val becameCompleted = game.status == GameStatus.COMPLETED.name &&
            old?.status != GameStatus.COMPLETED.name
        if (becameCompleted && game.finishDate == null) game = game.copy(finishDate = now)
        val becamePlaying = (game.status == GameStatus.PLAYING.name || game.status == GameStatus.REPLAYING.name) &&
            game.startDate == null
        if (becamePlaying) game = game.copy(startDate = now)

        val id = if (old == null) {
            dao.insertGame(game.copy(id = 0, addedAt = now))
        } else {
            if (old.coverUri != game.coverUri) images.delete(old.coverUri)
            dao.updateGame(game)
            game.id
        }
        if (becameCompleted) {
            dao.insertCompletion(Completion(gameId = id, completedAt = game.finishDate ?: now))
        }
        return id
    }

    suspend fun changeStatus(game: Game, status: GameStatus) {
        if (game.status == status.name) return
        var updated = game.copy(status = status.name)
        if (status == GameStatus.REPLAYING) {
            updated = updated.copy(startDate = System.currentTimeMillis(), finishDate = null)
        }
        if (status == GameStatus.COMPLETED) updated = updated.copy(finishDate = null)
        saveGame(game, updated)
    }

    suspend fun startReplay(game: Game) = changeStatus(game, GameStatus.REPLAYING)

    suspend fun update(game: Game) {
        dao.updateGame(game.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteGame(game: Game) {
        dao.deleteGame(game)
        images.delete(game.coverUri)
    }

    suspend fun addSession(session: PlaySession) {
        dao.insertSession(session)
    }

    suspend fun deleteSession(session: PlaySession) = dao.deleteSession(session)
}
