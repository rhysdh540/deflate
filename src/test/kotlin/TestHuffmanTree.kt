import dev.rdh.deflate.huffman.HuffmanAlphabet
import dev.rdh.deflate.huffman.HuffmanTree
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TestHuffmanTree {

    @Test
    fun prefixFree_and_Kraft_hold_on_typical_freqs() {
        val freq = intArrayOf(5, 7, 10, 15, 20, 30, 40, 80)
        val tree = HuffmanTree(freq, limit = 15)
        val alpha = tree.toAlphabet()

        assertPrefixFreeLSB(alpha)
        assertKraft(alpha)
        assertTrue(alpha.lengths.all { it in 0..15 })
    }

    @Test
    fun handles_all_zero_frequencies() {
        val freq = IntArray(8) // all zeros
        val tree = HuffmanTree(freq, limit = 15)
        val alpha = tree.toAlphabet()

        val lens = alpha.lengths
        val nonZero = lens.count { it > 0 }
        assertTrue(nonZero >= 2, "should have at least two codes")
        assertTrue(lens.all { it == 0 || it == 1 }, "dummies should be 1-bit codes")
        assertPrefixFreeLSB(alpha)
        assertKraft(alpha)
    }

    // ----- helpers -----

    /** Our codes are stored LSB-first; prefix check compares low bits. */
    private fun assertPrefixFreeLSB(a: HuffmanAlphabet) {
        val codes = a.codes
        val lens  = a.lengths
        val syms = (codes.indices).filter { lens[it] > 0 }
        for (i in syms.indices) {
            val si = syms[i]
            val ci = codes[si]; val li = lens[si]
            for (j in i + 1 until syms.size) {
                val sj = syms[j]
                val cj = codes[sj]; val lj = lens[sj]
                val m = (1 shl min(li, lj)) - 1
                // if one were a prefix of the other, their low min(li, lj) bits would be equal
                assertNotEquals(ci and m, cj and m, "Code $si is a prefix of $sj (or vice versa)")
            }
        }
    }

    /** Kraft–McMillan equality for a complete binary prefix code. */
    private fun assertKraft(a: HuffmanAlphabet) {
        val sum = a.lengths.filter { it > 0 }.sumOf { 1.0 / (1 shl it) }
        assertEquals(1.0, sum, 1e-9)
    }
}