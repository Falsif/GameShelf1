package com.lunexa.gameshelf.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Хранит обложки и аватар во внутренней памяти приложения,
 * чтобы изображения не пропадали, даже если исходный файл в галерее удалён.
 */
class ImageStore(private val context: Context) {

    private val dir: File
        get() = File(context.filesDir, "images").apply { mkdirs() }

    fun isLocal(path: String?): Boolean =
        path != null && path.startsWith(dir.absolutePath)

    /** Копирует изображение из галереи (content://) с уменьшением размера. */
    suspend fun importFromUri(uri: Uri, prefix: String = "cover"): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@runCatching null
            saveBytes(bytes, prefix)
        }.getOrNull()
    }

    /** Скачивает обложку из интернета, чтобы она была доступна офлайн. */
    suspend fun importFromUrl(url: String, prefix: String = "cover"): String? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 20000
            }
            try {
                if (conn.responseCode !in 200..299) return@runCatching null
                val bytes = conn.inputStream.use { it.readBytes() }
                saveBytes(bytes, prefix)
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    private fun saveBytes(bytes: ByteArray, prefix: String): String? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_SIDE || bounds.outHeight / (sample * 2) >= MAX_SIDE) {
            sample *= 2
        }
        val bitmap = BitmapFactory.decodeByteArray(
            bytes, 0, bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: return null
        val file = File(dir, "${prefix}_${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        bitmap.recycle()
        return file.absolutePath
    }

    fun delete(path: String?) {
        if (isLocal(path)) runCatching { File(path!!).delete() }
    }

    fun toBase64(path: String?): String? {
        if (!isLocal(path)) return null
        return runCatching {
            Base64.encodeToString(File(path!!).readBytes(), Base64.NO_WRAP)
        }.getOrNull()
    }

    fun fromBase64(data: String, prefix: String = "cover"): String? = runCatching {
        val bytes = Base64.decode(data, Base64.NO_WRAP)
        val file = File(dir, "${prefix}_${UUID.randomUUID()}.jpg")
        file.writeBytes(bytes)
        file.absolutePath
    }.getOrNull()

    /** Удаляет файлы, на которые больше нет ссылок. */
    fun cleanup(keep: Set<String>) {
        dir.listFiles()?.forEach { f ->
            if (f.absolutePath !in keep) runCatching { f.delete() }
        }
    }

    private companion object {
        const val MAX_SIDE = 1100
    }
}
