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

    val maxLengths = IntArray(29) { 0 } // best feasible length for lc
    val maxDists = IntArray(29) { Int.MAX_VALUE } // distance paired with maxLengths[lc]
    val baseDists = IntArray(29) { Int.MAX_VALUE } // best distance for base length of lc
    val haveMax = BooleanArray(29)
    val haveBase = BooleanArray(29)

    fun Int.betterThan(curDist: Int): Boolean {
        val newDc = Tables.distCodeIndex(this)
        val curDc = if (curDist == Int.MAX_VALUE) Int.MAX_VALUE else Tables.distCodeIndex(curDist)
        return newDc < curDc || (newDc == curDc && this < curDist)
    }

    var i = 0
    val size = raw.size
    while (i < size) {
        val (len, dist) = raw[i]
        val lcMax = Tables.lenCodeIndex(len)
        var lc = 0
        while (lc <= lcMax) {
            val base = Tables.LEN_BASE[lc]
            val extra = Tables.LEN_EXTRA[lc]
            val cap = base + ((1 shl extra) - 1) // max len that still has code equal to `lc`
            val feasibleMax = if (len < cap) len else cap

            if (!haveMax[lc]) {
                haveMax[lc] = true
                maxLengths[lc] = feasibleMax
                maxDists[lc] = dist
            } else {
                val curLen = maxLengths[lc]
                val curDist = maxDists[lc]
                if (feasibleMax > curLen || (feasibleMax == curLen && dist.betterThan(curDist))) {
                    maxLengths[lc] = feasibleMax
                    maxDists[lc] = dist
                }
            }

            if (!haveBase[lc] || dist.betterThan(baseDists[lc])) {
                haveBase[lc] = true
                baseDists[lc] = dist
            }
            lc++
        }
        i++
    }

    // output at most two candidates per lenCode: max and base (if different)
    var lc = 0
    while (lc < 29) {
        if (haveMax[lc]) {
            val maxLen = maxLengths[lc]
            val maxDist = maxDists[lc]
            out.add(Match(maxLen, maxDist))

            val base = Tables.LEN_BASE[lc]
            if (maxLen != base && haveBase[lc]) {
                out.add(Match(base, baseDists[lc]))
            }
        }
        lc++
    }

    return out
}