package com.supernova.launcher.store

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.supernova.launcher.data.StoreCategories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

// The one, process-wide lookup of store categories: F-Droid's index
// (weekly), then a Play page for each installed app nothing is known of,
// throttled, saved as it goes so a killed run loses nothing. Runs on its
// own; `changes` ticks whenever new answers were saved, for the drawer to
// regroup.
//
// Added for Kilonova; not part of upstream Supernova.
class StoreSync private constructor(context: Context) {

    sealed interface Status {
        data object Idle : Status
        data class Running(val done: Int, val total: Int) : Status
        data class Finished(val at: Long, val failures: Int, val stoppedEarly: Boolean) : Status
        data class Failed(val reason: String) : Status
    }

    private val appContext = context.applicationContext
    val cache = StoreCache(java.io.File(appContext.filesDir, "store"))
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val statusFlow = MutableStateFlow<Status>(Status.Idle)
    val status: StateFlow<Status> = statusFlow

    private val changesFlow = MutableStateFlow(0)
    val changes: StateFlow<Int> = changesFlow

    fun running(): Boolean = job?.isActive == true

    // Starts a run unless one is going. Unless forced, it waits for a
    // connection that is not metered and skips what is still fresh.
    fun start(packages: Collection<String>, force: Boolean) {
        if (running() || packages.isEmpty()) return
        if (!force && !unmetered()) return
        val wanted = packages.toList()
        job = scope.launch { run(wanted, force) }
    }

    private suspend fun run(packages: List<String>, force: Boolean) {
        statusFlow.value = Status.Running(0, packages.size)
        try {
            if (force || !cache.fdroidHasIndex() || cache.fdroidAgeMs() > StoreCache.FDROID_TTL_MS) syncFdroid()
            val due = cache.playDue(packages, ::narrowOnFdroid)
            var failures = 0
            var stopped = false
            var done = 0
            for (pkg in due) {
                if (!scope.isActive) return
                if (!force && !unmetered()) {
                    stopped = true
                    break
                }
                when (val result = PlayPage.fetch(pkg)) {
                    is PlayPage.Result.Found -> cache.putPlay(pkg, result.category)
                    PlayPage.Result.NotListed -> cache.putPlay(pkg, null)
                    PlayPage.Result.Throttled -> stopped = true
                    PlayPage.Result.Failed -> {
                        failures++
                        cache.putPlayFailure(pkg)
                    }
                }
                if (stopped) break
                done++
                statusFlow.value = Status.Running(done, due.size)
                if (done % CHANGE_EVERY == 0) changesFlow.value += 1
                delay(PAUSE_MS)
            }
            changesFlow.value += 1
            statusFlow.value = Status.Finished(System.currentTimeMillis(), failures, stopped)
        } catch (e: Exception) {
            changesFlow.value += 1
            statusFlow.value = Status.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    private fun narrowOnFdroid(categories: List<String>): Boolean = categories.any { !StoreCategories.isBroad(it) }

    // The index, only when it changed since the copy kept (ETag).
    private fun syncFdroid() {
        val connection = URL(FDROID_INDEX).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            cache.fdroidEtag().takeIf { it.isNotEmpty() && cache.fdroidHasIndex() }
                ?.let { connection.setRequestProperty("If-None-Match", it) }
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_NOT_MODIFIED -> cache.touchFdroid()
                HttpURLConnection.HTTP_OK -> {
                    val categories = connection.inputStream.use { FdroidIndexParser.categories(it) }
                    cache.putFdroid(
                        StoreCache.FdroidIndex(
                            fetchedAt = System.currentTimeMillis(),
                            etag = connection.getHeaderField("ETag").orEmpty(),
                            categories = categories
                        )
                    )
                    changesFlow.value += 1
                }
                else -> error("F-Droid index: HTTP $code")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun unmetered(): Boolean {
        val manager = appContext.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = manager.activeNetwork ?: return false
        val caps = manager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    companion object {
        private const val FDROID_INDEX = "https://f-droid.org/repo/index-v2.json"
        private const val TIMEOUT_MS = 30_000
        private const val PAUSE_MS = 1_200L
        private const val CHANGE_EVERY = 15

        @Volatile
        private var instance: StoreSync? = null

        fun get(context: Context): StoreSync =
            instance ?: synchronized(this) { instance ?: StoreSync(context).also { instance = it } }
    }
}
