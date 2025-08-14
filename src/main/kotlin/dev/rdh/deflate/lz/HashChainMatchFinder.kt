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
 * @param sameLenProbe The number of extra probes to allow for matches of the same length code (default is 32).
 */
class HashChainMatchFinder(
    private val windowSize: Int = 32 * 1024,
    hashBits: Int = 15,
    private val probeLimit: Int = 256,
    private val maxPerPos: Int = 24,
    private val niceLen: Int = 64,
    private val sameLenProbe: Int = 32
) : MatchFinder {

    private lateinit var s: ByteArray
    private lateinit var prev: IntArray   // prev[i] = previous pos with same hash, or -1
    private val hashSize = 1 shl hashBits

    // --- hashing (3-byte) ---
    private fun hash3(i: Int): Int {
        val a = s[i].toInt() and 0xFF
        val b = s[i + 1].toInt() and 0xFF
        val c = s[i + 2].toInt() and 0xFF
        // simple mixed 3-byte hash
        var h = (a * 0x1e35a7bd + b * 0x9e3779b1 + c * 0x85ebca6b).toInt()
        h = h xor (h ushr 13)
        return h and (hashSize - 1)
    }

    override fun reset(input: ByteArray) {
        s = input
        if (s.size < 3) {
            prev = IntArray(s.size) { -1 }
            return
        }

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
        if (index < 0 || index + 2 >= s.size) return emptyList()

        val maxLenAtI = min(258, s.size - index)
        val out = ObjectArrayList<Match>(min(12, maxPerPos))

        val buckets = 29 * 32 // 29 len codes (0..28) x 30 dist codes (0..29); use 32 stride
        val posByBucket = IntArray(buckets) { -1 }
        val bestLenByBucket = IntArray(buckets) { 0 }
        val bestDistByBucket = IntArray(buckets) { Int.MAX_VALUE }

        fun bucketIndex(len: Int, dist: Int): Int {
            val lc = Tables.lenCodeIndex(len)  // 0..28
            val dc = Tables.distCodeIndex(dist) // 0..29
            return (lc shl 5) or dc
        }

        fun tryStore(len: Int, dist: Int) {
            val bi = bucketIndex(len, dist)
            val curLen = bestLenByBucket[bi]
            val curDist = bestDistByBucket[bi]
            val newDc = Tables.distCodeIndex(dist)
            val curDc = if (curDist == Int.MAX_VALUE) Int.MAX_VALUE else Tables.distCodeIndex(curDist)
            val better = len > curLen || (len == curLen && newDc < curDc) || (len == curLen && newDc == curDc && dist < curDist)
            if (posByBucket[bi] == -1) {
                // new bucket
                posByBucket[bi] = out.size
                bestLenByBucket[bi] = len
                bestDistByBucket[bi] = dist
                out += Match(len, dist)
            } else if (better) {
                // replace existing entry in 'out'
                val pos = posByBucket[bi]
                bestLenByBucket[bi] = len
                bestDistByBucket[bi] = dist
                out[pos] = Match(len, dist)
            }
        }

        fun readU32(off: Int): Int =
            (s[off].toInt() and 0xFF) or
                    ((s[off + 1].toInt() and 0xFF) shl 8) or
                    ((s[off + 2].toInt() and 0xFF) shl 16) or
                    ((s[off + 3].toInt() and 0xFF) shl 24)

        fun matchLen(a: Int, b: Int, max: Int): Int {
            var l = 0
            // 4-byte blocks
            while (l + 4 <= max && readU32(a + l) == readU32(b + l)) {
                l += 4
            }
            // tail bytes
            while (l < max && s[a + l] == s[b + l]) {
                l++
            }
            return l
        }

        var bestLen = 0
        var bestLc = -1
        var extraScan = 0 // allow extra steps after hitting a nice lenCode to look for cheaper distance

        var p = prev[index]
        var steps = 0
        var probeLimit = probeLimit

        // adapt probe limit a bit if we quickly find long matches
        fun adaptProbe(len: Int) {
            if (len >= 32) probeLimit += 64
            if (len >= 64) probeLimit += 64
        }

        while (p >= 0 && steps++ < probeLimit) {
            val dist = index - p
            if (dist > windowSize) break

            // Quick 3-byte check
            if (s[p] == s[index] && s[p + 1] == s[index + 1] && s[p + 2] == s[index + 2]) {
                var len = 3
                if (maxLenAtI >= 7) {
                    len = matchLen(p, index, maxLenAtI)
                } else {
                    // tiny tail
                    while (len < maxLenAtI && s[p + len] == s[index + len]) len++
                }

                if (len >= 3) {
                    tryStore(len, dist)
                    if (len > bestLen) {
                        bestLen = len
                        bestLc = Tables.lenCodeIndex(len)
                        adaptProbe(len)
                        // start a small extra scan budget once we hit a "nice length"
                        if (len >= niceLen) extraScan = sameLenProbe
                    } else if (bestLc >= 0) {
                        // keep scanning a bit for same-lencode but cheaper distance
                        val lc = Tables.lenCodeIndex(len)
                        if (lc == bestLc && extraScan > 0) {
                            extraScan--
                        }
                    }

                    if (out.size >= maxPerPos && extraScan == 0 && bestLen >= niceLen) break
                }
            }
            p = prev[p]
        }

        return out
    }
}
