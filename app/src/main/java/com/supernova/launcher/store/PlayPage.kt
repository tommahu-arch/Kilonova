package com.supernova.launcher.store

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

// A Google Play Store page read for the category it names. Play has no open
// API; the page carries the app's schema.org block, whose applicationCategory
// is the id Play files the app under (MUSIC_AND_AUDIO, GAME_PUZZLE, ...).
// Only the package name goes out, with a generic browser identity.
//
// Added for Kilonova; not part of upstream Supernova.
object PlayPage {

    sealed interface Result {
        // The page's category (null: the page names none).
        data class Found(val category: String?) : Result
        data object NotListed : Result
        // Too many requests or the service is down: stop for now.
        data object Throttled : Result
        data object Failed : Result
    }

    private val categoryPattern = Regex(""""applicationCategory"\s*:\s*"([A-Z][A-Z_]*)"""")

    fun parse(html: CharSequence): String? = categoryPattern.find(html)?.groupValues?.get(1)

    fun fetch(packageName: String): Result {
        val url = URL("https://play.google.com/store/apps/details?id=" + java.net.URLEncoder.encode(packageName, "UTF-8") + "&hl=en&gl=US")
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.8")
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> Result.Found(parse(readCapped(connection)))
                HttpURLConnection.HTTP_NOT_FOUND -> Result.NotListed
                429, 503 -> Result.Throttled
                else -> if (code in 400..499) Result.NotListed else Result.Failed
            }
        } catch (_: IOException) {
            Result.Failed
        } finally {
            connection.disconnect()
        }
    }

    // The page runs past a megabyte and the block sits near its end; a cap
    // keeps a runaway answer from filling memory.
    private fun readCapped(connection: HttpURLConnection): CharSequence {
        val text = StringBuilder()
        connection.inputStream.bufferedReader().use { reader ->
            val buffer = CharArray(BUFFER)
            while (text.length < MAX_CHARS) {
                val read = reader.read(buffer)
                if (read < 0) break
                text.append(buffer, 0, read)
            }
        }
        return text
    }

    private const val TIMEOUT_MS = 20_000
    private const val BUFFER = 16 * 1024
    private const val MAX_CHARS = 4_000_000
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
}
