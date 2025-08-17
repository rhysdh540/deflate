package dev.rdh.deflate.dp

import dev.rdh.deflate.core.Literal
import dev.rdh.deflate.core.Match
import dev.rdh.deflate.core.Tables
import dev.rdh.deflate.core.Token
import dev.rdh.deflate.huffman.HuffmanAlphabet

class ParsingCostModel(private val codes: CodeLengths) {

    fun costLiteral(b: Int): Int = codes.litlenBits[b and 0xFF]

    fun costMatch(len: Int, dist: Int): Int {
        val lcIdx = Tables.lenCodeIndex(len)    // 0..28 => code = 257+lcIdx
        val dcIdx = Tables.distCodeIndex(dist)  // 0..29
        val lenBits = codes.litlenBits[257 + lcIdx]
        val distBits = codes.distBits[dcIdx]
        val extraLen = Tables.LEN_EXTRA[lcIdx]
        val extraDist = Tables.DIST_EXTRA[dcIdx]
        return lenBits + extraLen + distBits + extraDist
    }

    fun costEOB(): Int = codes.litlenBits[256]

    fun costPayload(tokens: List<Token>): Int {
        var bits = 0
        for (t in tokens) {
            bits += when (t) {
                is Literal -> costLiteral(t.intValue)
                is Match -> costMatch(t.len, t.dist)
            }
        }

        bits += costEOB() // EOB must be present
        return bits
    }

    companion object {
        val FIXED = ParsingCostModel(CodeLengths(HuffmanAlphabet.FIXED_LITLEN.lengths, HuffmanAlphabet.FIXED_DIST.lengths))
    }

    class CodeLengths(
        val litlenBits: IntArray,
        val distBits: IntArray,
    )
}