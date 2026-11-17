package com.supernova.launcher.store

import com.supernova.launcher.data.StoreInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File

// What the stores said, kept on disk so a later run asks for nothing it
// already knows. Two files: one entry per package for Play (a page each, the
// costly lookup), and F-Droid's index boiled down to package -> categories
// (one download covers every app, installed later or not).
//
// Added for Kilonova; not part of upstream Supernova.
class StoreCache(private val dir: File, private val clock: () -> Long = System::currentTimeMillis) {

    // Play's answer for a package. A found category stays fresh a month; a
    // package Play does not list (played = true, category null) two weeks;
    // both are used after that until the next lookup replaces them. A lookup
    // that failed keeps what was there and asks again after retryAt.
    @Serializable
    data class PlayEntry(
        val category: String? = null,
        val until: Long = 0L,
        val retryAt: Long = 0L
    )

    @Serializable
    data class FdroidIndex(
        val fetchedAt: Long = 0L,
        val etag: String = "",
        val categories: Map<String, List<String>> = emptyMap()
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val playSerializer = MapSerializer(String.serializer(), PlayEntry.serializer())
    private val playFile = File(dir, PLAY_FILE)
    private val fdroidFile = File(dir, FDROID_FILE)

    @Volatile
    private var play: Map<String, PlayEntry>? = null

    @Volatile
    private var fdroid: FdroidIndex? = null

    private fun loadedPlay(): Map<String, PlayEntry> = play ?: synchronized(this) {
        play ?: read(playFile) { json.decodeFromString(playSerializer, it) }.orEmpty().also { play = it }
    }

    private fun loadedFdroid(): FdroidIndex = fdroid ?: synchronized(this) {
        fdroid ?: read(fdroidFile) { json.decodeFromString(FdroidIndex.serializer(), it) }
            ?.also { fdroid = it } ?: FdroidIndex().also { fdroid = it }
    }

    // What is known of a package, or null while it is not: a package settles
    // when F-Droid lists it, or Play has answered (found or not).
    fun info(packageName: String): StoreInfo? {
        val fd = loadedFdroid().categories[packageName].orEmpty()
        val entry = loadedPlay()[packageName]
        val playKnown = entry != null && entry.until > 0L
        if (!playKnown && fd.isEmpty()) return null
        return StoreInfo(entry?.category, fd)
    }

    // Everything held for a package, for the metadata view: what Play said
    // (playAsked: it was asked; category null: it names none), and F-Droid's.
    class Details(val playAsked: Boolean, val playCategory: String?, val fdroid: List<String>)

    fun details(packageName: String): Details {
        val entry = loadedPlay()[packageName]
        return Details(
            playAsked = entry != null && entry.until > 0L,
            playCategory = entry?.category,
            fdroid = loadedFdroid().categories[packageName].orEmpty()
        )
    }

    // Packages that need a Play lookup now: none known to F-Droid with a
    // narrow category (that one would win over Play anyway), and no fresh
    // answer or recent failure.
    fun playDue(packages: Collection<String>, isNarrowOnFdroid: (List<String>) -> Boolean): List<String> {
        val now = clock()
        val index = loadedFdroid().categories
        val entries = loadedPlay()
        return packages.filter { pkg ->
            if (index[pkg]?.let(isNarrowOnFdroid) == true) return@filter false
            val entry = entries[pkg] ?: return@filter true
            entry.retryAt <= now && entry.until <= now
        }
    }

    fun fdroidAgeMs(): Long = clock() - loadedFdroid().fetchedAt

    fun fdroidFetchedAt(): Long = loadedFdroid().fetchedAt

    fun fdroidEtag(): String = loadedFdroid().etag

    fun fdroidHasIndex(): Boolean = loadedFdroid().fetchedAt > 0L

    fun putPlay(packageName: String, category: String?) {
        val now = clock()
        val ttl = if (category == null) NOT_FOUND_TTL_MS else FOUND_TTL_MS
        update { it + (packageName to PlayEntry(category, until = now + ttl)) }
    }

    fun putPlayFailure(packageName: String) {
        val now = clock()
        update { it + (packageName to (it[packageName] ?: PlayEntry()).copy(retryAt = now + RETRY_MS)) }
    }

    fun putFdroid(index: FdroidIndex) {
        fdroid = index
        write(fdroidFile, json.encodeToString(FdroidIndex.serializer(), index))
    }

    // The index was asked for and had not changed.
    fun touchFdroid() = putFdroid(loadedFdroid().copy(fetchedAt = clock()))

    private fun update(change: (Map<String, PlayEntry>) -> Map<String, PlayEntry>) = synchronized(this) {
        val next = change(loadedPlay())
        play = next
        write(playFile, json.encodeToString(playSerializer, next))
    }

    // Written whole and moved into place, so a kill mid-write leaves the old file.
    private fun write(file: File, text: String) {
        runCatching {
            dir.mkdirs()
            val temp = File(dir, file.name + ".tmp")
            temp.writeText(text)
            if (!temp.renameTo(file)) {
                file.delete()
                temp.renameTo(file)
            }
        }
    }

    private fun <T> read(file: File, parse: (String) -> T): T? =
        runCatching { if (file.isFile) parse(file.readText()) else null }.getOrNull()

    companion object {
        const val PLAY_FILE = "store_play.json"
        const val FDROID_FILE = "store_fdroid.json"
        const val FOUND_TTL_MS = 30L * 24 * 60 * 60 * 1000
        const val NOT_FOUND_TTL_MS = 14L * 24 * 60 * 60 * 1000
        const val RETRY_MS = 24L * 60 * 60 * 1000
        const val FDROID_TTL_MS = 7L * 24 * 60 * 60 * 1000
    }
}
