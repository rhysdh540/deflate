import dev.rdh.deflate.util.BitWriter
import org.junit.Assert.assertArrayEquals
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class TestBitWriter {

    @Test
    fun writesSingleBits_LSBFirst() {
        // Write bits: 1,0,1,0,1,0,1,0  -> 0b01010101 = 0x55
        val out = write { bw ->
            bw.writeBit(true);  bw.writeBit(false)
            bw.writeBit(true);  bw.writeBit(false)
            bw.writeBit(true);  bw.writeBit(false)
            bw.writeBit(true);  bw.writeBit(false)
        }
        assertArrayEquals(byteArrayOf(0x55), out)
    }

    @Test
    fun writeBits_8bits_emitsExactByte() {
        // Writing 0x62 (0b01100010) as 8 bits should emit that exact byte.
        val out = write { bw -> bw.writeBits(0x62, 8) }
        assertArrayEquals(byteArrayOf(0x62), out)
    }

    @Test
    fun crossesByteBoundary_correctLowThenHigh() {
        // 0xABCD as 16 bits LSB-first -> first byte 0xCD, then 0xAB
        val out = write { bw -> bw.writeBits(0xABCD, 16) }
        assertArrayEquals(byteArrayOf(0xCD.toByte(), 0xAB.toByte()), out)
    }

    @Test
    fun partialThenAlign_padsWithZeros() {
        // Write 5 bits: LSB-first of 0b10111 is bits 1,1,1,0,1 → byte 0x17 after padding
        val out = write { bw ->
            bw.writeBits(0b10111, 5)
            bw.alignToByte()
        }
        assertArrayEquals(byteArrayOf(0x17), out)
    }

    @Test
    fun interleavedWrites_formOneByteCorrectly() {
        // 3 bits (0b101), then 5 bits (0b00111)
        // Bits sequence LSB-first: 1,0,1, 1,1,1,0,0 -> 0b00111101 = 0x3D
        val out = write { bw ->
            bw.writeBits(0b101, 3)
            bw.writeBits(0b00111, 5)
        }
        assertArrayEquals(byteArrayOf(0x3D), out)
    }

    @Test
    fun writeAlignedByte_requiresAlignment() {
        // Not aligned: writing an aligned byte should fail
        val baos = ByteArrayOutputStream()
        val bw = BitWriter(baos)
        bw.writeBits(0b101, 3)
        val ex = assertThrows<IllegalStateException> {
            bw.writeAlignedByte(0xAA)
        }
        assertEquals(ex.message?.contains("Not byte-aligned"), true)
        bw.close()
    }

    @Test
    fun writeAlignedLE16_littleEndianOrder() {
        // At byte boundary, LE16 should write low then high
        val out = write { bw ->
            bw.alignToByte()
            bw.writeAlignedLE16(0x1234)
        }
        assertArrayEquals(byteArrayOf(0x34, 0x12.toByte()), out)
    }

    @Test
    fun closeFlushesPartialByte() {
        // 3 bits (0b101) then close -> one byte 0x05 emitted (zeros padded at high bits)
        val baos = ByteArrayOutputStream()
        BitWriter(baos).use { bw ->
            bw.writeBits(0b101, 3)
            // no explicit align; close() should flush the partial byte
        }
        assertArrayEquals(byteArrayOf(0x05), baos.toByteArray())
    }
}