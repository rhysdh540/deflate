package dev.rdh.deflate.split

import dev.rdh.deflate.core.Token
import dev.rdh.deflate.format.writeDynamicBlock
import dev.rdh.deflate.util.BitCounter

/**
 * Basic greedy/recursive zopfli-style block splitter.
 * This is a greedy block splitter that uses a coarse step to find a good initial split,
 * then refines the split using a finer step.
 * It is designed to be fast and simple, but may not always find THE optimal split.
 *
 * @param minTokensPerBlock The minimum number of tokens per block. Prevents tiny blocks.
 * @param coarseStep The step size for the scanning stride/coarse split.
 * @param refineRadius The radius around the coarse split to search.
 * @param refineStep The step size for the refinement search.
 * @param gainThresholdBits The minimum gain in bits required (compared to the combined block) to accept a split.
 */
class GreedyBlockSplitter(
    private val minTokensPerBlock: Int = 300,
    private val coarseStep: Int = 1024,
    private val refineRadius: Int = 2048,
    private val refineStep: Int = 256,
    private val gainThresholdBits: Int = 128
) : BlockSplitter {

    override fun split(tokens: List<Token>): List<BlockSplitter.Block> {
        val out = mutableListOf<BlockSplitter.Block>()
        fun recurse(lo: Int, hi: Int) {
            val n = hi - lo
            if (n <= minTokensPerBlock) { out += BlockSplitter.Block(lo, hi); return }

            val full = blockCost(tokens, lo, hi)
            var bestPos = -1
            var bestGain = 0L

            var p = lo + minTokensPerBlock
            while (p <= hi - minTokensPerBlock) {
                if (((p - lo) % coarseStep) == 0) {
                    val gain = full - (blockCost(tokens, lo, p) + blockCost(tokens, p, hi))
                    if (gain > bestGain) { bestGain = gain; bestPos = p }
                }
                p++
            }
            if (bestPos >= 0) {
                val rlo = maxOf(lo + minTokensPerBlock, bestPos - refineRadius)
                val rhi = minOf(hi - minTokensPerBlock, bestPos + refineRadius)
                var q = rlo
                while (q <= rhi) {
                    if (((q - rlo) % refineStep) == 0) {
                        val gain = full - (blockCost(tokens, lo, q) + blockCost(tokens, q, hi))
                        if (gain > bestGain) { bestGain = gain; bestPos = q }
                    }
                    q++
                }
            }

            if (bestPos >= 0 && bestGain > gainThresholdBits) {
                recurse(lo, bestPos)
                recurse(bestPos, hi)
            } else {
                out += BlockSplitter.Block(lo, hi)
            }
        }
        recurse(0, tokens.size)
        return out
    }

    private fun blockCost(tokens: List<Token>, lo: Int, hi: Int): Long {
        val counter = BitCounter()
        writeDynamicBlock(tokens.subList(lo, hi), counter)
        return counter.bitsWritten
    }
}