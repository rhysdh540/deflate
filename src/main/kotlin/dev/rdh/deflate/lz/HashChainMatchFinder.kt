package dev.rdh.deflate.lz

import dev.rdh.deflate.core.Match
import dev.rdh.deflate.core.Tables

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlin.math.min

/**
 * A match finder that uses a hash chain to find matches in a byte array.
 * It builds a hash table of positions for 3-byte hashes and probes
 * previous positions to find matches efficiently.
 *
 * @param windowSize The size of the sliding window for matching.
 * @param hashBits The number of bits used for the hash table size (default is 15 -> 32,768 entries).
 * @param probeLimit The maximum number of steps to probe for matches (default is 256).
 * @param maxPerPos The maximum number of matches to return per position (default is 24).
 * @param niceLen The length at which to stop probing early if a good match is found (default is 64).
 */
class HashChainMatchFinder(
    private val windowSize: Int = 32 * 1024,
    hashBits: Int = 15,
    private val probeLimit: Int = 256,
    private val maxPerPos: Int = 24,
    private val niceLen: Int = 64
) : MatchFinder {

    private lateinit var s: ByteArray
    private lateinit var prev: IntArray   // prev[i] = previous pos with same hash, or -1
    private val hashSize = 1 shl hashBits

    private fun hash3(i: Int): Int {
        val a = s[i].toInt() and 0xFF
        val b = s[i + 1].toInt() and 0xFF
        val c = s[i + 2].toInt() and 0xFF
        var h = (a * 0x1e35a7bd + b * 0x9e3779b1 + c * 0x85ebca6b).toInt()
        h = h xor (h ushr 13)
        return h and (hashSize - 1)
    }

    override fun reset(input: ByteArray) {
        s = input
        if (s.size < 3) { prev = IntArray(s.size) { -1 }; return }

        // build prev[] in one pass: for each position i, link to previous with same hash
        prev = IntArray(s.size) { -1 }
        val head = IntArray(hashSize) { -1 }
        val end = s.size - 3 // last index with a valid 3-byte hash start
        var i = 0
        while (i <= end) {
            val h = hash3(i)
            prev[i] = head[h]
            head[h] = i
            i++
        }
    }

    override fun matchesAt(index: Int): List<Match> {
        // we need at least 3 bytes ahead for a match
        if (index < 0 || index + 2 >= s.size) return emptyList()

        val maxLenAtI = min(258, s.size - index)
        val out = ObjectArrayList<Match>(min(8, maxPerPos))

        // keep one best per (lenCode, distCode) bucket + overall longest
        val buckets = 29 * 32 // len codes 0..28, dist codes 0..29 (round dist to 32 for index math)
        val seen = BooleanArray(buckets)
        var bestLen = 0
        fun bucketIndex(len: Int, dist: Int): Int {
            val lc = Tables.lenCodeIndex(len)  // 0..28
            val dc = Tables.distCodeIndex(dist) // 0..29
            return (lc shl 5) or dc
        }

        fun addCandidate(len: Int, dist: Int) {
            val idx = bucketIndex(len, dist)
            if (!seen[idx]) {
                seen[idx] = true
                out.add(Match(len, dist))
                if (len > bestLen) bestLen = len
            }
        }

        var p = prev[index] // start from the previous pos with same hash (fast!)
        var steps = 0

        while (p >= 0 && steps++ < probeLimit) {
            val dist = index - p
            if (dist > windowSize) break // older entries only get farther

            // fast first-byte check (saves length loop when hash collided)
            if (s[p] == s[index] && s[p + 1] == s[index + 1] && s[p + 2] == s[index + 2]) {
                var len = 3
                // extend match
                while (len < maxLenAtI && s[p + len] == s[index + len]) len++

                // add the exact length
                addCandidate(len, dist)

                // also add boundary lengths that change length-code (helps dp without many candidates)
                // emit a couple of boundaries just below the best length to give choices
                var b = len - 1
                var added = 0
                while (b >= 3 && added < 2) {
                    val lcHere = Tables.lenCodeIndex(b)
                    val lcBest = Tables.lenCodeIndex(len)
                    if (lcHere != lcBest) { addCandidate(b, dist); added++ }
                    b--
                }

                if (len >= niceLen) break // early stop on a very good match
                if (out.size >= maxPerPos) break
            }
            p = prev[p]
        }

        return out
    }
}
