package dev.rdh.deflate.dp

import dev.rdh.deflate.core.Literal
import dev.rdh.deflate.core.Match
import dev.rdh.deflate.core.Token
import dev.rdh.deflate.lz.MatchFinder
import it.unimi.dsi.fastutil.objects.ObjectArrayList

object OptimalParser {
    fun run(
        input: ByteArray,
        mf: MatchFinder,
        costs: ParsingCostModel,
        start: Int = 0,
        end: Int = input.size
    ): ParseResult {
        val n = end - start
        val dp = IntArray(n + 1) { Int.MAX_VALUE / 4 }
        val choice = IntArray(n + 1) { -1 }  // -1 -> literal; otherwise match length
        val dist = IntArray(n + 1) { 0 }

        dp[n] = costs.costEOB()

        for (gi in end - 1 downTo start) {
            val i = gi - start
            // literal
            var best = costs.costLiteral(input[gi].toInt()) + dp[i + 1]
            var bestLen = -1
            var bestDist = 0

            // matches
            for (m in mf.matchesAt(gi)) {
                val j = i + m.len
                if (j <= n) {
                    val c = costs.costMatch(m.len, m.dist) + dp[j]
                    if (c < best || (c == best && breakTie(m.len, m.dist, bestLen, bestDist))) {
                        best = c
                        bestLen = m.len
                        bestDist = m.dist
                    }
                }
            }

            dp[i] = best
            if (bestLen > 0) {
                choice[i] = bestLen
                dist[i] = bestDist
            } else {
                choice[i] = -1
            }
        }

        // Reconstruct tokens
        val out = ObjectArrayList<Token>()
        var i = 0
        while (i < n) {
            if (choice[i] == -1) {
                out.add(Literal(input[start + i]))
                i++
            } else {
                out.add(Match(choice[i], dist[i]))
                i += choice[i]
            }
        }

        return ParseResult(out, dp[0])
    }

    /**
     * Break ties between two candidates for the same position.
     * Prioritizes longer matches, and if lengths are equal,
     * prefers the one with the smaller distance.
     */
    private fun breakTie(len: Int, dist: Int, curLen: Int, curDist: Int): Boolean {
        if (curLen <= 0) return true
        if (len != curLen) return len > curLen
        return dist < curDist
    }

    data class ParseResult(val tokens: List<Token>, val totalBitsNoHeader: Int)
}