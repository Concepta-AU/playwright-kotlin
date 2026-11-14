package au.concepta.playwright

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class TestBaseIdTests : TestBase<DummyApp1>() {

    @Test
    fun `testId is stable within the same test`() {
        val id1 = testId
        val id2 = testId
        assertThat(id1).isEqualTo(id2)
        assertThat(id1).matches("^[0-9a-z]+$")
    }

    @Test
    fun `uniqueName formats with prefix and tag`() {
        val id = testId
        assertThat(uniqueName()).isEqualTo(id)
        assertThat(uniqueName("user")).isEqualTo("user-$id")
        assertThat(uniqueName("user", "buyer")).isEqualTo("user-$id-buyer")
        assertThat(uniqueName(tag = "seller")).isEqualTo("$id-seller")
    }

    @Test
    fun `uniqueSubId generates tagged or sequential sub-identifiers`() {
        val id = testId
        assertThat(uniqueSubId("custom")).isEqualTo("$id-custom")

        val sub1 = uniqueSubId()
        val sub2 = uniqueSubId()
        assertThat(sub1).isEqualTo("$id-1")
        assertThat(sub2).isEqualTo("$id-2")
    }

    @Test
    fun `resetTestId clears cached testId and subId counter`() {
        val id1 = testId
        val sub1 = uniqueSubId()
        assertThat(sub1).isEqualTo("$id1-1")

        resetTestId()

        val id2 = testId
        assertThat(id2).isNotEqualTo(id1)
        val sub2 = uniqueSubId()
        assertThat(sub2).isEqualTo("$id2-1")
    }
}

class CustomStrategyTestBase : TestBase<DummyApp1>() {
    override val testIdStrategy = TestIdGenerator.Strategy.SHORT

    @Test
    fun `subclass can override testIdStrategy`() {
        assertThat(testId).matches("^[0-9a-z]+$")
        assertThat(testId.length).isLessThanOrEqualTo(4)
    }
}
