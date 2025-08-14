package dev.rdh.deflate.util

import java.io.OutputStream

/**
 * LSB-first bit writer for writing bits to an output stream.
 */
class BitWriter(private val out: OutputStream) : BitSink {
    private var current = 0
    private var pos = 0

    override val isAligned: Boolean
        get() = pos == 0

    override fun writeBit(bit: Boolean) {
        if (bit) current = current or (1 shl pos)
        pos++
        if (pos == 8) flushByte()
    }

    override fun writeBits(value: Int, n: Int) {
        require(n in 1..Int.SIZE_BITS)
        var v = value
        var left = n
        while (left > 0) {
            val space = 8 - pos
            val take = if (left < space) left else space
            current = current or ((v and ((1 shl take) - 1)) shl pos)
            pos += take
            v = v ushr take
            left -= take
            if (pos == 8) flushByte()
        }
    }

    override fun alignToByte() {
        if (pos != 0) flushByte()
    }

    override fun writeAlignedByte(b: Int) {
        check(pos == 0) { "Not byte-aligned" }
        out.write(b and 0xFF)
    }

    override fun writeAlignedLE16(v: Int) {
        writeAlignedByte(v)
        writeAlignedByte(v ushr 8)
    }

    private fun flushByte() {
        out.write(current)
        current = 0
        pos = 0
    }

    override fun close() {
        alignToByte()
        out.flush()
    }
}