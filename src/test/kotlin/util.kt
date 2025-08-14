import dev.rdh.deflate.util.BitWriter
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.Inflater
import kotlin.io.use
import kotlin.use

fun write(block: (BitWriter) -> Unit): ByteArray {
    val out = ByteArrayOutputStream()
    BitWriter(out).use { bw -> block(bw) }
    return out.toByteArray()
}

fun inflate(data: ByteArray): ByteArray {
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

fun resourceBytes(path: String): ByteArray {
    val ins: InputStream = Thread.currentThread().contextClassLoader
        ?.getResourceAsStream(path.removePrefix("/"))
        ?: error("Resource not found on classpath: $path")
    return ins.use { it.readBytes() }
}