package com.supernova.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreSortingTest {

    private fun group(name: String, vararg keys: String, members: Set<String> = emptySet()) =
        DrawerGroup(name, members, storeCats = keys.toSet())

    private fun app(pkg: String) = StoreSorting.App("0/$pkg/Main", pkg)

    @Test
    fun fdroidNarrowBeatsPlayBeatsFdroidBroad() {
        val info = StoreInfo("MUSIC_AND_AUDIO", listOf("Multimedia", "Podcast"))
        assertEquals(
            listOf("fdroid:Podcast", "play:MUSIC_AND_AUDIO", "fdroid:Multimedia"),
            StoreSorting.candidates(info)
        )
    }

    @Test
    fun firstCandidateAGroupHoldsWins() {
        val info = StoreInfo("MUSIC_AND_AUDIO", listOf("Podcast"))
        // Nobody holds the narrow one: Play decides.
        assertEquals("play:MUSIC_AND_AUDIO", StoreSorting.keyFor("x", info, setOf("play:MUSIC_AND_AUDIO")))
        assertEquals(
            "fdroid:Podcast",
            StoreSorting.keyFor("x", info, setOf("play:MUSIC_AND_AUDIO", "fdroid:Podcast"))
        )
    }

    @Test
    fun samsungAppsFallBackToTheirGroupThenUnsorted() {
        val none = StoreInfo(null, emptyList())
        val both = setOf(StoreCategories.SAMSUNG, StoreCategories.UNSORTED)
        assertEquals(StoreCategories.SAMSUNG, StoreSorting.keyFor("com.samsung.android.x", none, both))
        assertEquals(StoreCategories.UNSORTED, StoreSorting.keyFor("com.other", none, both))
        assertEquals(
            StoreCategories.UNSORTED,
            StoreSorting.keyFor("com.samsung.android.x", none, setOf(StoreCategories.UNSORTED))
        )
        assertNull(StoreSorting.keyFor("com.other", none, setOf(StoreCategories.SAMSUNG)))
    }

    @Test
    fun samsungAppWithAStoreCategoryGoesThere() {
        val info = StoreInfo("PRODUCTIVITY", emptyList())
        assertEquals(
            "play:PRODUCTIVITY",
            StoreSorting.keyFor(
                "com.samsung.android.calendar", info,
                setOf("play:PRODUCTIVITY", StoreCategories.SAMSUNG)
            )
        )
    }

    @Test
    fun unknownAppsAndHandPickedAppsAreLeftAlone() {
        val groups = listOf(
            group("Music", "play:MUSIC_AND_AUDIO"),
            group("Mine", members = setOf("0/com.b/Main")),
            group("Rest", StoreCategories.UNSORTED)
        )
        val infos = mapOf(
            "com.a" to StoreInfo("MUSIC_AND_AUDIO", emptyList()),
            "com.b" to StoreInfo("MUSIC_AND_AUDIO", emptyList()),
            "com.d" to StoreInfo(null, emptyList())
        )
        val auto = StoreSorting.autoMembers(
            groups, listOf(app("com.a"), app("com.b"), app("com.c"), app("com.d")),
            manual = setOf("0/com.b/Main")
        ) { infos[it] }
        assertEquals(setOf("0/com.a/Main"), auto["Music"])
        // com.b was picked by hand; com.c is not known yet.
        assertEquals(setOf("0/com.d/Main"), auto["Rest"])
        assertNull(auto["Mine"])
    }

    @Test
    fun withAutoAddsToMembers() {
        val groups = listOf(group("Music", "play:X", members = setOf("0/a/Main")))
        val merged = StoreSorting.withAuto(groups, mapOf("Music" to setOf("0/b/Main")))
        assertEquals(setOf("0/a/Main", "0/b/Main"), merged.single().members)
    }

    @Test
    fun assignTakesTheKeyFromItsFormerGroup() {
        val groups = listOf(group("A", "play:X", "play:Y"), group("B"))
        val moved = StoreSorting.assign(groups, "B", setOf("play:Y"))
        assertEquals(setOf("play:X"), moved[0].storeCats)
        assertEquals(setOf("play:Y"), moved[1].storeCats)
    }

    @Test
    fun presetsKeepEveryKeyInOneGroupOnly() {
        val made = StorePresets.apply(emptyList(), DrawerGroupType.FOLDER)
        val keys = made.flatMap { it.storeCats }
        assertEquals(keys.size, keys.toSet().size)
        assertEquals(StoreCategories.presets.size + 2, made.size)
        assertEquals(setOf("play:ART_AND_DESIGN", "fdroid:Draw", "fdroid:Graphics"), made.first { it.name == "Art & Design" }.storeCats)
        assertTrue(made.any { it.name == "Unsorted apps" && StoreCategories.UNSORTED in it.storeCats })
    }

    @Test
    fun presetsNeverTakeFromOrRewriteWhatIsThere() {
        val mine = DrawerGroup("Mine", emptySet(), order = 0, storeCats = setOf("play:TOOLS"))
        val made = StorePresets.apply(listOf(mine), DrawerGroupType.TAB)
        assertEquals(setOf("play:TOOLS"), made.first { it.name == "Mine" }.storeCats)
        assertTrue("play:TOOLS" !in made.first { it.name == "Tools" }.storeCats)
        val again = StorePresets.apply(made, DrawerGroupType.TAB)
        assertEquals(made, again)
    }

    @Test
    fun everyPresetFdroidNameExists() {
        val known = StoreCategories.fdroidNarrow.toSet() + StoreCategories.fdroidBroad
        StoreCategories.presets.flatMap { it.fdroid }.forEach { assertTrue(it, it in known) }
    }

    @Test
    fun storeCategoriesRoundTripThroughTheCodec() {
        val original = DrawerGroup(
            "Tools", setOf("0/a/Main"), order = 3, type = DrawerGroupType.FOLDER,
            storeCats = setOf("play:TOOLS", "fdroid:Calendar & Agenda", "fdroid:We,ird|One")
        )
        assertEquals(original, DrawerGroup.decode(original.encode()))
        val plain = DrawerGroup("P", emptySet(), storeCats = setOf("play:TOOLS"))
        assertEquals(plain, DrawerGroup.decode(plain.encode()))
        assertEquals(emptySet<String>(), DrawerGroup.decode("Old|0/a/Main|1|TAB||keep")!!.storeCats)
    }
}
