package au.concepta.playwright

import com.microsoft.playwright.Page
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * A page with a target element that becomes hidden [delayMillis] after `#hide` is clicked, rather than
 * immediately -- so tests here can tell an assertion that samples visibility once from one that actually
 * waits for it.
 */
class VisibilityTestPage(page: Page) : ApplicationPage<VisibilityTestPage>(page, page.locator("body")) {
    fun triggerDelayedHide(): VisibilityTestPage {
        page.locator("#hide").click()
        return downcast()
    }

    fun assertTargetNotVisible() = assertElementNotVisible(page.locator(".target"), "target")

    fun assertDuplicateNotVisible() = assertElementNotVisible(page.locator(".target"), "duplicate target")
}

class VisibilityTestApp : Application<VisibilityTestPage>() {
    private val delayMillis = 300

    override val defaultBaseUrl: String =
        "data:text/html,<html><body>" +
                "<div class='target'>still here</div>" +
                "<button id='hide'>hide</button>" +
                "<script>document.getElementById('hide').addEventListener('click', () => " +
                "setTimeout(() => document.querySelectorAll('.target').forEach(e => e.style.display = 'none'), $delayMillis))" +
                "</script>" +
                "</body></html>"

    override fun getInitialApplicationPage(page: Page): VisibilityTestPage = VisibilityTestPage(page)
}

class DuplicateTargetApp : Application<VisibilityTestPage>() {
    override val defaultBaseUrl: String =
        "data:text/html,<html><body>" +
                "<div class='target'>one</div>" +
                "<div class='target'>two</div>" +
                "</body></html>"

    override fun getInitialApplicationPage(page: Page): VisibilityTestPage = VisibilityTestPage(page)
}

class ApplicationPageAssertionsTests {

    @Test
    fun `assertElementNotVisible waits for the element to become hidden instead of sampling`() {
        val app = VisibilityTestApp()
        try {
            val page = app.start()
            // the click returns well before the setTimeout callback hides the element -- a one-shot
            // isVisible sample taken right here would still see it visible and fail spuriously
            page.triggerDelayedHide()
            page.assertTargetNotVisible()
        } finally {
            app.close()
        }
    }

    @Test
    fun `assertElementNotVisible reports strict mode violations distinctly`() {
        val app = DuplicateTargetApp()
        try {
            val page = app.start()
            // two elements match `.target` here, so this exercises the strict-mode-violation path,
            // which arrives as a PlaywrightException rather than an assertion failure
            val error = assertFailsWith<AssertionError> { page.assertDuplicateNotVisible() }
            assertThat(error.message).contains("we found multiple")
        } finally {
            app.close()
        }
    }
}
