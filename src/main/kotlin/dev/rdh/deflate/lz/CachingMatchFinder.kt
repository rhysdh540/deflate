package dev.rdh.deflate.lz

import dev.rdh.deflate.core.Match
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList

abstract class CachingMatchFinder : MatchFinder {

    protected val cache = Int2ObjectOpenHashMap<List<Match>>()

    protected abstract fun findAt(index: Int): List<Match>

    override fun matchesAt(index: Int): List<Match> {
        return cache.getOrPut(index) { findAt(index) }
    }
}