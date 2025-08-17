package dev.rdh.deflate.lz

import dev.rdh.deflate.core.Match
import dev.rdh.deflate.core.Tables
import dev.rdh.deflate.util.MatchList
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

abstract class CachingMatchFinder : MatchFinder {

    protected val cache = Int2ObjectOpenHashMap<MatchList>()

    protected abstract fun findAt(index: Int): MatchList

    override fun reset(input: ByteArray) {
        cache.clear()
    }

    override fun matchesAt(index: Int): MatchList {
        return cache.getOrPut(index) { findAt(index) }
    }
}

/**
 * Builds a list of sub-length candidates from a raw list of matches.
 * This processes the raw matches to find the best candidates
 * for each length code, considering the cheapest/shortest distance
 * by [distCodeIndex][Tables.distCodeIndex], then by distance.
 * I put it here because I have no idea where else to put it.
 *
 * @param raw The raw list of matches to process.
 * @return A new [MatchList] containing the best sub-length candidates.
 */
fun buildSublenCandidates(raw: MatchList): MatchList {
    if (raw.isEmpty()) return raw
    val out = MatchList()

    val bestDistCodes = IntArray(29) { Int.MAX_VALUE } // per length code
    val bestDists = IntArray(29) { Int.MAX_VALUE } // tiebreaker
    val maxLengths = IntArray(29) { 0 } // max feasible length per length code

    var i = 0
    val size = raw.size
    while (i < size) {
        val (len, dist) = raw[i]
        val distCode = Tables.distCodeIndex(dist)
        val lcMax = Tables.lenCodeIndex(len)
        var lc = 0
        while (lc <= lcMax) {
            val base = Tables.LEN_BASE[lc]
            val extra = Tables.LEN_EXTRA[lc]
            val cap = base + ((1 shl extra) - 1) // max len that still has code equal to `lc`
            val feasibleMax = if (len < cap) len else cap
            if (feasibleMax > maxLengths[lc]) {
                maxLengths[lc] = feasibleMax
            }

            val curDistCode = bestDistCodes[lc]
            val curDist = bestDists[lc]
            if (distCode < curDistCode || (distCode == curDistCode && dist < curDist)) {
                bestDistCodes[lc] = distCode
                bestDists[lc] = dist
            }
            lc++
        }
        i++
    }

    // output at most two candidates per lenCode: max and base (if different)
    var emitted = 0
    var lc = 0
    while (lc < 29) {
        val maxLen = maxLengths[lc]
        if (maxLen != 0) {
            val bestDist = bestDists[lc]
            out.add(Match(maxLen, bestDist))
            val base = Tables.LEN_BASE[lc]
            if (maxLen != base) out.add(Match(base, bestDist))
            emitted += if (maxLen != base) 2 else 1
        }
        lc++
    }

    return out
}