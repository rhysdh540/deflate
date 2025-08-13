import dev.rdh.deflate.core.Literal
import dev.rdh.deflate.core.Match
import dev.rdh.deflate.core.Tables
import dev.rdh.deflate.core.Token
import dev.rdh.deflate.format.writeDynamicBlock
import dev.rdh.deflate.huffman.HuffmanAlphabet
import org.junit.Assert.assertArrayEquals
import kotlin.test.Test
import kotlin.test.assertEquals

class TestDynamicBlockRoundTrip {
    private fun deflate(tokens: List<Token>): ByteArray {
        return write {
            writeDynamicBlock(tokens, it, final = true)
        }
    }

    @Test
    fun allLiterals_roundTrip() {
        val src = "no repeats here, just literals!".encodeToByteArray()
        val tokens = src.map { Literal(it) }
        val deflated = deflate(tokens)
        val inflated = inflate(deflated)
        assertArrayEquals(src, inflated)
    }

    @Test
    fun withMatches_roundTrip() {
        val src = "banana_band_banana_band_banana".encodeToByteArray()
        // trivial tokens: Literal('b'), Match(len,1) pattern just to exercise len/dist paths
        val tokens = buildList {
            var i = 0
            while (i < src.size) {
                if (i >= 1 && i + 3 <= src.size && src[i] == src[i-1]) {
                    add(Match(len = minOf(4, src.size - i), dist = 1))
                    i += minOf(4, src.size - i)
                } else {
                    add(Literal(src[i])); i++
                }
            }
        }
        val deflated = deflate(tokens)
        val inflated = inflate(deflated)
        assertArrayEquals(src, inflated)
    }

    @Test
    fun lotsOfLengthZeroes() {
        val src = ByteArray(2048) { 'A'.code.toByte() }
        val tokens: List<Token> = src.map { Literal(it) }

        val deflated = deflate(tokens)

        //println("deflated size: ${deflated.size} bytes")

        val inflated = inflate(deflated)
        assertArrayEquals(src, inflated)

        val br = BitReader(deflated)
        assertEquals(0b1, br.readBit()) // final bit
        assertEquals(0b010, br.readBits(2)) // BTYPE: dynamic

        val litCount  = br.readBits(5) + 257
        val distCount = br.readBits(5) + 1
        val codeLenCount = br.readBits(4) + 4

        //println("counts: lit=$litCount, dist=$distCount, codeLen=$codeLenCount")

        val codeLenLengths = IntArray(Tables.CODELENGTH_ORDER.size)
        for (i in 0 until codeLenCount) {
            codeLenLengths[Tables.CODELENGTH_ORDER[i]] = br.readBits(3)
        }

        val codeLengths = HuffmanAlphabet.fromLengths(codeLenLengths)

        var decoded = 0
        val opCounts = IntArray(19)

        fun readCLSymbol(): Int {
            var code = 0
            var len = 0
            while (true) {
                val bit = br.readBit()
                code = code or (bit shl len)
                len++
                for (s in 0 until 19) {
                    if (codeLengths.lengths[s] == len && codeLengths.codes[s] == code) {
                        return s
                    }
                }
            }
        }

        while (decoded < litCount + distCount) {
            when (val s = readCLSymbol()) {
                in 0..15 -> {
                    decoded++
                    opCounts[s]++
                }
                16 -> {
                    val rep = 3 + br.readBits(2)
                    // repeat previous length
                    decoded += rep
                    opCounts[16]++
                }
                17 -> {
                    val rep = 3 + br.readBits(3)
                    // repeat zeros
                    decoded += rep
                    opCounts[17]++
                }
                18 -> {
                    val rep = 11 + br.readBits(7)
                    decoded += rep
                    opCounts[18]++
                }
                else -> error("invalid CL symbol $s")
            }
        }

        //println("opCounts: ${opCounts.joinToString(", ")}")
        assert(opCounts[18] > 0) { "Expected at least one 18 (length 11+ repeat), got ${opCounts[18]}" }
    }

    private class BitReader(private val data: ByteArray) {
        private var i = 0
        private var mask = 1
        fun readBit(): Int {
            val b = (data[i].toInt() and 0xFF)
            val bit = (b and mask) != 0
            mask = mask shl 1
            if (mask == 256) { mask = 1; i++ }
            return if (bit) 1 else 0
        }
        fun readBits(n: Int): Int {
            var v = 0
            var s = 0
            repeat(n) { v = v or (readBit() shl s); s++ } // LSB-first
            return v
        }
    }
}