package au.concepta.playwright

import com.microsoft.playwright.ConsoleMessage
import com.microsoft.playwright.Page
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import kotlin.test.Test

private const val GUARDED_WARNING = "NG0956: tracked collection re-created"

/** Judges warnings carrying [GUARDED_WARNING]'s code on top of the default console errors. */
class WarningGuardingApp : Application<DummyPage>() {
    override val defaultBaseUrl: String = "data:text/html,<html><body>Warnings</body></html>"
    override fun getInitialApplicationPage(page: Page): DummyPage = DummyPage(page)
    override fun isGuarded(message: ConsoleMessage) =
        super.isGuarded(message) || (message.type() == "warning" && "NG0956" in message.text())
}

/**
 * The console guard: which console messages it judges, what an expected or transient one does, and that every page
 * error is recorded. [TransientErrorsTests] covers the transient classification itself.
 *
 * A listener runs while the test thread is inside a Playwright call, so each test makes one more call
 * (`page.title()`) after the message is logged, to have it dispatched.
 */
class ConsoleGuardTests {
    @Test
    fun `a warning is not judged by default`() {
        ErrorReportingApp().use { app ->
            val page = app.start().page
            page.evaluate("() => console.warn('${GUARDED_WARNING}')")
            page.title()

            assertThat(app.consoleMessages.map { it.text() }).contains(GUARDED_WARNING)
        }
    }

    @Test
    fun `a guarded warning fails the test`() {
        WarningGuardingApp().use { app ->
            val page = app.start().page
            assertThatThrownBy {
                page.evaluate("() => console.warn('${GUARDED_WARNING}')")
                page.title()
            }.isInstanceOf(AssertionError::class.java)
                .hasMessage("Caught logged warning: $GUARDED_WARNING")

            // recorded before it was judged, so the printed console log shows it
            assertThat(app.consoleMessages.map { it.text() }).contains(GUARDED_WARNING)
        }
    }

    @Test
    fun `a warning the override does not select still passes`() {
        WarningGuardingApp().use { app ->
            val page = app.start().page
            page.evaluate("() => console.warn('some other warning')")
            page.title()

            assertThat(app.consoleMessages.map { it.text() }).contains("some other warning")
        }
    }

    @Test
    fun `the override keeps the default's console errors`() {
        WarningGuardingApp().use { app ->
            val page = app.start().page
            assertThatThrownBy {
                page.evaluate("() => console.error('boom')")
                page.title()
            }.isInstanceOf(AssertionError::class.java)
                .hasMessage("Caught logged error: boom")
        }
    }

    @Test
    fun `an expected guarded warning does not fail the test`() {
        WarningGuardingApp().use { app ->
            app.expect { it.startsWith("NG0956") }
            val page = app.start().page
            page.evaluate("() => console.warn('${GUARDED_WARNING}')")
            page.title()

            assertThat(app.consoleMessages.map { it.text() }).contains(GUARDED_WARNING)
        }
    }

    @Test
    fun `transient tolerance applies to a guarded message of any type`() {
        val transient = "Failed to load resource: net::ERR_NETWORK_CHANGED"
        val app = object : Application<DummyPage>() {
            override val defaultBaseUrl: String = "data:text/html,<html><body>Warnings</body></html>"
            override fun getInitialApplicationPage(page: Page): DummyPage = DummyPage(page)
            override fun isGuarded(message: ConsoleMessage) = true
        }
        app.use {
            val page = it.start().page
            page.evaluate("() => console.warn('$transient')")
            page.title()

            assertThat(it.transientErrors).containsExactly(transient)
        }
    }

    @Test
    fun `a failing page error is recorded`() {
        ErrorReportingApp().use { app ->
            val page = app.start().page
            assertThatThrownBy {
                page.evaluate("() => setTimeout(() => { throw new Error('uncaught') })")
                page.waitForTimeout(100.0)
                page.title()
            }.isInstanceOf(AssertionError::class.java)
                .hasMessageContaining("Caught page error")

            assertThat(app.pageErrors).hasSize(1)
            assertThat(app.pageErrors.single()).contains("uncaught")
        }
    }

    @Test
    fun `an expected page error is recorded and passes`() {
        ErrorReportingApp().use { app ->
            app.expect { it.contains("uncaught") }
            val page = app.start().page
            page.evaluate("() => setTimeout(() => { throw new Error('uncaught') })")
            page.waitForTimeout(100.0)
            page.title()

            assertThat(app.pageErrors).hasSize(1)
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun `the deprecated expectError still works`() {
        ErrorReportingApp().use { app ->
            app.expectError("boom")
            app.expectError { it.startsWith("bang") }
            val page = app.start().page
            page.evaluate("() => { console.error('boom'); console.error('bang!') }")
            page.title()

            assertThat(app.consoleMessages.map { it.text() }).contains("boom", "bang!")
        }
    }
}
