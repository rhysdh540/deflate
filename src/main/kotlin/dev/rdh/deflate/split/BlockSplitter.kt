package dev.rdh.deflate.split

import dev.rdh.deflate.core.Token

data class Block(val start: Int, val end: Int)

interface BlockSplitter {
    /**
     * Splits the input tokens into blocks.
     * @param tokens The input list of tokens to be split into blocks.
     * @return A list of [Block] objects, each representing a range of tokens in the input.
     */
    fun split(tokens: List<Token>): List<Block>
}