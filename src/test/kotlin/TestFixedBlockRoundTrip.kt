import dev.rdh.deflate.core.Literal
import dev.rdh.deflate.core.Token
import dev.rdh.deflate.dp.CostModel
import dev.rdh.deflate.dp.OptimalParser
import dev.rdh.deflate.format.writeFixedBlock
import dev.rdh.deflate.lz.DefaultMatchFinder
import dev.rdh.deflate.util.BitWriter
import org.junit.Assert.assertArrayEquals
import java.io.ByteArrayOutputStream
import java.util.zip.Inflater
import kotlin.test.Test

class TestFixedBlockRoundTrip {

    private fun deflate(tokens: List<Token>): ByteArray {
        val baos = ByteArrayOutputStream()
        BitWriter(baos).use { bw ->
            writeFixedBlock(tokens, bw, final = true)
        }
        return baos.toByteArray()
    }

    private fun inflate(data: ByteArray): ByteArray {
        val inf = Inflater(true) // nowrap=true = raw deflate stream
        inf.setInput(data)
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        while (!inf.finished() && !inf.needsDictionary()) {
            val n = inf.inflate(buf)
            if (n == 0) {
                if (inf.needsInput()) break
                if (inf.needsDictionary()) break
            } else {
                out.write(buf, 0, n)
            }
        }
        inf.end()
        return out.toByteArray()
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