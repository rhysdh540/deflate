package dev.rdh.deflate.lz

import dev.rdh.deflate.core.Match
import dev.rdh.deflate.util.MatchList

/**
 * Basic match finder that finds every match in the input data
 * within a specified sliding window.
 */
class DefaultMatchFinder(private val windowSize: Int = 32 * 1024) : MatchFinder {
    private lateinit var s: ByteArray

    override fun reset(input: ByteArray) {
        s = input
    }

    override fun matchesAt(index: Int): MatchList {
        // Need at least 3 bytes of lookahead for a valid match
        if (index < 0 || index >= s.size - 2) return MatchList()

        val start = maxOf(0, index - windowSize)
        val maxLen = minOf(258, s.size - index)

        val matches = MatchList()
        for (i in start until index) {
            var len = 0
            while (len < maxLen && s[i + len] == s[index + len]) {
                len++
            }
            if (len >= 3) {
                matches.add(Match(len, index - i))
            }
        }
        return matches
    }
}