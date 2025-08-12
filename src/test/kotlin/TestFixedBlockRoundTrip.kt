import dev.rdh.deflate.core.Literal
import dev.rdh.deflate.core.Token
import dev.rdh.deflate.dp.CostModel
import dev.rdh.deflate.dp.OptimalParser
import dev.rdh.deflate.format.writeFixedBlock
import dev.rdh.deflate.lz.DefaultMatchFinder
import org.junit.Assert.assertArrayEquals
import kotlin.test.Test

class TestFixedBlockRoundTrip {

    private fun deflate(tokens: List<Token>): ByteArray {
        return write {
            writeFixedBlock(tokens, it, final = true)
        }
    }

    @Test
    fun roundTrip_fixedBlock_withDPtokens() {
        val src = "banana_band_banana_band".encodeToByteArray()

        val mf = DefaultMatchFinder(32 * 1024).also { it.reset(src) }
        val dp = OptimalParser.run(src, mf, CostModel.FIXED)

        val deflated = deflate(dp.tokens)
        val inflated = inflate(deflated)

        assertArrayEquals(src, inflated)
    }

    @Test
    fun roundTrip_fixedBlock_allLiterals() {
        val src = "no repeats here!".encodeToByteArray()
        val tokens = src.map { Literal(it) }
        val deflated = deflate(tokens)
        val inflated = inflate(deflated)
        assertArrayEquals(src, inflated)
    }
}