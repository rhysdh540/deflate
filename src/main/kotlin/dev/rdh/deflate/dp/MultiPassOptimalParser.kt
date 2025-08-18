package dev.rdh.deflate.dp

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
        epsilon: Double = 0.0,
        start: Int = 0,
        end: Int = input.size,
        startingCosts: ParsingCostModel = ParsingCostModel.FIXED
    ): MultiPassResult {
        var costs = startingCosts

        var bestTotal = Long.MAX_VALUE
        var bestRes: MultiPassResult? = null

        var prevTotal = Long.MAX_VALUE
        var prevLit = IntArray(0)
        var prevDist = IntArray(0)


        var pass = 0
        while (pass < maxPasses) {
            val parsed = OptimalParser.run(input, mf, costs, start, end)
            val tokens = parsed.tokens

            val counter = BitCounter()
            val (litAlpha, distAlpha) = writeDynamicHeader(tokens, counter)
            val headerBits = counter.bitsWritten

            val passCosts = ParsingCostModel(
                ParsingCostModel.CodeLengths(litAlpha.lengths, distAlpha.lengths)
            )

            val payloadBits = passCosts.costPayload(tokens)
            val total = 3 + headerBits + payloadBits

            if (total < bestTotal) {
                bestTotal = total
                bestRes = MultiPassResult(tokens, passCosts, total, headerBits, payloadBits, pass + 1)
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

        return bestRes!!
    }

}
