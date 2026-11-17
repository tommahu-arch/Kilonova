package com.supernova.launcher.store

import com.supernova.launcher.data.StoreInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class StoreCacheTest {
    @get:Rule
    val folder = TemporaryFolder()

    private var now = 1_000_000L

    private fun cache() = StoreCache(folder.root) { now }

    @Test
    fun playPageCategoryIsFoundInSchemaBlock() {
        val html = """<script type="application/ld+json">{"@type":"SoftwareApplication","applicationCategory":"MUSIC_AND_AUDIO","name":"x"}</script>"""
        assertEquals("MUSIC_AND_AUDIO", com.supernova.launcher.store.PlayPage.parse(html))
        assertNull(com.supernova.launcher.store.PlayPage.parse("<html>nothing</html>"))
    }

    @Test
    fun unknownUntilStoreAnswers() {
        val cache = cache()
        assertNull(cache.info("com.a"))
        cache.putPlay("com.a", "TOOLS")
        assertEquals(StoreInfo("TOOLS", emptyList()), cache.info("com.a"))
        cache.putPlay("com.b", null)
        assertEquals(StoreInfo(null, emptyList()), cache.info("com.b"))
    }

    @Test
    fun answersSurviveARestartAndAreNotAskedAgainWhileFresh() {
        cache().putPlay("com.a", "TOOLS")
        cache().putPlay("com.b", null)
        val later = cache()
        assertEquals(listOf("com.c"), later.playDue(listOf("com.a", "com.b", "com.c")) { false })
        now += StoreCache.NOT_FOUND_TTL_MS + 1
        assertEquals(listOf("com.b", "com.c"), later.playDue(listOf("com.a", "com.b", "com.c")) { false })
        now += StoreCache.FOUND_TTL_MS
        assertEquals(listOf("com.a", "com.b"), later.playDue(listOf("com.a", "com.b")) { false })
        // Stale answers still serve.
        assertEquals("TOOLS", later.info("com.a")?.playId)
    }

    @Test
    fun failureWaitsADayAndKeepsOldAnswer() {
        val cache = cache()
        cache.putPlay("com.a", "TOOLS")
        now += StoreCache.FOUND_TTL_MS + 1
        cache.putPlayFailure("com.a")
        assertTrue(cache.playDue(listOf("com.a")) { false }.isEmpty())
        assertEquals("TOOLS", cache.info("com.a")?.playId)
        now += StoreCache.RETRY_MS + 1
        assertEquals(listOf("com.a"), cache.playDue(listOf("com.a")) { false })
    }

    @Test
    fun fdroidNarrowSkipsPlayLookup() {
        val cache = cache()
        cache.putFdroid(StoreCache.FdroidIndex(now, "e", mapOf("org.x" to listOf("Podcast"), "org.y" to listOf("System"))))
        val due = cache().playDue(listOf("org.x", "org.y", "org.z")) { cats -> cats.any { it != "System" } }
        assertEquals(listOf("org.y", "org.z"), due)
        assertEquals(StoreInfo(null, listOf("Podcast")), cache().info("org.x"))
        assertEquals("e", cache().fdroidEtag())
    }
}
