package au.concepta.playwright

import com.microsoft.playwright.Page
import com.microsoft.playwright.TimeoutError
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import kotlin.test.Test

private const val ELSEWHERE = "data:text/html,<html><body><p id='elsewhere'>Logged out</p></body></html>"

/** A start page that only matches the base URL, so waiting for it anywhere else times out. */
class StartOnlyPage(page: Page) : ApplicationPage<StartOnlyPage>(page, page.locator("#start"))

/** Exposes the protected accessors so the tests can reach them as a subclass would. */
class StartOnlyApp : Application<StartOnlyPage>() {
    override val defaultBaseUrl: String = "data:text/html,<html><body><p id='start'>Start</p></body></html>"
    override fun getInitialApplicationPage(page: Page): StartOnlyPage = StartOnlyPage(page)

    override fun configureNewPage(page: Page) {
        page.setDefaultTimeout(1_000.0)
    }

    fun page(): Page = currentPage
    fun goTo(url: String): Page = navigate(url)
    fun openHome(): StartOnlyPage = StartOnlyPage(navigate(baseUrl))
}

class CurrentPageTests {
    @Test
    fun `currentPage starts the test on the base URL if none is running`() {
        StartOnlyApp().use { app ->
            val page = app.page()
            assertThat(app.testRunning).isTrue()
            assertThat(page.locator("#start").isVisible).isTrue()
        }
    }

    @Test
    fun `currentPage returns the page the browser is on without checking the start page`() {
        StartOnlyApp().use { app ->
            val started = app.start().page
            started.navigate(ELSEWHERE)

            val current = app.page()
            assertThat(current).isSameAs(started)
            assertThat(current.locator("#elsewhere").isVisible).isTrue()
        }
    }

    @Test
    fun `start still checks the start page once the browser has left it`() {
        StartOnlyApp().use { app ->
            app.start().page.navigate(ELSEWHERE)
            assertThatThrownBy { app.start() }.isInstanceOf(TimeoutError::class.java)
        }
    }

    @Test
    fun `navigate leaves a page the start page does not model`() {
        StartOnlyApp().use { app ->
            app.start().page.navigate(ELSEWHERE)

            val home = app.openHome()
            assertThat(home.page.locator("#start").isVisible).isTrue()
        }
    }

    @Test
    fun `navigate starts the test if none is running`() {
        StartOnlyApp().use { app ->
            val page = app.goTo(ELSEWHERE)
            assertThat(app.testRunning).isTrue()
            assertThat(page.locator("#elsewhere").isVisible).isTrue()
        }
    }
}
