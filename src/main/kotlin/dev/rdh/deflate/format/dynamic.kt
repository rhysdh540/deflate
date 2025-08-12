package dev.rdh.deflate.format

import dev.rdh.deflate.core.Literal
import dev.rdh.deflate.core.Match
import dev.rdh.deflate.core.Tables
import dev.rdh.deflate.core.Token
import dev.rdh.deflate.huffman.HuffmanAlphabet
import dev.rdh.deflate.huffman.HuffmanTree
import dev.rdh.deflate.util.BitWriter

fun writeDynamicBlock(
    tokens: List<Token>,
    bw: BitWriter,
    final: Boolean = false
) {
    bw.writeBit(final)
    bw.writeBits(0b10, 2) // BTYPE=10 (dynamic)

    val (litFreq, distFreq) = buildHistograms(tokens)
    val litlens = HuffmanTree(litFreq, limit = 15).toAlphabet()
    val dists = HuffmanTree(distFreq, limit = 15).toAlphabet()

    val (litCount, distCount) = computeCounts(litlens.lengths, dists.lengths)

    val lengths = litlens.lengths.copyOfRange(0, litCount) + dists.lengths.copyOfRange(0, distCount)
    val codeLengths = buildCodeLengthAlphabet(lengths)

    val hclen = computeHCLEN(codeLengths)

    bw.writeBits(litCount - 257, 5) // HLIT = 257..286 (encoded as HLIT=0..29)
    bw.writeBits(distCount - 1, 5) // HDIST = 1..30 (encoded as HDIST=0..29)
    bw.writeBits(hclen - 4, 4) // HCLEN = 4..19 (encoded as HCLEN=0..15)
    for (i in 0 until hclen) {
        val sym = Tables.CODELENGTH_ORDER[i]
        bw.writeBits(codeLengths.lengths[sym], 3) // 3-bit code length per CL symbol
    }

    for (v in lengths) {
        codeLengths.writeSymbol(bw, v)
    }

    writeCompressedPayload(tokens, litlens, dists, bw)
    bw.alignToByte()
}

private fun buildHistograms(tokens: List<Token>): Pair<IntArray, IntArray> {
    val lit = IntArray(286)
    val dist = IntArray(30)
    for (t in tokens) {
        when (t) {
            is Literal -> lit[t.intValue]++
            is Match -> {
                val lc = Tables.lenCodeIndex(t.len)    // 0..28
                lit[257 + lc]++
                val dc = Tables.distCodeIndex(t.dist)  // 0..29
                dist[dc]++
            }
        }
    }
    lit[256]++ // EOB must be present
    return lit to dist
}

private fun computeCounts(litlenLengths: IntArray, distLengths: IntArray): Pair<Int, Int> {
    fun IntArray.lastNonZero(maxIdx: Int): Int {
        var last = -1
        for (i in 0..maxIdx) {
            if (this[i] != 0) {
                last = i
            }
        }
        return last
    }

    val lastLit  = maxOf(256, litlenLengths.lastNonZero(285))
    val lastDist = maxOf(0,   distLengths.lastNonZero(29))

    val litCount  = (lastLit + 1).coerceIn(257, 286)
    val distCount = (lastDist + 1).coerceIn(1, 30)
    return litCount to distCount
}

// TODO: RLE for this
private fun buildCodeLengthAlphabet(lengths: IntArray): HuffmanAlphabet {
    val freq = IntArray(19)
    for (v in lengths) {
        freq[v]++
    }
    return HuffmanTree(freq, limit = 7).toAlphabet()
}

private fun computeHCLEN(codeLengths: HuffmanAlphabet): Int {
    var last = -1
    for (i in 0 until 19) {
        val sym = Tables.CODELENGTH_ORDER[i]
        if (codeLengths.lengths[sym] != 0) last = i
    }
    return maxOf(4, last + 1) // HCLEN = number of entries we emit from CL_ORDER, min 4
}

