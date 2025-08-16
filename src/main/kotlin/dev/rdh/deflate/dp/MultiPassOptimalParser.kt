package dev.rdh.deflate.dp

import dev.rdh.deflate.core.Literal
import dev.rdh.deflate.core.Match
import dev.rdh.deflate.core.Token
import dev.rdh.deflate.format.writeDynamicHeader
import dev.rdh.deflate.lz.MatchFinder
import dev.rdh.deflate.util.BitCounter

data class MultiPassResult(
    val tokens: List<Token>,
    val costs: ParsingCostModel, // final code lengths used by the last pass
    val totalBits: Long, // 3 (BFINAL+BTYPE) + header + payload
    val headerBits: Long,
    val payloadBits: Int,
    val passes: Int
)

object MultiPassOptimalParser {

    fun run(
        input: ByteArray,
        mf: MatchFinder,
        maxPasses: Int = 3,
        epsilon: Double = 5e-4
    ): MultiPassResult {
        var costs = ParsingCostModel.FIXED
        mf.reset(input)

        var bestTotal = Long.MAX_VALUE
        var bestRes: MultiPassResult? = null

        var prevTotal = Long.MAX_VALUE
        var prevLit = IntArray(0)
        var prevDist = IntArray(0)


        var pass = 0
        while (pass < maxPasses) {
            val parsed = OptimalParser.run(input, mf, costs)
            val tokens = parsed.tokens

            val counter = BitCounter()
            val (litAlpha, distAlpha) = writeDynamicHeader(tokens, counter)
            val headerBits = counter.bitsWritten

            val passCosts = ParsingCostModel(
                ParsingCostModel.CodeLengths(litAlpha.lengths, distAlpha.lengths)
            )
            val total = 3 + headerBits + payloadBits(tokens, passCosts)

            if (total < bestTotal) {
                bestTotal = total
                bestRes = MultiPassResult(tokens, passCosts, total, headerBits, payloadBits(tokens, passCosts), pass + 1)
            }

            // stopping conditions
            val relGain = if (prevTotal == Long.MAX_VALUE) 1.0 else (prevTotal - total).toDouble() / prevTotal
            val sameLit = litAlpha.lengths.contentEquals(prevLit)
            val sameDist = distAlpha.lengths.contentEquals(prevDist)
            if (relGain < epsilon || (sameLit && sameDist)) break

            // prepare for next pass using the new trees
            costs = passCosts
            prevTotal = total
            prevLit = litAlpha.lengths
            prevDist = distAlpha.lengths
            pass++
        }

        return requireNotNull(bestRes)
    }

    private fun payloadBits(tokens: List<Token>, costs: ParsingCostModel): Int {
        var bits = 0
        for (t in tokens) {
            bits += when (t) {
                is Literal -> costs.costLiteral(t.intValue)
                is Match -> costs.costMatch(t.len, t.dist)
            }
        }
        bits += costs.costEOB()
        return bits
    }
}
