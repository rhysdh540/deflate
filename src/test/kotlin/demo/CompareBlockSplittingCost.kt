package demo

import dev.rdh.deflate.core.Literal
import dev.rdh.deflate.core.Match
import dev.rdh.deflate.core.Token
import dev.rdh.deflate.dp.ParsingCostModel
import dev.rdh.deflate.dp.OptimalParser
import dev.rdh.deflate.util.BitWriter
import dev.rdh.deflate.lz.DefaultMatchFinder
import dev.rdh.deflate.format.writeDynamicBlock
import dev.rdh.deflate.split.BlockSplitter
import dev.rdh.deflate.split.GreedyBlockSplitter
import inflate
import java.io.ByteArrayOutputStream
import java.io.InputStream

fun main() {
    val resourcePath = "Alice.txt"
    val data = resourceBytes(resourcePath)
    println("Input: ${data.size} bytes (${resourcePath})")

    val mf = DefaultMatchFinder().also { it.reset(data) }
    val dp = OptimalParser.run(data, mf, ParsingCostModel.FIXED)
    val tokens = dp.tokens
    println("Tokens: literals/matches = ${tokens.count { it is Literal }}/${tokens.count { it is Match }}")

    val single = deflateSingleBlock(tokens)
    val singleInflated = inflate(single)
    check(singleInflated.contentEquals(data)) { "single-block round-trip failed" }
    println("Single-block: ${single.size} bytes")

    val splitter: BlockSplitter = GreedyBlockSplitter(
        minTokensPerBlock = 300,
        coarseStep = 1024,
        refineRadius = 2048,
        refineStep = 256,
        gainThresholdBits = 128
    )
    val blocks = splitter.split(tokens)
    val split = deflateWithSplits(tokens, blocks)
    val splitInflated = inflate(split)
    check(splitInflated.contentEquals(data)) { "split-blocks round-trip failed" }
    println("Split-blocks: ${split.size} bytes in ${blocks.size} block(s)")

    val delta = single.size - split.size
    val pct = if (single.isNotEmpty()) (delta.toDouble() / single.size * 100.0) else 0.0
    val sign = if (delta >= 0) "-" else "+"
    println("Improvement: $sign${kotlin.math.abs(delta)} bytes (${String.format("%.2f", pct)}%)")
}

private fun deflateSingleBlock(tokens: List<Token>): ByteArray {
    val baos = ByteArrayOutputStream()
    BitWriter(baos).use { bw ->
        writeDynamicBlock(tokens, bw, final = true)
        bw.alignToByte()
    }
    return baos.toByteArray()
}

private fun deflateWithSplits(tokens: List<Token>, blocks: List<BlockSplitter.Block>): ByteArray {
    val baos = ByteArrayOutputStream()
    BitWriter(baos).use { bw ->
        for ((i, b) in blocks.withIndex()) {
            val last = (i == blocks.lastIndex)
            val slice = tokens.subList(b.start, b.end)
            writeDynamicBlock(slice, bw, final = last)
        }
        bw.alignToByte()
    }
    return baos.toByteArray()
}

private fun resourceBytes(path: String): ByteArray {
    val ins: InputStream = Thread.currentThread().contextClassLoader
        ?.getResourceAsStream(path.removePrefix("/"))
        ?: error("Resource not found on classpath: $path")
    return ins.use { it.readBytes() }
}