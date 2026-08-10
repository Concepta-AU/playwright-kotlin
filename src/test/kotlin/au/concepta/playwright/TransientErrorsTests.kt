package au.concepta.playwright

import au.concepta.playwright.util.waitUntil
import com.microsoft.playwright.Page
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.Duration
import kotlin.test.Test

private const val TRANSIENT = "Failed to load resource: net::ERR_NETWORK_CHANGED"

class ErrorReportingApp : Application<DummyPage>() {
    override val defaultBaseUrl: String = "data:text/html,<html><body>Errors</body></html>"
    override fun getInitialApplicationPage(page: Page): DummyPage = DummyPage(page)
}

class StrictErrorReportingApp : Application<DummyPage>() {
    override val defaultBaseUrl: String = "data:text/html,<html><body>Errors</body></html>"
    override fun getInitialApplicationPage(page: Page): DummyPage = DummyPage(page)
    override val tolerateTransientErrors = false
}

/**
 * The transient-error exemption cannot be verified by reproducing the failure - it depends on a network event
 * outside the test's control. It is verified here instead: the classification against the error strings browsers
 * actually emit, and the behaviour of the console guard around it.
 */
class TransientErrorsTests {
    @Test
    fun `errors reporting a dead connection are transient`() {
        listOf(
            TRANSIENT,
            "Failed to load resource: net::ERR_HTTP2_PING_FAILED",
            "Failed to load resource: net::ERR_CONNECTION_RESET",
            "Failed to load resource: net::ERR_CONNECTION_CLOSED",
            "Failed to load resource: net::ERR_CONNECTION_TIMED_OUT",
            "Failed to load resource: net::ERR_NETWORK_IO_SUSPENDED",
            "Failed to load resource: net::ERR_INTERNET_DISCONNECTED",
        ).forEach {
            assertThat(isTransientError(it)).describedAs(it).isTrue()
        }
    }

    @Test
    fun `application and addressing errors are not transient`() {
        listOf(
            // ordinary HTTP failures carry no net:: code at all
            "Failed to load resource: the server responded with a status of 404 (Not Found)",
            "Failed to load resource: the server responded with a status of 500 (Internal Server Error)",
            // a broken asset URL or a misconfigured endpoint does produce a net:: code - and must still fail
            "Failed to load resource: net::ERR_NAME_NOT_RESOLVED",
            "Failed to load resource: net::ERR_CONNECTION_REFUSED",
            "Failed to load resource: net::ERR_ADDRESS_UNREACHABLE",
            "Failed to load resource: net::ERR_ABORTED",
            "Failed to load resource: net::ERR_CERT_AUTHORITY_INVALID",
            "Failed to load resource: net::ERR_BLOCKED_BY_CLIENT",
            // errors from the application itself, the guard's actual purpose
            "TypeError: Cannot read properties of undefined (reading 'key')",
            "Uncaught (in promise) Error: something went wrong",
            // an unlisted code must not match as an extension of a listed one
            "Failed to load resource: net::ERR_CONNECTION_RESET_BY_PEER",
        ).forEach {
            assertThat(isTransientError(it)).describedAs(it).isFalse()
        }
    }

    @Test
    fun `a transient error is tolerated and recorded instead of failing the test`() {
        ErrorReportingApp().use { app ->
            val page = app.start().page
            page.evaluate("() => console.error(${TRANSIENT.quoted()})")
            // listeners are dispatched while the calling thread is inside a Playwright call, so the predicate
            // has to keep the message loop turning rather than just sleep
            waitUntil(waitTime = Duration.ofMillis(50)) {
                page.title()
                app.transientErrors.isNotEmpty()
            }

            assertThat(app.transientErrors).containsExactly(TRANSIENT)
            // still part of the console log the test output prints
            assertThat(app.consoleMessages.map { it.text() }).contains(TRANSIENT)
            assertThat(app.pageErrors).isEmpty()
        }
    }

    @Test
    fun `a non-transient error still fails the test`() {
        ErrorReportingApp().use { app ->
            val page = app.start().page
            assertThatThrownBy {
                page.evaluate("() => console.error('Failed to load resource: net::ERR_CONNECTION_REFUSED')")
                // any further call pumps the message loop, so the listener runs even if it had not yet
                page.title()
            }.isInstanceOf(AssertionError::class.java)
                .hasMessageContaining("Caught logged error")
                .hasMessageContaining("ERR_CONNECTION_REFUSED")
        }
    }

    @Test
    fun `tolerating transient errors can be turned off`() {
        StrictErrorReportingApp().use { app ->
            val page = app.start().page
            assertThatThrownBy {
                page.evaluate("() => console.error(${TRANSIENT.quoted()})")
                page.title()
            }.isInstanceOf(AssertionError::class.java)
                .hasMessageContaining("Caught logged error")

            assertThat(app.transientErrors).isEmpty()
        }
    }

    @Test
    fun `an expected error takes precedence over transient handling`() {
        ErrorReportingApp().use { app ->
            app.expectError(TRANSIENT)
            val page = app.start().page
            page.evaluate("() => console.error(${TRANSIENT.quoted()})")
            page.title()

            assertThat(app.transientErrors).isEmpty()
        }
    }

    private fun String.quoted() = "'$this'"
}
