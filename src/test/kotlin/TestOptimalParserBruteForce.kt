import dev.rdh.deflate.core.Literal
import dev.rdh.deflate.core.Match
import dev.rdh.deflate.core.Token
import dev.rdh.deflate.dp.CostModel
import dev.rdh.deflate.dp.OptimalParser
import dev.rdh.deflate.lz.DefaultMatchFinder
import dev.rdh.deflate.lz.MatchFinder
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlin.math.min
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestOptimalParserBruteForce {

    @Test
    fun dpEqualsBruteforce_onHandpickedInputs() {
        val cases = listOf(
            "aaaaab".encodeToByteArray(),
            "banana_band".encodeToByteArray(),
            "abracadabra".encodeToByteArray(),
            "abcabcabcX".encodeToByteArray(),
            "mississippi".encodeToByteArray()
        ).map { it.copyOf(min(it.size, 12)) } // keep tiny to bound the search

        for (s in cases) {
            val mf = DefaultMatchFinder(32 * 1024).also { it.reset(s) }
            val costs = CostModel.FIXED

            val (bfBits, bfTokens) = bruteForceMinBits(s, mf, costs)
            val dp = OptimalParser.run(s, mf, costs)

            // DP must match the brute force minimum bitcount
            assertEquals(bfBits, dp.totalBitsNoHeader, "DP bitcount should equal bruteforce minimum")

            // Also re-score DP tokens independently to double-check emission cost math
            val dpRescored = scoreTokens(dp.tokens, costs)
            assertEquals(bfBits, dpRescored, "Rescored DP tokens should equal bruteforce minimum")

            // Quick sanity: path progresses to end
            assertTrue(reachesEnd(dp.tokens, s.size), "DP tokenization must consume the whole input")
        }
    }

    @Test
    fun dpEqualsBruteforce_onRandomTinyInputs() {
        val rnd = Random(1234) // fixed seed for reproducibility
        repeat(25) {
            val n = 6 + rnd.nextInt(6) // 6..11
            val s = ByteArray(n) { (rnd.nextInt(5) + 'a'.code).toByte() } // small alphabet to allow matches

            val mf = DefaultMatchFinder(32 * 1024).also { it.reset(s) }
            val costs = CostModel.FIXED

            val (bfBits, _) = bruteForceMinBits(s, mf, costs)
            val dp = OptimalParser.run(s, mf, costs)

            assertEquals(bfBits, dp.totalBitsNoHeader, "DP should match brute force on random tiny input")
        }
    }

    // ---- helpers ----

    private fun reachesEnd(tokens: List<Token>, n: Int): Boolean {
        var i = 0
        for (t in tokens) {
            i += when (t) {
                is Literal -> 1
                is Match -> t.len
            }
        }

        return i == n
    }

    private fun scoreTokens(tokens: List<Token>, costs: CostModel): Int {
        var bits = 0
        for (t in tokens) {
            bits += when (t) {
                is Literal -> costs.costLiteral(t.intValue)
                is Match -> costs.costMatch(t.len, t.dist)
            }
        }
        return bits + costs.costEOB()
    }

    /**
     * Exhaustively explore all tokenizations permitted by the given MatchFinder.
     * Uses branch-and-bound with a literal-cost lower bound to keep it fast.
     */
    private fun bruteForceMinBits(
        input: ByteArray,
        mf: MatchFinder,
        costs: CostModel
    ): Pair<Int, List<Token>> {
        val n = input.size
        var bestBits = Int.MAX_VALUE / 4
        val bestPath = ObjectArrayList<Token>()
        val curPath = ObjectArrayList<Token>()

        val minLit = (0..255).minOf { costs.costLiteral(it) } // 8 for fixed, but computed generically

        fun dfs(i: Int, costSoFar: Int) {
            // Bound: even if we used only the cheapest literal for the rest + EOB
            val optimistic = costSoFar + minLit * (n - i) + costs.costEOB()
            if (optimistic >= bestBits) return

            if (i >= n) {
                val total = costSoFar + costs.costEOB()
                if (total < bestBits) {
                    bestBits = total
                    bestPath.clear()
                    bestPath.addAll(curPath)
                }
                return
            }

            // Option 1: literal at i
            val litCost = costs.costLiteral(input[i].toInt() and 0xFF)
            curPath.add(Literal(input[i]))
            dfs(i + 1, costSoFar + litCost)
            curPath.removeAt(curPath.size - 1)

            // Option 2: any match that starts at i
            val matches = mf.matchesAt(i)
            for (m in matches) {
                val j = i + m.len
                if (j <= n) {
                    curPath.add(Match(m.len, m.dist))
                    val c = costs.costMatch(m.len, m.dist)
                    dfs(j, costSoFar + c)
                    curPath.removeAt(curPath.size - 1)
                }
            }
        }

        dfs(0, 0)
        return bestBits to bestPath
    }
}