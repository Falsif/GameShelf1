package com.lunexa.gameshelf.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RawgException(message: String) : Exception(message)

data class RawgGame(
    val id: Int,
    val name: String,
    val released: String?,
    val imageUrl: String?,
    val genres: List<String>,
    val platforms: List<String>
) {
    val year: String? get() = released?.take(4)
}

data class RawgDetails(
    val id: Int,
    val name: String,
    val description: String,
    val released: String?,
    val imageUrl: String?,
    val genres: List<String>,
    val platforms: List<String>,
    val developers: List<String>,
    val publishers: List<String>
)

/** Клиент онлайн-каталога RAWG (https://rawg.io/apidocs). */
class RawgClient(private val context: Context) {

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun search(query: String, key: String): List<RawgGame> = withContext(Dispatchers.IO) {
        val url = "$BASE/games?key=${enc(key)}&search=${enc(query)}&page_size=20&search_precise=true"
        val json = get(url)
        val results = json.optJSONArray("results") ?: JSONArray()
        (0 until results.length()).map { i ->
            val o = results.getJSONObject(i)
            RawgGame(
                id = o.getInt("id"),
                name = o.optString("name"),
                released = o.optStringOrNull("released"),
                imageUrl = o.optStringOrNull("background_image"),
                genres = names(o.optJSONArray("genres")).map { translateGenre(it) },
                platforms = platformNames(o.optJSONArray("platforms"))
            )
        }
    }

    suspend fun details(id: Int, key: String): RawgDetails = withContext(Dispatchers.IO) {
        val o = get("$BASE/games/$id?key=${enc(key)}")
        RawgDetails(
            id = o.getInt("id"),
            name = o.optString("name"),
            description = o.optString("description_raw", "").trim(),
            released = o.optStringOrNull("released"),
            imageUrl = o.optStringOrNull("background_image"),
            genres = names(o.optJSONArray("genres")).map { translateGenre(it) },
            platforms = platformNames(o.optJSONArray("platforms")),
            developers = names(o.optJSONArray("developers")),
            publishers = names(o.optJSONArray("publishers"))
        )
    }

    private fun get(url: String): JSONObject {
        if (!isOnline()) throw RawgException("Нет подключения к интернету. Добавьте игру вручную.")
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 20000
            setRequestProperty("User-Agent", "GameShelf/1.0 (LUNEXA)")
        }
        try {
            val code = conn.responseCode
            when {
                code == 401 || code == 403 -> throw RawgException("API-ключ RAWG недействителен. Проверьте его в настройках.")
                code == 404 -> throw RawgException("Игра не найдена в каталоге")
                code == 429 -> throw RawgException("Превышен лимит запросов RAWG. Попробуйте позже.")
                code !in 200..299 -> throw RawgException("Сервер каталога ответил с ошибкой ($code)")
            }
            val body = conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            return JSONObject(body)
        } catch (e: RawgException) {
            throw e
        } catch (e: UnknownHostException) {
            throw RawgException("Нет подключения к интернету. Добавьте игру вручную.")
        } catch (e: SocketTimeoutException) {
            throw RawgException("Сервер не отвечает. Попробуйте ещё раз.")
        } catch (e: Exception) {
            throw RawgException("Ошибка сети: ${e.message ?: e.javaClass.simpleName}")
        } finally {
            conn.disconnect()
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun names(arr: JSONArray?): List<String> =
        if (arr == null) emptyList()
        else (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optString("name")?.takeIf { n -> n.isNotBlank() } }

    private fun platformNames(arr: JSONArray?): List<String> =
        if (arr == null) emptyList()
        else (0 until arr.length()).mapNotNull {
            arr.optJSONObject(it)?.optJSONObject("platform")?.optString("name")?.takeIf { n -> n.isNotBlank() }
        }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null

    companion object {
        private const val BASE = "https://api.rawg.io/api"

        private val genreMap = mapOf(
            "Action" to "Экшен", "Adventure" to "Приключения", "RPG" to "RPG",
            "Shooter" to "Шутер", "Strategy" to "Стратегия", "Simulation" to "Симулятор",
            "Racing" to "Гонки", "Sports" to "Спорт", "Puzzle" to "Головоломка",
            "Platformer" to "Платформер", "Fighting" to "Файтинг", "Indie" to "Инди",
            "Massively Multiplayer" to "MMO", "Arcade" to "Аркада", "Casual" to "Казуальная",
            "Family" to "Семейная", "Board Games" to "Настольная", "Educational" to "Обучающая",
            "Card" to "Карточная"
        )

        fun translateGenre(name: String): String = genreMap[name] ?: name

        /** "2015-05-18" → миллисекунды (локальный полдень). */
        fun parseDate(value: String?): Long? {
            if (value.isNullOrBlank()) return null
            return runCatching {
                val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value) ?: return null
                Calendar.getInstance().apply {
                    time = d
                    set(Calendar.HOUR_OF_DAY, 12)
                }.timeInMillis
            }.getOrNull()
        }
    }
}
