package demo

import dev.rdh.deflate.dp.MultiPassOptimalParser
import dev.rdh.deflate.format.writeDynamicBlock
import dev.rdh.deflate.lz.HashChainMatchFinder
import dev.rdh.deflate.split.BlockSplitter
import dev.rdh.deflate.split.GreedyBlockSplitter
import dev.rdh.deflate.util.BitWriter
import inflate
import resourceBytes
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import kotlin.time.measureTimedValue
import kotlin.use

fun main() {
    val resourcePath = "E.coli"
    val data = resourceBytes(resourcePath)
    println("Input: ${data.size} bytes (${resourcePath})")

    val jdk = measureTimedValue { jdkDeflate(data) }
    println("java.util.zip size: ${jdk.value.size} bytes (${jdk.duration})")

    val (result, duration) = measureTimedValue {
        val mf = HashChainMatchFinder().also { it.reset(data) }
        val dp = MultiPassOptimalParser.run(data, mf)

        val splitter: BlockSplitter = GreedyBlockSplitter(
            minTokensPerBlock = 300,
            coarseStep = 1024,
            refineRadius = 2048,
            refineStep = 256,
            gainThresholdBits = 128
        )
        val blocks = splitter.split(dp.tokens)
        ByteArrayOutputStream().use {
            val w = BitWriter(it)
            for ((i, b) in blocks.withIndex()) {
                val last = (i == blocks.lastIndex)
                val slice = dp.tokens.subList(b.start, b.end)
                writeDynamicBlock(slice, w, final = last)
            }
            w.alignToByte()

            Triple(it.toByteArray(), blocks.size, dp)
        }
    }

    val (deflated, numBlocks, dp) = result

    inflate(deflated)

    println("Custom deflate size: ${deflated.size} bytes in $numBlocks blocks, ${dp.passes} passes (${duration})")
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