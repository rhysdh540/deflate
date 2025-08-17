package demo

import dev.rdh.deflate.dp.MultiPassOptimalParser
import dev.rdh.deflate.format.writeDynamicBlock
import dev.rdh.deflate.lz.HashChainMatchFinder
import dev.rdh.deflate.split.GreedyBlockSplitter
import dev.rdh.deflate.util.BitWriter
import inflate
import resourceBytes
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import kotlin.io.path.Path
import kotlin.io.path.writeBytes
import kotlin.time.measureTimedValue
import kotlin.use

fun main() {
    val resourcePath = "bible.txt"
    val data = resourceBytes(resourcePath)
    println("Input: ${data.size} bytes (${resourcePath})")

    val jdk = measureTimedValue { jdkDeflate(data) }
    println("java.util.zip size: ${jdk.value.size} bytes (${jdk.duration})")

    val (deflated, duration) = measureTimedValue {
        val mf = HashChainMatchFinder()
        val (dp, duration0) = measureTimedValue {
            MultiPassOptimalParser.run(data, mf, maxPasses = 100)
        }

        println("main optimal parser: ${dp.tokens.size} tokens, ${dp.passes} passes (${duration0})")

        val blocks = GreedyBlockSplitter().split(dp.tokens)

        println("split ${blocks.size} blocks")


        ByteArrayOutputStream().use {
            val w = BitWriter(it)
            for ((i, b) in blocks.withIndex()) {
                val last = (i == blocks.lastIndex)
                val slice = dp.tokens.subList(b.start, b.end)
                writeDynamicBlock(slice, w, final = last)
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