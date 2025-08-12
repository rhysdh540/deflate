package dev.rdh.deflate.core

import it.unimi.dsi.fastutil.ints.IntArrayList

object Tables {
    // length codes 257..285 (index 0..28)
    val LEN_BASE = intArrayOf(
        3,4,5,6,7,8,9,10, 11,13,15,17, 19,23,27,31,
        35,43,51,59, 67,83,99,115, 131,163,195,227, 258
    )
    val LEN_EXTRA = intArrayOf(
        0,0,0,0,0,0,0,0, 1,1,1,1, 2,2,2,2,
        3,3,3,3, 4,4,4,4, 5,5,5,5, 0
    )
    // distance codes 0..29
    val DIST_BASE = intArrayOf(
        1,2,3,4, 5,7,9,13, 17,25,33,49, 65,97,129,193,
        257,385,513,769, 1025,1537,2049,3073, 4097,6145,8193,12289,16385,24577
    )
    val DIST_EXTRA = intArrayOf(
        0,0,0,0, 1,1,2,2, 3,3,4,4, 5,5,6,6,
        7,7,8,8, 9,9,10,10, 11,11,12,12, 13,13
    )

    val lengthBoundaries: IntArray by lazy {
        // boundaries where length code/extra changes (helpful for pruning)
        val b = IntArrayList()
        for (i in 0 until LEN_BASE.size) {
            val base = LEN_BASE[i]
            val span = 1 shl LEN_EXTRA[i]
            for (k in 0 until span) b.add(base + k)
        }
        b.distinct().sorted().toIntArray()
    }

    fun lenCodeIndex(len: Int): Int {
        if (len == 258) return 28
        for (i in 0 until 28) {
            val base = LEN_BASE[i]
            val span = 1 shl LEN_EXTRA[i]
            val hi = base + span - 1
            if (len in base..hi) return i
        }
        return 28
    }

    fun distCodeIndex(dist: Int): Int {
        for (i in 0 until DIST_BASE.size) {
            val base = DIST_BASE[i]
            val span = 1 shl DIST_EXTRA[i]
            val hi = base + span - 1
            if (dist in base..hi) return i
        }
        return DIST_BASE.lastIndex
    }
}