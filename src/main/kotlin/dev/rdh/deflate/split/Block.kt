package dev.rdh.deflate.split

import dev.rdh.deflate.core.Literal
import dev.rdh.deflate.core.Match
import dev.rdh.deflate.core.Token

data class Block(val start: Int, val end: Int) {
    companion object {
        // bytesPrefix[k] = total uncompressed bytes covered by tokens[0 until k]
        fun bytesPrefix(tokens: List<Token>): IntArray {
            val pref = IntArray(tokens.size + 1)
            var sum = 0
            for (i in tokens.indices) {
                sum += when (val t = tokens[i]) {
                    is Literal -> 1
                    is Match -> t.len
                }
                pref[i + 1] = sum
            }
            return pref
        }

        // Map token blocks to byte [start,end) ranges
        fun toByteRanges(blocks: List<Block>, pref: IntArray): List<IntRange> =
            blocks.map { b ->
                val startByte = pref[b.start]
                val endByte   = pref[b.end]
                startByte until endByte
            }
    }
}