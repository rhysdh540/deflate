package demo

import dev.rdh.deflate.dp.MultiPassOptimalParser
import dev.rdh.deflate.dp.OptimalParser
import dev.rdh.deflate.dp.ParsingCostModel
import dev.rdh.deflate.format.writeDynamicBlock
import dev.rdh.deflate.lz.HashChainMatchFinder
import dev.rdh.deflate.split.Block
import dev.rdh.deflate.split.GreedyBlockSplitter
import dev.rdh.deflate.util.BitWriter
import inflate
import resourceBytes
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import kotlin.io.path.Path
import kotlin.io.path.writeBytes
import kotlin.io.println
import kotlin.time.measureTimedValue
import kotlin.use

fun main() {
    val resourcePath = "scrabble.txt"
    val data = resourceBytes(resourcePath)
    println("Input: ${data.size} bytes (${resourcePath})")

    val jdk = measureTimedValue { jdkDeflate(data) }
    println("java.util.zip size: ${jdk.value.size} bytes (${jdk.duration})")

    val (deflated, duration) = measureTimedValue {
        val mf = HashChainMatchFinder().also { it.reset(data) }
        val dp = OptimalParser.run(data, mf, ParsingCostModel.FIXED)

        val blocks = GreedyBlockSplitter().split(dp.tokens)

        val blockDPs = Block.toByteRanges(blocks, Block.bytesPrefix(dp.tokens)).map {
            MultiPassOptimalParser.run(data, mf,
                start = it.first, end = it.last + 1, maxPasses = 100
            )
        }

        ByteArrayOutputStream().use {
            val w = BitWriter(it)
            for ((i, dp) in blockDPs.withIndex()) {
                val last = (i == blockDPs.lastIndex)
                writeDynamicBlock(dp.tokens, w, final = last)
            }
            w.alignToByte()

            it.toByteArray()
        }
    }

    val inflated = inflate(deflated)
    if (inflated.contentEquals(data)) {
        println("Custom deflate matches original data")
    } else {
        println("Custom deflate does NOT match original data")
        Path("inflated.$resourcePath").writeBytes(inflated)
    }

    println("Custom deflate size: ${deflated.size} bytes (${duration})")
}

fun jdkDeflate(data: ByteArray, level: Int = Deflater.BEST_COMPRESSION): ByteArray {
    val deflater = Deflater(level, true)
    return try {
        deflater.setInput(data)
        deflater.finish()

        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)

        while (!deflater.finished()) {
            val n = deflater.deflate(buffer)
            if (n <= 0) break
            out.write(buffer, 0, n)
        }
        out.toByteArray()
    } finally {
        deflater.end()
    }
}