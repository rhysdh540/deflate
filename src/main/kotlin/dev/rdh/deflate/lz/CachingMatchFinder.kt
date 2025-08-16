package dev.rdh.deflate.lz

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