package dev.rdh.deflate.split

import dev.rdh.deflate.core.Token

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
    private val minTokensPerBlock: Int,
    private val coarseStep: Int,
    private val refineRadius: Int,
    private val refineStep: Int,
    private val gainThresholdBits: Int
) : BlockSplitter {
    override fun split(tokens: List<Token>): List<Block> {
        TODO()
    }
}