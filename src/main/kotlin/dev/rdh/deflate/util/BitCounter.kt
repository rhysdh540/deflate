package dev.rdh.deflate.util

class BitCounter : BitSink {
    var bitsWritten = 0L; private set
    private var pos = 0

    override val isAligned: Boolean
        get() = pos == 0

    override fun writeBit(bit: Boolean) {
        bitsWritten++
        pos++
        if (pos == 8) {
            pos = 0
        }
    }

    override fun writeBits(value: Int, n: Int) {
        require(n in 1..32) { "n must be between 1 and 32" }
        bitsWritten += n
        pos += n
        if (pos >= 8) {
            pos = pos and 7
        }
    }

    override fun alignToByte() {
        if (pos > 0) {
            bitsWritten += (8 - pos)
            pos = 0
        }
    }

    override fun writeAlignedByte(b: Int) {
        check(pos == 0) { "Not byte-aligned" }
        bitsWritten += 8
    }

    override fun writeAlignedLE16(v: Int) {
        check(pos == 0) { "Not byte-aligned" }
        bitsWritten += 16
    }

    override fun close() {
        alignToByte()
    }
}