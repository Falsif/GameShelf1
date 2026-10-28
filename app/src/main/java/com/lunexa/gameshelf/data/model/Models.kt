package com.lunexa.gameshelf.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Статусы прохождения с цветовыми индикаторами. */
enum class GameStatus(val title: String, val color: Long) {
    WANT("Хочу поиграть", 0xFF60A5FA),
    PLAYING("Играю сейчас", 0xFF34D399),
    COMPLETED("Пройдено", 0xFFFBBF24),
    PAUSED("Отложено", 0xFFFB923C),
    DROPPED("Брошено", 0xFFF87171),
    REPLAYING("Перепрохожу", 0xFFF472B6);

    companion object {
        fun from(name: String?): GameStatus = entries.firstOrNull { it.name == name } ?: WANT
    }
}

@Entity(tableName = "games", indices = [Index("title")])
data class Game(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val coverUri: String? = null,
    val genre: String = "",
    val developer: String = "",
    val publisher: String = "",
    /** Дата выпуска, миллисекунды (локальный полдень). */
    val releaseDate: Long? = null,
    /** Платформы через запятую. */
    val platforms: String = "",
    val status: String = GameStatus.WANT.name,
    /** Личная оценка 1..10, 0 — без оценки. */
    val rating: Int = 0,
    /** Время, введённое вручную (минуты), к нему прибавляются сессии. */
    val manualMinutes: Int = 0,
    val startDate: Long? = null,
    val finishDate: Long? = null,
    val notes: String = "",
    val isFavorite: Boolean = false,
    val inWishlist: Boolean = false,
    val wishPrice: Double? = null,
    val wishBuyDate: Long? = null,
    val wishPurchased: Boolean = false,
    /** 1 — низкий, 2 — средний, 3 — высокий. */
    val wishPriority: Int = 2,
    val rawgId: Int? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// Вычисляемые свойства вынесены из сущности, чтобы Room их не трогал.
val Game.statusEnum: GameStatus get() = GameStatus.from(status)
val Game.platformList: List<String> get() = splitList(platforms)
val Game.genreList: List<String> get() = splitList(genre)

fun splitList(value: String): List<String> =
    value.split(",").map { it.trim() }.filter { it.isNotEmpty() }

@Entity(
    tableName = "sessions",
    foreignKeys = [ForeignKey(
        entity = Game::class,
        parentColumns = ["id"],
        childColumns = ["gameId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("gameId")]
)
data class PlaySession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val startedAt: Long,
    val durationMinutes: Int,
    val note: String = ""
)

@Entity(
    tableName = "completions",
    foreignKeys = [ForeignKey(
        entity = Game::class,
        parentColumns = ["id"],
        childColumns = ["gameId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("gameId")]
)
data class Completion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val completedAt: Long
)

/** Игра вместе с вычисленным игровым временем и количеством прохождений. */
data class GameItem(
    val game: Game,
    val totalMinutes: Int,
    val completions: Int
)
