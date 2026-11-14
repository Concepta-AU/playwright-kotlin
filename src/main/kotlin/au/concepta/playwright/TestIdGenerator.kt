package au.concepta.playwright

import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Generates unique identifiers for isolating test data in concurrent and multi-process test runs.
 */
object TestIdGenerator {

    /**
     * Generation strategy for test identifiers.
     */
    enum class Strategy {
        /**
         * Multi-process safe on the same machine (~6–8 characters).
         *
         * Combines a 2-character startup salt, the JVM process ID in Base36, and an atomic sequence
         * counter in Base36. All characters are lowercase alphanumeric (`[0-9a-z]`), making it safe
         * for case-insensitive systems, URLs, and fields with strict validation regexes.
         *
         * Example: `"4kvt31"`
         */
        COMPACT,

        /**
         * Process-local atomic counter in Base36 (1–4 characters).
         *
         * Minimal length, but NOT safe across multiple processes. Use when running single-process
         * test suites or against systems with severe field length limits.
         *
         * Example: `"1"`, `"2"`, `"a"`
         */
        SHORT,

        /**
         * 128-bit UUID encoded as a lowercase Base36 string (~25 characters).
         *
         * Globally unique across time and machines.
         *
         * Example: `"3f9z...b2"`
         */
        FULL
    }

    // 2-character salt based on process start time modulo 1296 (36^2) to differentiate
    // sequential test runs if the OS reuses process IDs.
    private val runSalt: String = (System.currentTimeMillis() / 1000 % 1296).toString(36).padStart(2, '0')
    private val pidBase36: String = ProcessHandle.current().pid().toString(36)
    private val counter = AtomicInteger(0)

    /**
     * Generate a new unique identifier using the provided [strategy].
     *
     * @param strategy The generation strategy to use (defaults to [Strategy.COMPACT]).
     * @return A unique alphanumeric identifier.
     */
    fun nextId(strategy: Strategy = Strategy.COMPACT): String = when (strategy) {
        Strategy.COMPACT -> "$runSalt$pidBase36${counter.incrementAndGet().toString(36)}"
        Strategy.SHORT -> counter.incrementAndGet().toString(36)
        Strategy.FULL -> {
            val uuid = UUID.randomUUID()
            val bytes = ByteBuffer.allocate(16)
                .putLong(uuid.mostSignificantBits)
                .putLong(uuid.leastSignificantBits)
                .array()
            BigInteger(1, bytes).toString(36)
        }
    }

    /**
     * Generate a new unique identifier using [Strategy.COMPACT].
     */
    fun nextCompactId(): String = nextId(Strategy.COMPACT)

    /**
     * Generate a new unique identifier using [Strategy.SHORT].
     */
    fun nextShortId(): String = nextId(Strategy.SHORT)

    /**
     * Generate a new unique identifier using [Strategy.FULL].
     */
    fun nextFullId(): String = nextId(Strategy.FULL)
}
