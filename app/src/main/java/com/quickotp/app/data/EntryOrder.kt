package com.quickotp.app.data

object EntryOrder {
    /**
     * Returns [ids] with [id] moved [offset] places (negative is up), or null when [id]
     * is missing or would move past either end.
     */
    fun move(ids: List<Long>, id: Long, offset: Int): List<Long>? {
        val from = ids.indexOf(id)
        if (from < 0) return null
        val to = from + offset
        if (to !in ids.indices || to == from) return null
        return ids.toMutableList().apply { add(to, removeAt(from)) }
    }
}
