package au.concepta.playwright

import com.microsoft.playwright.Page
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * A page with a target element that becomes hidden [VisibilityTestApp.delayMillis] after `#hide` is clicked, rather than
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
 * A page whose class, focus, count, attribute, and checked state all only settle a delay after `go` is clicked,
 * rather than immediately -- so the tests below can tell an assertion that samples once from one that actually waits.
 */
class DelayedStatePage(page: Page) : ApplicationPage<DelayedStatePage>(page, page.locator("body")) {
    fun triggerDelayedUpdate(): DelayedStatePage {
        page.locator("#go").click()
        return downcast()
    }

    fun assertHasClass(cls: String) = assertElementHasClass(page.locator("#target"), cls, "target")

    fun assertDoesNotHaveClass(cls: String) = assertElementDoesNotHaveClass(page.locator("#target"), cls, "target")

    fun assertFocused() = assertElementFocused(page.locator("#input"), "input")

    fun assertCount(count: Int) = assertElementCount(page.locator(".item"), count, "items")

    fun assertAttr(name: String, value: String) = assertAttribute(page.locator("#target"), name, value, "target")

    fun assertChecked() = assertElementChecked(page.locator("#check"), "checkbox")
}

class DelayedStateApp : Application<DelayedStatePage>() {
    private val delayMillis = 300

    override val defaultBaseUrl: String =
        "data:text/html,<html><body>" +
                "<div id='target' class='initial'>box</div>" +
                "<input id='input'/>" +
                "<div class='item'>one</div>" +
                "<input id='check' type='checkbox'/>" +
                "<button id='go'>go</button>" +
                "<script>document.getElementById('go').addEventListener('click', () => setTimeout(() => {" +
                "var t = document.getElementById('target');" +
                "t.classList.remove('initial');" +
                "t.classList.add('active');" +
                "t.setAttribute('data-state', 'ready');" +
                "document.getElementById('input').focus();" +
                "var d = document.createElement('div');" +
                "d.className = 'item';" +
                "d.textContent = 'two';" +
                "document.body.appendChild(d);" +
                "document.getElementById('check').checked = true;" +
                "}, $delayMillis))</script>" +
                "</body></html>"

    override fun getInitialApplicationPage(page: Page): DelayedStatePage = DelayedStatePage(page)
}

/**
 * Runs [block] against a freshly started [DelayedStateApp], always closing it again.
 */
private fun withDelayedState(block: (DelayedStatePage) -> Unit) {
    val app = DelayedStateApp()
    try {
        block(app.start())
    } finally {
        app.close()
    }
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

    // --- class assertions ---

    @Test
    fun `assertElementHasClass waits for the class to appear`() = withDelayedState { page ->
        page.triggerDelayedUpdate().assertHasClass("active")
    }

    @Test
    fun `assertElementHasClass fails when the class is absent`() = withDelayedState { page ->
        val error = assertFailsWith<AssertionError> { page.assertHasClass("missing") }
        assertThat(error.message).contains("missing").contains("does not")
    }

    @Test
    fun `assertElementDoesNotHaveClass waits for the class to disappear`() = withDelayedState { page ->
        page.triggerDelayedUpdate().assertDoesNotHaveClass("initial")
    }

    @Test
    fun `assertElementDoesNotHaveClass fails when the class is present`() = withDelayedState { page ->
        val error = assertFailsWith<AssertionError> { page.assertDoesNotHaveClass("initial") }
        assertThat(error.message).contains("initial").contains("but it does")
    }

    // --- focus assertion ---

    @Test
    fun `assertElementFocused waits for the element to receive focus`() = withDelayedState { page ->
        page.triggerDelayedUpdate().assertFocused()
    }

    @Test
    fun `assertElementFocused fails when the element is not focused`() = withDelayedState { page ->
        val error = assertFailsWith<AssertionError> { page.assertFocused() }
        assertThat(error.message).contains("input").contains("not")
    }

    // --- count assertion ---

    @Test
    fun `assertElementCount waits for the count to stabilize`() = withDelayedState { page ->
        page.triggerDelayedUpdate().assertCount(2)
    }

    @Test
    fun `assertElementCount fails when the count does not match`() = withDelayedState { page ->
        val error = assertFailsWith<AssertionError> { page.assertCount(5) }
        assertThat(error.message).contains("5").contains("1")
    }

    // --- attribute assertion ---

    @Test
    fun `assertAttribute waits for the attribute to appear`() = withDelayedState { page ->
        page.triggerDelayedUpdate().assertAttr("data-state", "ready")
    }

    @Test
    fun `assertAttribute fails when the attribute does not match`() = withDelayedState { page ->
        val error = assertFailsWith<AssertionError> { page.assertAttr("data-state", "wrong") }
        assertThat(error.message).contains("data-state").contains("wrong")
    }

    // --- checked assertion ---

    @Test
    fun `assertElementChecked waits for the element to become checked`() = withDelayedState { page ->
        page.triggerDelayedUpdate().assertChecked()
    }

    @Test
    fun `assertElementChecked fails when the element is not checked`() = withDelayedState { page ->
        val error = assertFailsWith<AssertionError> { page.assertChecked() }
        assertThat(error.message).contains("checkbox").contains("not")
    }
}
