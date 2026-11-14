package com.supernova.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DockInsertTest {

    @Test
    fun aFreeSlotMovesNothing() {
        assertEquals(emptyMap<String, Int>(), DockInsert.plan(mapOf("a" to 0, "b" to 1), null, 3, 5))
    }

    @Test
    fun anOccupiedSlotPushesNeighboursTowardsTheNearestGap() {
        // a b c _ _ : dropping at slot 1 pushes b and c right.
        val plan = DockInsert.plan(mapOf("a" to 0, "b" to 1, "c" to 2), null, 1, 5)
        assertEquals(mapOf("b" to 2, "c" to 3), plan)
    }

    @Test
    fun theNearerGapWins() {
        // _ a b c d : dropping at slot 3 closes on the gap at 0 only by 3
        // steps; slot 4 is full, so it goes left.
        val plan = DockInsert.plan(mapOf("a" to 1, "b" to 2, "c" to 3, "d" to 4), null, 3, 5)
        assertEquals(mapOf("a" to 0, "b" to 1, "c" to 2), plan)
    }

    @Test
    fun aDockIconLeavingItsSlotLeavesTheGap() {
        // a b c d e, drag a onto slot 3: b, c, d close up leftwards.
        val all = mapOf("a" to 0, "b" to 1, "c" to 2, "d" to 3, "e" to 4)
        assertEquals(mapOf("b" to 0, "c" to 1, "d" to 2), DockInsert.plan(all, "a", 3, 5))
        // and e dragged onto slot 1 pushes b, c, d right.
        assertEquals(mapOf("b" to 2, "c" to 3, "d" to 4), DockInsert.plan(all, "e", 1, 5))
    }

    @Test
    fun aFullPageWithNoOutsiderHasNoRoom() {
        val all = mapOf("a" to 0, "b" to 1, "c" to 2)
        assertNull(DockInsert.plan(all, null, 1, 3))
    }

    @Test
    fun otherPagesAreLeftAlone() {
        // Two slots a page: page 0 is full, page 1 holds c at rank 2.
        val all = mapOf("a" to 0, "b" to 1, "c" to 2)
        assertNull(DockInsert.plan(all, null, 0, 2))
        assertEquals(mapOf("c" to 3), DockInsert.plan(all, null, 2, 2))
    }
}
