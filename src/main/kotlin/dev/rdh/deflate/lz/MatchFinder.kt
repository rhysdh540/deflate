package dev.rdh.deflate.lz

import dev.rdh.deflate.core.Match
import dev.rdh.deflate.util.MatchList

/**
 * Finds matches in a byte array. wow!
 */
interface MatchFinder : AutoCloseable {

    /**
     * Resets this match finder with a new input byte array.
     * Future calls to [matchesAt] should search this new input.
     */
    fun reset(input: ByteArray)

    /**
     * Finds matches at the given index in the input byte array.
     * The finder should search *before* the given index, finding sequences
     * that match the bytes starting at the given index.
     * Which matches are included in the result is up to the implementation
     * and may depend on the current state of the match finder.
     *
     * @param index The index in the input byte array to search for matches.
     * @return A list of [Match] objects found at the specified index.
     */
    fun matchesAt(index: Int): MatchList

    override fun close() {}
}