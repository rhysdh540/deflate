import dev.rdh.deflate.lz.DefaultMatchFinder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestDefaultMatchFinder {
    @Test
    fun findsBasicBackReferences() {
        val s = "abcabcabcX".encodeToByteArray()
        val mf = DefaultMatchFinder(32 * 1024)
        mf.reset(s)

        // at index 3 ('a'), there should be a match back to index 0 with distance 3
        val matches = mf.matchesAt(3)
        assertTrue(matches.any { it.dist == 3 && it.len >= 3 },
            "expected a match with dist=3 and len>=3 at index 3")
    }

    @Test
    fun respectsWindowAndMinimumLength() {
        val s = ByteArray(6) { 'a'.code.toByte() }
        val window = 4
        val mf = DefaultMatchFinder(window)
        mf.reset(s)

        val idx = 6 // last 'a'
        val matches = mf.matchesAt(idx)

        // all distances must be <= window and lengths >= 3
        assertTrue(matches.all { it.dist in 1..window }, "distance must be within window")
        assertTrue(matches.all { it.len >= 3 }, "min match length should be >= 3")
    }

    @Test
    fun capsMatchLengthAt258() {
        val s = ByteArray(600) { 'a'.code.toByte() }
        val mf = DefaultMatchFinder(32 * 1024)
        mf.reset(s)

        val idx = 100
        val matches = mf.matchesAt(idx)
        val maxLen = matches.maxOfOrNull { it.len } ?: 0
        assertEquals(258, maxLen, "longest match should be capped at 258")
    }

    @Test
    fun nearEndReturnsEmpty() {
        val s = "hello".encodeToByteArray()
        val mf = DefaultMatchFinder(32 * 1024)
        mf.reset(s)

        // With fewer than 3 bytes left, there can be no match starting here
        val matches = mf.matchesAt(s.size - 2)
        assertTrue(matches.isEmpty(), "expected no matches when <3 bytes remain")
    }
}