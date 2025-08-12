package dev.rdh.deflate.format

import dev.rdh.deflate.core.Literal
import dev.rdh.deflate.core.Match
import dev.rdh.deflate.core.Tables
import dev.rdh.deflate.core.Token
import dev.rdh.deflate.huffman.HuffmanAlphabet
import dev.rdh.deflate.huffman.HuffmanTree
import dev.rdh.deflate.util.BitWriter
import it.unimi.dsi.fastutil.ints.IntArrayList

fun writeDynamicBlock(
    tokens: List<Token>,
    bw: BitWriter,
    final: Boolean = false
) {
    bw.writeBit(final)
    bw.writeBits(0b10, 2)

    val (litFreq, distFreq) = buildHistograms(tokens)
    val litlens = HuffmanTree(litFreq, limit = 15).toAlphabet()
    val dists = if (distFreq.any { it > 0 }) {
        HuffmanTree(distFreq, limit = 15).toAlphabet()
    } else {
        NO_DISTANCES_ALPHABET
    }

    val (litCount, distCount) = computeCounts(litlens.lengths, dists.lengths)

    val lengths = litlens.lengths.copyOfRange(0, litCount) + dists.lengths.copyOfRange(0, distCount)
    val rle = rleCodeLengths(lengths)
    val codeLengths = HuffmanTree(rle.freq, limit = 7).toAlphabet()

    val hclen = computeHCLEN(codeLengths)

    bw.writeBits(litCount - 257, 5) // HLIT = 257..286 (encoded as HLIT=0..29)
    bw.writeBits(distCount - 1, 5) // HDIST = 1..30 (encoded as HDIST=0..29)
    bw.writeBits(hclen - 4, 4) // HCLEN = 4..19 (encoded as HCLEN=0..15)
    for (i in 0 until hclen) {
        val sym = Tables.CODELENGTH_ORDER[i]
        bw.writeBits(codeLengths.lengths[sym], 3) // 3-bit code length per CL symbol
    }

    var i = 0
    while (i < rle.syms.size) {
        val s = rle.syms.getInt(i)
        codeLengths.writeSymbol(bw, s)
        val nb = rle.nbits.getInt(i)
        if (nb != 0) bw.writeBits(rle.extras.getInt(i), nb)
        i++
    }

    writeCompressedPayload(tokens, litlens, dists, bw)
    bw.alignToByte()
}

// if there's no distance tokens, the spec still requires at least one distance code
// so this is a minimal alphabet with a single code of length 1
val NO_DISTANCES_ALPHABET = HuffmanAlphabet.fromLengths(IntArray(30).also { it[0] = 1 })

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

private fun computeHCLEN(codeLengths: HuffmanAlphabet): Int {
    var last = -1
    for (i in 0 until 19) {
        val sym = Tables.CODELENGTH_ORDER[i]
        if (codeLengths.lengths[sym] != 0) last = i
    }
    return maxOf(4, last + 1) // HCLEN = number of entries we emit from CL_ORDER, min 4
}

/**
 * RLE of code lengths using symbols 0..15 for literals, and
 * 16/17/18 for runs of the same code length
 *
 * @param syms symbols in 0..18
 * @param extras extra-bit values for 16/17/18 symbols
 * @param nbits extra-bit counts for 16/17/18 symbols (0,2,3,7)
 * @param freq frequency of CL symbols 0..18
 */
private class CLRLE(
    val syms: IntArrayList,
    val extras: IntArrayList,
    val nbits: IntArrayList,
    val freq: IntArray
)

/**
 * RLE-encode the code-length sequence
 * @param seq the raw lengths, 0..15
 * @return the sequence that represents the input, with 16/17/18 codes for runs of the same number
 */
private fun rleCodeLengths(seq: IntArray): CLRLE {
    val syms = IntArrayList()
    val extras = IntArrayList()
    val nbits = IntArrayList()
    val freq = IntArray(19)

    var i = 0
    val n = seq.size
    while (i < n) {
        val num = seq[i]
        var count = 1
        while (i + count < n && seq[i + count] == num) {
            count++
        }

        if (num == 0) {
            var rem = count

            // code 18 for 11+ zeros
            while (rem >= 11) {
                val take = minOf(rem, 138)
                syms.add(18)
                extras.add(take - 11)
                nbits.add(7)
                freq[18]++
                rem -= take
            }

            // code 17 for 3–10 zeros
            while (rem >= 3) {
                val take = minOf(rem, 10)
                syms.add(17)
                extras.add(take - 3)
                nbits.add(3)
                freq[17]++
                rem -= take
            }

            // leftover 1–2 zeros
            repeat(rem) {
                syms.add(0)
                extras.add(0)
                nbits.add(0)
                freq[0]++
            }
        } else {
            // Emit one literal v, then repeats via 16
            syms.add(num)
            extras.add(0)
            nbits.add(0)
            freq[num]++
            var rem = count - 1
            while (rem >= 3) {
                val take = minOf(rem, 6)
                syms.add(16)
                extras.add(take - 3)
                nbits.add(2)
                freq[16]++
                rem -= take
            }

            // leftover 1–2
            repeat(rem) { syms.add(num)
                extras.add(0)
                nbits.add(0)
                freq[num]++
            }
        }

        i += count
    }
    return CLRLE(syms, extras, nbits, freq)
}