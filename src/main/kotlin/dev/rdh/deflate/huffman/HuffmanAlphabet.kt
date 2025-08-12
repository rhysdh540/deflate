package dev.rdh.deflate.huffman

import dev.rdh.deflate.util.BitWriter

/**
 * Represents a Huffman alphabet with lengths and codes for symbols.
 * Provides methods to write symbols using the Huffman codes.
 * @param lengths: Array of code lengths for each symbol.
 * @param codes: Array of Huffman codes for each symbol, least-significant bit first.
 */
class HuffmanAlphabet(
    val lengths: IntArray,
    val codes: IntArray,
) {
    fun writeSymbol(bw: BitWriter, sym: Int) {
        val len = lengths[sym]
        if (len == 0) error("symbol $sym has length 0")
        bw.writeBits(codes[sym], len)
    }

    companion object {
        private const val MAX_BITS = 15

        fun fromLengths(lengthsIn: IntArray): HuffmanAlphabet {
            val lengths = lengthsIn.copyOf()

            // count number of codes for each bit length
            val blCount = IntArray(MAX_BITS + 1)
            for (l in lengths) {
                require(l in 0..MAX_BITS) { "Invalid code length: $l" }
                if (l > 0) blCount[l]++
            }

            // get the first code for each bit length
            val nextCode = IntArray(MAX_BITS + 1)
            var code = 0
            for (bits in 1..MAX_BITS) {
                code = (code + blCount[bits - 1]) shl 1
                nextCode[bits] = code
            }

            // assign msb-first codes to symbols
            val codesMSB = IntArray(lengths.size)
            for (i in lengths.indices) {
                val l = lengths[i]
                if (l > 0) codesMSB[i] = nextCode[l]++
            }

            // flip them all to lsb-first
            val codesLE = IntArray(lengths.size)
            for (i in lengths.indices) {
                val l = lengths[i]
                if (l > 0) {
                    var x = codesMSB[i]
                    var r = 0
                    repeat(l) { r = (r shl 1) or (x and 1); x = x ushr 1 }
                    codesLE[i] = r
                }
            }

            return HuffmanAlphabet(lengths, codesLE)
        }

        val FIXED_LITLEN: HuffmanAlphabet by lazy {
            // RFC 1951 fixed litlen codes
            val lengths = IntArray(288) { i ->
                when {
                    i <= 143 -> 8 // 0..143
                    i <= 255 -> 9 // 144..255
                    i <= 279 -> 7 // 256..279
                    else -> 8   // 280..287
                }
            }
            fromLengths(lengths)
        }

        val FIXED_DIST: HuffmanAlphabet by lazy {
            // RFC 1951 fixed dist codes (all 5 bits)
            fromLengths(IntArray(30) { 5 })
        }
    }
}