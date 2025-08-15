import dev.rdh.deflate.core.Tables
import dev.rdh.deflate.core.Tables.DIST_BASE
import dev.rdh.deflate.core.Tables.DIST_EXTRA
import dev.rdh.deflate.core.Tables.LEN_BASE
import dev.rdh.deflate.core.Tables.LEN_EXTRA
import kotlin.test.Test
import kotlin.test.assertEquals

class TablesTests {

    // verify the fast versions of lenCodeIndex and distCodeIndex we have in Tables.kt are correct
    private fun slowLenCodeIndex(len: Int): Int {
        if (len == 258) return 28
        for (i in 0 until 28) {
            val base = LEN_BASE[i]
            val span = 1 shl LEN_EXTRA[i]
            val hi = base + span - 1
            if (len in base..hi) return i
        }
        return 28
    }

    private fun slowDistCodeIndex(dist: Int): Int {
        for (i in 0 until DIST_BASE.size) {
            val base = DIST_BASE[i]
            val span = 1 shl DIST_EXTRA[i]
            val hi = base + span - 1
            if (dist in base..hi) return i
        }
        return DIST_BASE.lastIndex
    }

    @Test
    fun testLenCodeIndex() {
        for (len in 3..258) {
            assertEquals(slowLenCodeIndex(len), Tables.lenCodeIndex(len))
        }
    }

    @Test
    fun testDistCodeIndex() {
        for (dist in 1..32768) {
            assertEquals(slowDistCodeIndex(dist), Tables.distCodeIndex(dist))
        }
    }
}