package com.quickotp.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntryOrderTest {
    private val ids = listOf(10L, 20L, 30L, 40L)

    @Test
    fun move_up_swapsWithPrevious() {
        assertEquals(listOf(10L, 30L, 20L, 40L), EntryOrder.move(ids, 30L, -1))
    }

    @Test
    fun move_down_swapsWithNext() {
        assertEquals(listOf(20L, 10L, 30L, 40L), EntryOrder.move(ids, 10L, 1))
    }

    @Test
    fun move_firstUp_isNull() {
        assertNull(EntryOrder.move(ids, 10L, -1))
    }

    @Test
    fun move_lastDown_isNull() {
        assertNull(EntryOrder.move(ids, 40L, 1))
    }

    @Test
    fun move_unknownId_isNull() {
        assertNull(EntryOrder.move(ids, 99L, 1))
    }

    @Test
    fun move_zero_isNull() {
        assertNull(EntryOrder.move(ids, 20L, 0))
    }
}
