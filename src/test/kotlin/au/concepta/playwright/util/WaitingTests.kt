package au.concepta.playwright.util

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.Duration
import kotlin.test.Test

val oneMs = Duration.ofMillis(1)!!

class WaitingTests {
    @Test
    fun `waitUntil calls onFail correctly`() {
        assertThatThrownBy { waitUntil( attempts = 23, waitTime = oneMs) { false } }
            .isInstanceOf(AssertionError::class.java)
            .hasMessage("Failed waiting for 23 attempts")

        val onFail = { throw RuntimeException("custom generated error") }
        assertThatThrownBy { waitUntil( attempts = 11, waitTime = oneMs, onFail = onFail) { false } }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("custom generated error")
    }

    @Test
    fun `waitFor gets object`() {
        var count = 0
        val targetVal = "TARGET"
        val result = waitFor(waitTime = oneMs) {
            if (count++ < 10) null else targetVal
        }
        assertThat(count).isEqualTo(11)
        assertThat(result).isEqualTo(targetVal)
    }

    @Test
    fun `waitFor calls onFail correctly`() {
        val onFail = { throw RuntimeException("custom generated error") }
        assertThatThrownBy { waitFor(waitTime = oneMs, onFail = onFail) { null } }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("custom generated error")

        val defaultVal = "DEFAULT"
        val defaultOnFail = { defaultVal }
        val result = waitFor(waitTime = oneMs, onFail = defaultOnFail) { null }
        assertThat(result).isEqualTo(defaultVal)
    }
}