package com.supernova.launcher.data

// What the stores say about one installed package: the Play category its
// page names (null: none found) and its F-Droid categories (empty: not on
// F-Droid). Null from the cache instead means nothing is known yet.
data class StoreInfo(val playId: String?, val fdroid: List<String>)

// Which drawer group an app joins by its store categories.
//
// Order of trust: an F-Droid narrow category, then the Play category, then an
// F-Droid broad one. The first of them that a group has taken decides; a
// category no group has taken is passed over. An app left with none joins
// the group holding "Samsung built-in apps" when it is a com.samsung.* one,
// else the group holding "Unsorted apps". An app nothing is known about yet
// joins nothing.
//
// Added for Kilonova; not part of upstream Supernova.
object StoreSorting {

    // The keys an app can be sorted by, most trusted first.
    fun candidates(info: StoreInfo): List<String> {
        val (broad, narrow) = info.fdroid.partition(StoreCategories::isBroad)
        return narrow.map(StoreCategories::fdroid) +
            listOfNotNull(info.playId?.let(StoreCategories::play)) +
            broad.map(StoreCategories::fdroid)
    }

    // The key an app is sorted under among the keys some group holds
    // (owned), or null.
    fun keyFor(packageName: String, info: StoreInfo, owned: Set<String>): String? {
        candidates(info).firstOrNull { it in owned }?.let { return it }
        if (packageName.startsWith(SAMSUNG_PREFIX) && StoreCategories.SAMSUNG in owned) {
            return StoreCategories.SAMSUNG
        }
        return StoreCategories.UNSORTED.takeIf { it in owned }
    }

    // Each group's store-sorted members, as app keys. Apps picked into a
    // group by hand (manual: the keys in any group's members) stay where
    // they were put.
    fun autoMembers(
        groups: List<DrawerGroup>,
        apps: List<App>,
        manual: Set<String>,
        infoOf: (String) -> StoreInfo?
    ): Map<String, Set<String>> {
        val owner = HashMap<String, String>()
        groups.forEach { group -> group.storeCats.forEach { owner.putIfAbsent(it, group.name) } }
        if (owner.isEmpty()) return emptyMap()
        val owned = owner.keys
        val result = HashMap<String, MutableSet<String>>()
        for (app in apps) {
            if (app.key in manual) continue
            val info = infoOf(app.packageName) ?: continue
            val key = keyFor(app.packageName, info, owned) ?: continue
            result.getOrPut(owner.getValue(key)) { HashSet() }.add(app.key)
        }
        return result
    }

    // The groups with their store-sorted apps added to their members.
    fun withAuto(groups: List<DrawerGroup>, auto: Map<String, Set<String>>): List<DrawerGroup> =
        if (auto.isEmpty()) groups else groups.map { group ->
            auto[group.name]?.let { group.copy(members = group.members + it) } ?: group
        }

    // A key moved to a group: it leaves the group that held it.
    fun assign(groups: List<DrawerGroup>, name: String, keys: Set<String>): List<DrawerGroup> =
        groups.map { group ->
            when {
                group.name == name -> group.copy(storeCats = group.storeCats + keys, smart = false)
                group.storeCats.any { it in keys } -> group.copy(storeCats = group.storeCats - keys, smart = false)
                else -> group
            }
        }

    // The key of a group's apps (hiddenKey), with its package for the lookup.
    class App(val key: String, val packageName: String)

    private const val SAMSUNG_PREFIX = "com.samsung."
}
