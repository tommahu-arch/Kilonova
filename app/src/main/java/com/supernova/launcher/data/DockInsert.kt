package com.supernova.launcher.data

// Dropping an icon on a dock slot that already holds one makes room instead
// of trading places: the neighbours slide one slot along, towards the nearest
// free slot of the same dock page, and the dropped icon takes the freed one.
// Pure, so the drag's preview and the model write agree (and tests can pin it).
object DockInsert {

    // The new ranks of the icons that move so that `rank` is free for the
    // dropped item: item id to its new rank. Empty when the slot is already
    // free; null when the page has no free slot (the caller falls back to the
    // old trade). occupied: every dock item's id and rank, all pages.
    // movingId: the dropped item when it is already in the dock - its own slot
    // counts as free, since it is leaving it. count: slots per dock page.
    fun plan(occupied: Map<String, Int>, movingId: String?, rank: Int, count: Int): Map<String, Int>? {
        if (count <= 0 || rank < 0) return null
        val base = rank / count * count
        val slots = arrayOfNulls<String>(count)
        for ((id, r) in occupied) {
            if (id != movingId && r in base until base + count) slots[r - base] = id
        }
        val at = rank - base
        if (slots[at] == null) return emptyMap()

        var left = -1
        for (i in at - 1 downTo 0) if (slots[i] == null) { left = i; break }
        var right = -1
        for (i in at + 1 until count) if (slots[i] == null) { right = i; break }
        if (left < 0 && right < 0) return null

        val from = movingId?.let { occupied[it] }?.takeIf { it in base until base + count }?.minus(base)
        val goRight = when {
            left < 0 -> true
            right < 0 -> false
            at - left != right - at -> right - at < at - left
            // A tie: the way the dragged icon came from, so a neighbour
            // swap reads as one.
            else -> from?.let { it > at } ?: true
        }

        val moves = LinkedHashMap<String, Int>()
        if (goRight) {
            for (i in at until right) slots[i]?.let { moves[it] = base + i + 1 }
        } else {
            for (i in at downTo left + 1) slots[i]?.let { moves[it] = base + i - 1 }
        }
        return moves
    }
}
