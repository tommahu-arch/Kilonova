package com.supernova.launcher.data

// The groups "Add Play Store groups" makes: one per Play category, named as
// Play names it and holding its Play id and the F-Droid categories mapped to
// it, then "Samsung built-in apps" and "Unsorted apps". A category some group
// already holds stays with that group, and a group of the same name only
// gains what is free, so running it again changes nothing the user set.
//
// Added for Kilonova; not part of upstream Supernova.
object StorePresets {

    fun apply(groups: List<DrawerGroup>, type: DrawerGroupType): List<DrawerGroup> {
        val taken = groups.flatMapTo(HashSet()) { it.storeCats }
        val made = LinkedHashMap<String, DrawerGroup>()
        groups.forEach { made[it.name] = it }
        var order = groups.map { it.order }.filter { it != Int.MAX_VALUE }.maxOrNull()?.plus(1) ?: 0

        fun add(name: String, keys: List<String>) {
            val free = keys.filter { taken.add(it) }.toSet()
            val existing = made[name]
            when {
                existing != null -> made[name] = existing.copy(storeCats = existing.storeCats + free)
                else -> made[name] = DrawerGroup(name, emptySet(), order = order++, type = type, storeCats = free)
            }
        }

        StoreCategories.presets.forEach { preset ->
            add(
                preset.name,
                preset.play.map(StoreCategories::play) + preset.fdroid.map(StoreCategories::fdroid)
            )
        }
        add(StoreCategories.SAMSUNG_NAME, listOf(StoreCategories.SAMSUNG))
        add(StoreCategories.UNSORTED_NAME, listOf(StoreCategories.UNSORTED))
        return made.values.toList()
    }
}
