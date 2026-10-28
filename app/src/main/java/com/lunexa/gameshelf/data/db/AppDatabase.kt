package com.lunexa.gameshelf.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.lunexa.gameshelf.data.model.Completion
import com.lunexa.gameshelf.data.model.Game
import com.lunexa.gameshelf.data.model.PlaySession
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    // --- Игры ---
    @Query("SELECT * FROM games ORDER BY addedAt DESC")
    fun observeGames(): Flow<List<Game>>

    @Query("SELECT * FROM games WHERE id = :id")
    fun observeGame(id: Long): Flow<Game?>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getGame(id: Long): Game?

    @Query("SELECT * FROM games")
    suspend fun getAllGames(): List<Game>

    @Insert
    suspend fun insertGame(game: Game): Long

    @Insert
    suspend fun insertGames(games: List<Game>)

    @Update
    suspend fun updateGame(game: Game)

    @Delete
    suspend fun deleteGame(game: Game)

    @Query("DELETE FROM games")
    suspend fun clearGames()

    // --- Сессии ---
    @Query("SELECT * FROM sessions WHERE gameId = :gameId ORDER BY startedAt DESC")
    fun observeSessions(gameId: Long): Flow<List<PlaySession>>

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun observeAllSessions(): Flow<List<PlaySession>>

    @Query("SELECT * FROM sessions")
    suspend fun getAllSessions(): List<PlaySession>

    @Insert
    suspend fun insertSession(session: PlaySession): Long

    @Insert
    suspend fun insertSessions(sessions: List<PlaySession>)

    @Delete
    suspend fun deleteSession(session: PlaySession)

    @Query("DELETE FROM sessions")
    suspend fun clearSessions()

    // --- Прохождения ---
    @Query("SELECT * FROM completions ORDER BY completedAt DESC")
    fun observeAllCompletions(): Flow<List<Completion>>

    @Query("SELECT * FROM completions")
    suspend fun getAllCompletions(): List<Completion>

    @Insert
    suspend fun insertCompletion(completion: Completion): Long

    @Insert
    suspend fun insertCompletions(completions: List<Completion>)

    @Query("DELETE FROM completions")
    suspend fun clearCompletions()
}

@Database(
    entities = [Game::class, PlaySession::class, Completion::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "gameshelf.db").build()
    }
}
