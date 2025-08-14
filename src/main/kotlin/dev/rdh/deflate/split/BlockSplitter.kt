package dev.rdh.deflate.split

import dev.rdh.deflate.core.Token

interface BlockSplitter {
    data class Block(val start: Int, val end: Int)

    /**
     * Splits the input tokens into blocks.
     * @param tokens The input list of tokens to be split into blocks.
     * @return A list of [Block] objects, each representing a range of tokens in the input.
     */
    fun split(tokens: List<Token>): List<Block>
}