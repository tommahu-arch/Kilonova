// Kilonova: added file, not part of upstream Supernova.
package com.supernova.launcher.search

import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.supernova.launcher.data.LauncherPrefs
import com.supernova.launcher.data.SearchEngine

// One UI's Finder, which on recent One UI lives inside One UI Home: its
// search screen takes the standard SEARCH action, with the query (if any)
// already typed, and searches files, notes, settings and more, which
// Kilonova's own search can't reach.
object SamsungFinder {

    private val component = ComponentName(
        "com.sec.android.app.launcher",
        "com.sec.android.app.launcher.search.SearchActivity"
    )

    private fun intent(query: String?): Intent = Intent(Intent.ACTION_SEARCH)
        .setComponent(component)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .apply { if (!query.isNullOrBlank()) putExtra(SearchManager.QUERY, query) }

    // Whether this phone has a Finder another app may open.
    fun available(context: Context): Boolean = runCatching {
        context.packageManager.resolveActivity(intent(null), 0)?.activityInfo?.exported == true
    }.getOrDefault(false)

    // Whether search should go to Finder: picked, and still on the phone.
    fun chosen(context: Context, prefs: LauncherPrefs): Boolean =
        prefs.searchEngine == SearchEngine.SAMSUNG_FINDER && available(context)

    // Opens Finder; false when it couldn't, so the caller falls back to
    // Kilonova's own search.
    fun open(context: Context, query: String? = null): Boolean = runCatching {
        context.startActivity(intent(query))
        true
    }.getOrDefault(false)
}
