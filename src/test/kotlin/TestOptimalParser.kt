import dev.rdh.deflate.core.Literal
import dev.rdh.deflate.core.Match
import dev.rdh.deflate.dp.CostModel
import dev.rdh.deflate.dp.OptimalParser
import dev.rdh.deflate.lz.DefaultMatchFinder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestOptimalParser {
    @Test
    fun repeatingSequencePrefersMatchOverLiterals() {
        // "aaaaab"  -> expect: L('a'), M(len=4, dist=1), L('b') under fixed Huffman costs
        val s = "aaaaab".encodeToByteArray()
        val mf = DefaultMatchFinder(32 * 1024).also { it.reset(s) }
        val costs = CostModel.FIXED

        val result = OptimalParser.run(s, mf, costs)
        val tokens = result.tokens

        assertEquals(3, tokens.size, "expected 3 tokens")

        val t0 = tokens[0]
        val t1 = tokens[1]
        val t2 = tokens[2]

        assertTrue(t0 is Literal && t0.intValue == 'a'.code, "t0 should be Literal('a')")
        assertTrue(t1 is Match && t1.len == 4 && t1.dist == 1, "t1 should be a match with dist=1 and len=3")
        assertTrue(t2 is Literal && t2.intValue == 'b'.code, "t2 should be Literal('b')")
    }

    @Test
    fun randomBytesStayAsLiterals() {
        // no repeats → dp should choose all literals
        val s = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val mf = DefaultMatchFinder(32 * 1024).also { it.reset(s) }
        val costs = CostModel.FIXED

        val result = OptimalParser.run(s, mf, costs)
        val tokens = result.tokens

        assertEquals(s.size, tokens.size, "one token per byte")
        assertTrue(tokens.all { it is Literal }, "all tokens should be literals")
        // verify they match the input bytes
        for (i in s.indices) {
            val t = tokens[i] as Literal
            assertEquals(s[i], t.value, "literal mismatch at $i")
        }
    }
}