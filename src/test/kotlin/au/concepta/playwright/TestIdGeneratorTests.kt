package au.concepta.playwright

import org.assertj.core.api.Assertions.assertThat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test

class TestIdGeneratorTests {

    @Test
    fun `compact strategy produces lowercase alphanumeric strings`() {
        val id = TestIdGenerator.nextCompactId()
        assertThat(id).matches("^[0-9a-z]+$")
        assertThat(id.length).isBetween(5, 10)
    }

    @Test
    fun `compact strategy generates unique IDs concurrently`() {
        val threadCount = 8
        val idsPerThread = 250
        val totalIds = threadCount * idsPerThread
        val generatedIds = ConcurrentHashMap.newKeySet<String>()

        val executor = Executors.newFixedThreadPool(threadCount)
        repeat(totalIds) {
            executor.submit {
                generatedIds.add(TestIdGenerator.nextCompactId())
            }
        }
        executor.shutdown()
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue()

        assertThat(generatedIds).hasSize(totalIds)
    }

    @Test
    fun `short strategy produces ultra short alphanumeric IDs`() {
        val id1 = TestIdGenerator.nextShortId()
        val id2 = TestIdGenerator.nextShortId()

        assertThat(id1).matches("^[0-9a-z]+$")
        assertThat(id2).matches("^[0-9a-z]+$")
        assertThat(id1).isNotEqualTo(id2)
        assertThat(id1.length).isLessThanOrEqualTo(4)
    }

    @Test
    fun `full strategy produces 128-bit UUID encoded in base36`() {
        val id1 = TestIdGenerator.nextFullId()
        val id2 = TestIdGenerator.nextFullId()

        assertThat(id1).matches("^[0-9a-z]+$")
        assertThat(id2).matches("^[0-9a-z]+$")
        assertThat(id1).isNotEqualTo(id2)
        assertThat(id1.length).isBetween(20, 26)
    }

    @Test
    fun `default nextId uses compact strategy`() {
        val id = TestIdGenerator.nextId()
        assertThat(id).matches("^[0-9a-z]+$")
        assertThat(id.length).isBetween(5, 10)
    }
}
