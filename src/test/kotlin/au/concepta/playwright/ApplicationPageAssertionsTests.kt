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

/**
 * A page whose text, field value and select options all only settle a delay after `go` is clicked, rather than
 * immediately -- so the tests below can tell an assertion that samples once from one that actually waits.
 */
class DelayedContentPage(page: Page) : ApplicationPage<DelayedContentPage>(page, page.locator("body")) {
    fun triggerDelayedUpdate(): DelayedContentPage {
        page.locator("#go").click()
        return downcast()
    }

    fun assertLabelText(expected: String) = assertTextContent(page.locator("#label"), expected, "label")

    fun assertFieldValue(expected: String) = assertFieldContent(page.locator("#field"), expected, "field")

    fun assertFieldHasContent() = assertFieldNotEmpty(page.locator("#field"), "field")

    fun assertHasChoice(value: String) = assertSelectHasOption("choice", value)

    fun assertDoesNotHaveChoice(value: String) = assertSelectDoesNotHaveOption("choice", value)

    fun assertChosen(value: String) = assertSelectOption("choice", value)
}

class DelayedContentApp : Application<DelayedContentPage>() {
    private val delayMillis = 300

    override val defaultBaseUrl: String =
        "data:text/html,<html><body>" +
                "<div id='label'>pending</div>" +
                "<input id='field' value=''>" +
                "<select id='choice'></select>" +
                "<button id='go'>go</button>" +
                "<script>document.getElementById('go').addEventListener('click', () => setTimeout(() => {" +
                "document.getElementById('label').textContent = 'settled text';" +
                "document.getElementById('field').value = 'settled input';" +
                "document.getElementById('choice').innerHTML = " +
                "'<option>alpha</option><option>beta</option>';" +
                "}, $delayMillis))</script>" +
                "</body></html>"

    override fun getInitialApplicationPage(page: Page): DelayedContentPage = DelayedContentPage(page)
}

/**
 * Runs [block] against a freshly started [DelayedContentApp], always closing it again.
 */
private fun withDelayedContent(block: (DelayedContentPage) -> Unit) {
    val app = DelayedContentApp()
    try {
        block(app.start())
    } finally {
        app.close()
    }
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
            // two elements match `.target` here, so this exercises the strict-mode-violation path. isHidden()
            // wraps that into the AssertionFailedError it raises rather than letting a PlaywrightException
            // escape, which is why assertElementNotVisible checks for it inside both catch blocks.
            val error = assertFailsWith<AssertionError> { page.assertDuplicateNotVisible() }
            assertThat(error.message).contains("we found multiple")
        } finally {
            app.close()
        }
    }

    // Each assertion below is made right after the click that schedules the change, so the change has not
    // happened yet at that point -- a one-shot sample taken there would read the pre-change state and fail.

    @Test
    fun `assertTextContent waits for the text to settle`() = withDelayedContent { page ->
        page.triggerDelayedUpdate().assertLabelText("settled text")
    }

    @Test
    fun `assertFieldContent waits for the field value to settle`() = withDelayedContent { page ->
        page.triggerDelayedUpdate().assertFieldValue("settled input")
    }

    @Test
    fun `assertFieldNotEmpty waits for the field to be populated`() = withDelayedContent { page ->
        page.triggerDelayedUpdate().assertFieldHasContent()
    }

    @Test
    fun `assertSelectHasOption waits for the options to load`() = withDelayedContent { page ->
        // the select starts with no options at all, which is what the old querySelectorAll read saw
        page.triggerDelayedUpdate().assertHasChoice("beta")
    }

    @Test
    fun `assertSelectOption waits for the options to load`() = withDelayedContent { page ->
        // the browser selects the first option once they exist
        page.triggerDelayedUpdate().assertChosen("alpha")
    }

    // The waiting assertions report failure through catch blocks that rewrite the exception. These check the
    // rewriting does not swallow a genuine failure into a pass.

    @Test
    fun `assertTextContent still fails when the text never matches`() = withDelayedContent { page ->
        val error = assertFailsWith<AssertionError> { page.assertLabelText("never appears") }
        assertThat(error.message).contains("never appears").contains("pending")
    }

    @Test
    fun `assertFieldNotEmpty still fails when the field stays empty`() = withDelayedContent { page ->
        val error = assertFailsWith<AssertionError> { page.assertFieldHasContent() }
        assertThat(error.message).contains("field").contains("empty")
    }

    @Test
    fun `assertSelectHasOption still fails when the option never appears`() = withDelayedContent { page ->
        page.triggerDelayedUpdate().assertHasChoice("alpha")
        val error = assertFailsWith<AssertionError> { page.assertHasChoice("gamma") }
        assertThat(error.message).contains("gamma").contains("has not")
    }

    @Test
    fun `assertSelectDoesNotHaveOption still fails when the option is present`() = withDelayedContent { page ->
        page.triggerDelayedUpdate().assertHasChoice("alpha")
        val error = assertFailsWith<AssertionError> { page.assertDoesNotHaveChoice("alpha") }
        assertThat(error.message).contains("alpha").contains("but it has")
    }
}
