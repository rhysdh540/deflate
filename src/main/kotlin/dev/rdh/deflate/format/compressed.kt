package dev.rdh.deflate.format

import dev.rdh.deflate.core.Literal
import dev.rdh.deflate.core.Match
import dev.rdh.deflate.core.Tables
import dev.rdh.deflate.core.Token
import dev.rdh.deflate.huffman.HuffmanAlphabet
import dev.rdh.deflate.util.BitWriter

fun writeFixedBlock(
    tokens: List<Token>,
    bw: BitWriter,
    final: Boolean = false
) {
    bw.writeBit(final)
    bw.writeBits(0b01, 2)
    writeCompressedPayload(tokens, HuffmanAlphabet.FIXED_LITLEN, HuffmanAlphabet.FIXED_DIST, bw)
    bw.alignToByte()
}

internal fun writeCompressedPayload(
    tokens: List<Token>,
    litlen: HuffmanAlphabet,
    dist: HuffmanAlphabet,
    bw: BitWriter
) {
    if (tokens.size > 0xFFFF) {
        error("Too many tokens for a single block: ${tokens.size}")
    }

    for (t in tokens) {
        when (t) {
            is Literal -> {
                litlen.writeSymbol(bw, t.intValue)
            }
            is Match -> {
                lengthSymbol(t.len).write(bw, litlen)
                distanceSymbol(t.dist).write(bw, dist)
            }
        }
    }

    litlen.writeSymbol(bw, 256)
}

private data class Sym(val symbol: Int, val extraVal: Int, val extraBits: Int) {
    fun write(bw: BitWriter, alphabet: HuffmanAlphabet) {
        alphabet.writeSymbol(bw, symbol)
        if (extraBits != 0) {
            bw.writeBits(extraVal, extraBits)
        }
    }
}

private fun lengthSymbol(len: Int): Sym {
    val idx = Tables.lenCodeIndex(len)
    val base = Tables.LEN_BASE[idx]
    val eb   = Tables.LEN_EXTRA[idx]
    val extraVal = if (eb == 0) 0 else (len - base)
    return Sym(idx + 257, extraVal, eb)
}

private fun distanceSymbol(dist: Int): Sym {
    val idx = Tables.distCodeIndex(dist)
    val base = Tables.DIST_BASE[idx]
    val eb   = Tables.DIST_EXTRA[idx]
    val extraVal = if (eb == 0) 0 else (dist - base)
    return Sym(idx, extraVal, eb)
}