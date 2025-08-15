package dev.rdh.deflate.core

object Tables {
    // length codes 257..285 (index 0..28)
    val LEN_BASE = intArrayOf(
        3, 4, 5, 6, 7, 8, 9, 10,
        11, 13, 15, 17,
        19, 23, 27, 31,
        35, 43, 51, 59,
        67, 83, 99, 115,
        131, 163, 195, 227,
        258
    )
    val LEN_EXTRA = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0,
        1, 1, 1, 1, 2, 2, 2, 2,
        3, 3, 3, 3,
        4, 4, 4, 4,
        5, 5, 5, 5,
        0
    )

    // distance codes 0..29
    val DIST_BASE = intArrayOf(
        1, 2, 3, 4,
        5, 7, 9, 13,
        17, 25, 33, 49,
        65, 97, 129, 193,
        257, 385, 513, 769,
        1025, 1537, 2049, 3073,
        4097, 6145, 8193, 12289, 16385, 24577
    )
    val DIST_EXTRA = intArrayOf(
        0, 0, 0, 0,
        1, 1, 2, 2,
        3, 3, 4, 4,
        5, 5, 6, 6,
        7, 7, 8, 8,
        9, 9, 10, 10,
        11, 11, 12, 12,
        13, 13
    )
    val CODELENGTH_ORDER = intArrayOf(
        16, 17, 18,
        0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15
    )

    val LEN_TO_INDEX: IntArray = IntArray(259) { -1 }
    val DIST_TO_INDEX: IntArray = IntArray(32768 + 1) { -1 }

    init {
        for (idx in 0..28) {
            val base = LEN_BASE[idx]
            val ext  = LEN_EXTRA[idx]
            val max  = base + ((1 shl ext) - 1)
            for (l in base..max) {
                // let higher indices overwrite (so 258 ends up with idx=28)
                if (l in 3..258) LEN_TO_INDEX[l] = idx
            }
        }

        for (idx in 0..29) {
            val base = DIST_BASE[idx]
            val ext  = DIST_EXTRA[idx]
            val max  = base + ((1 shl ext) - 1)
            val upper = minOf(max, DIST_TO_INDEX.lastIndex)
            for (d in base..upper) {
                DIST_TO_INDEX[d] = idx
            }
        }
    }

    fun lenCodeIndex(len: Int): Int {
        require(len in 3..258) { "Length must be in range 3..258, got $len" }
        return LEN_TO_INDEX[len]
    }

    fun distCodeIndex(dist: Int): Int {
        require(dist in 1..32768) { "Distance must be in range 1..32768, got $dist" }
        return DIST_TO_INDEX[dist]
    }
}