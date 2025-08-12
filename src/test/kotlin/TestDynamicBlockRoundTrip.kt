import dev.rdh.deflate.core.Literal
import dev.rdh.deflate.core.Match
import dev.rdh.deflate.core.Token
import dev.rdh.deflate.format.writeDynamicBlock
import org.junit.Assert.assertArrayEquals
import kotlin.test.Test

class TestDynamicBlock {
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
}