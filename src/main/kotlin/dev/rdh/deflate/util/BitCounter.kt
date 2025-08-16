package dev.rdh.deflate.util

class BitCounter : BitSink {
    var bitsWritten = 0L; private set

    override val isAligned: Boolean
        get() {
            throw NotImplementedError("BitCounter does not support alignment checks")
        }

    override fun writeBit(bit: Boolean) {
        bitsWritten++
    }

    override fun writeBits(value: Int, n: Int) {
        require(n in 1..32) { "n must be between 1 and 32" }
        bitsWritten += n
    }

    override fun alignToByte() {
        throw NotImplementedError("BitCounter does not support alignment checks")
    }

    override fun writeAlignedByte(b: Int) {
        bitsWritten += 8
    }

    override fun writeAlignedLE16(v: Int) {
        bitsWritten += 16
    }

    override fun close() {
        alignToByte()
    }
}