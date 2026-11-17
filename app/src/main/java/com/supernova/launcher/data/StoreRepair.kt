package com.supernova.launcher.data

// Version 0.0.6 stored a group's store categories where version 0.0.7 keeps
// its id, so groups saved by 0.0.6 came out of the update with the category
// list as their id and no categories. DrawerGroup.decode now reads them
// right; this rewrites the stored entries once so ids are ids again, and
// settles keys two groups hold after "Add Play Store groups" ran on the
// emptied groups: the group that had the key in 0.0.6 keeps it.
//
// Added for Kilonova; not part of upstream Supernova.
object StoreRepair {

    class Result(val entries: Set<String>, val defaultTab: String)

    // Null when no entry needs it.
    fun repair(stored: Set<String>, defaultTab: String): Result? {
        val legacyIds = HashMap<String, String>()
        val priority = HashMap<String, String>()
        stored.forEach { raw ->
            val field = DrawerGroup.legacyStoreField(raw) ?: return@forEach
            val group = DrawerGroup.decode(raw) ?: return@forEach
            legacyIds[field] = group.name
            field.split(',').filter { it.isNotBlank() }.map { StoreCategories.unescape(it) }
                .forEach { priority.putIfAbsent(it, group.name) }
        }
        if (legacyIds.isEmpty()) return null
        val decoded = stored.mapNotNull { DrawerGroup.decode(it) }
        val out = decoded.map { group ->
            group.copy(
                storeCats = group.storeCats.filter { priority[it] == null || priority[it] == group.name }.toSet()
            )
        }
        val entries = out.map { it.encode() }.toSet() + stored.filter { DrawerGroup.decode(it) == null }
        return Result(entries, defaultTab)
    }
}
