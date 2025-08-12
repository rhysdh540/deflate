package dev.rdh.deflate.lz

import dev.rdh.deflate.core.Match

interface MatchFinder : AutoCloseable {
    fun reset(input: ByteArray)
    fun matchesAt(index: Int): List<Match>
    override fun close() {}
}