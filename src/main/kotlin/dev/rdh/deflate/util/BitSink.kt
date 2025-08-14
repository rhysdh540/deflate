package dev.rdh.deflate.util

/**
 * LSB-first interface for writing bits to somewhere.
 */
interface BitSink : AutoCloseable {

    /**
     * True if we are currently byte-aligned (no pending bits).
     */
    val isAligned: Boolean

    /**
     * Write a single bit (LSB-first within the current byte).
     * @param bit The bit to write, true for 1, false for 0.
     */
    fun writeBit(bit: Boolean)

    /**
     * Write [n] bits from [value], least-significant bit first.
     * @param value The integer value to write.
     * @param n The number of bits to write (1-32).
     */
    fun writeBits(value: Int, n: Int)

    /**
     * Pad with zero bits to the next byte boundary.
     */
    fun alignToByte()

    /**
     * Raw aligned byte/LE16 writes (use ONLY when byte-aligned).
     * @param b The byte to write (0-255).
     */
    fun writeAlignedByte(b: Int)

    /**
     * Write a 16-bit little-endian value, aligned to byte boundary.
     * Use only when [isAligned] is true.
     * @param v The 16-bit value to write.
     */
    fun writeAlignedLE16(v: Int)
}