@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package au.concepta.playwright

import com.deque.html.axecore.playwright.AxeBuilder
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.PlaywrightException
import com.microsoft.playwright.TimeoutError
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.WaitForSelectorState
import au.concepta.playwright.util.retry
import org.opentest4j.AssertionFailedError

/**
 * Base class for page object representations within an application under test.
 *
 * Subclasses represent specific pages or screens, holding a reference to the Playwright [Page]
 * and providing assertion helpers specific to that page's domain. The [page] is expected to be
 * navigated to the relevant URL before this constructor is called.
 */
abstract class ApplicationPage<T : ApplicationPage<T>>(val page: Page, elementToWaitFor: Locator) {
    init {
        println("- ${this::class.java.simpleName} ${page.url()}")
        elementToWaitFor.waitFor()
    }

    /**
     * Reload the current page and return this page instance for method chaining.
     */
    fun reload(): T {
        page.reload()
        return downcast()
    }

    /*
     * To have the fluid API, we need to stay at the specific type, and we expect all implementations to reference
     * themselves. It would be nice to have a cleaner way of doing this, but we don't have one yet.
     */
    @Suppress("UNCHECKED_CAST")
    /**
     * Cast `this` to the concrete page type for fluid API support.
     */
    protected fun downcast(): T {
        return this as T
    }

    /**
     * Run accessibility checks via axe-core and fail the test if any violations are found.
     *
     * Returns this page instance for method chaining.
     */
    fun validateAccessibility(): T {
        val report = AxeBuilder(page).analyze()
        if(report.violations.isNotEmpty()) {
            throw AssertionError(
                "Accessibility violations found:\n" +
                        report.violations.joinToString("\n") { "[${it.id}] ${it.impact}: ${it.description}" }
            )
        }
        return downcast()
    }

    /**
     * Assert that the given element is not visible on the page.
     *
     * Uses Playwright's auto-retrying assertion (`isHidden()`) rather than a one-shot check,
     * so it waits for the element to actually disappear. Fails if multiple elements match
     * the locator (strict mode violation).
     */
    protected fun assertElementNotVisible(element: Locator, name: String) {
        // mirrors assertElementVisible below: wait via Playwright's auto-retrying assertion rather than
        // sampling isVisible once, which loses whenever the app hasn't caught up yet with hiding the element.
        // isHidden() also treats a detached element as hidden, matching frameworks that remove rather than hide.
        try {
            assertThat(element).isHidden()
        } catch (e: AssertionFailedError) {
            // unlike the one-shot isVisible sample this replaces, isHidden() reports a strict-mode violation
            // by wrapping it into this same AssertionFailedError rather than a distinct PlaywrightException
            if (e.message?.contains("strict mode violation") == true) {
                throw AssertionError("Expected $name to be not visible, but we found multiple", e)
            }
            throw AssertionError("Expected $name to be not visible, but it is", e)
        } catch (e: PlaywrightException) {
            // kept as a fallback in case a strict-mode violation ever surfaces unwrapped, as it does for isVisible
            if (e.message?.contains("strict mode violation") == true) {
                throw AssertionError("Expected $name to be not visible, but we found multiple", e)
            }
            throw e
        }
    }

    /**
     * Assert that the given element is visible on the page.
     *
     * Uses Playwright's auto-retrying assertion (`isVisible()`) rather than a one-shot check,
     * so it waits for the element to appear.
     */
    protected fun assertElementVisible(element: Locator, name: String) {
        // do not use element.isVisible() as it is flaky -- see https://playwright.dev/docs/api/class-locator#locator-is-visible
        try {
            assertThat(element).isVisible()
        } catch (e: AssertionFailedError) {
            throw AssertionError("Expected $name to be visible, but it is not")
        }
    }

    /**
     * Assert that the given element is disabled.
     *
     * Times out if the element is not found.
     */
    protected fun assertElementDisabled(element: Locator, name: String) {
        try {
            assertThat(element).isDisabled()
        } catch (e: AssertionFailedError) {
            throw AssertionError("Expected $name to be disabled, but it is not")
        } catch (e: TimeoutError) {
            throw AssertionError("Expected $name to be disabled, but it was not found")
        }
    }

    /**
     * Assert that the given element is enabled.
     *
     * Times out if the element is not found.
     */
    protected fun assertElementEnabled(element: Locator, name: String) {
        try {
            assertThat(element).isEnabled()
        } catch (e: AssertionFailedError) {
            throw AssertionError("Expected $name to be enabled, but it is not")
        } catch (e: TimeoutError) {
            throw AssertionError("Expected $name to be enabled, but it was not found")
        }
    }

    /**
     * Assert that the element's text content contains the expected value.
     *
     * @param element The locator for the element to check.
     * @param expected The expected text substring.
     * @param name Human-readable name of the element for error messages.
     * @param exact If `true`, requires an exact match; otherwise checks if the text contains the expected value.
     */
    protected fun assertTextContent(element: Locator, expected: String, name: String, exact: Boolean = false) {
        // textContent() waits only for the element to be attached, not for its text to settle, so sampling it
        // once loses whenever the app is still catching up. These assertions retry until the text matches.
        try {
            if (exact) assertThat(element).hasText(expected) else assertThat(element).containsText(expected)
        } catch (e: AssertionFailedError) {
            throw AssertionError("Expected $name to contain '$expected', but got '${element.textContent()}'", e)
        }
    }

    /**
     * Assert that a form field has the expected value.
     *
     * @param field The locator for the input field.
     * @param expected The expected input value.
     * @param fieldName Human-readable name of the field for error messages.
     */
    protected fun assertFieldContent(field: Locator, expected: String, fieldName: String) {
        // hasValue retries, unlike a one-shot inputValue() read
        try {
            assertThat(field).hasValue(expected)
        } catch (e: AssertionFailedError) {
            throw AssertionError("Expected $fieldName to be '$expected', but we got '${field.inputValue()}'", e)
        }
    }

    /**
     * Assert that a form field is not empty.
     *
     * @param field The locator for the input field.
     * @param fieldName Human-readable name of the field for error messages.
     */
    protected fun assertFieldNotEmpty(field: Locator, fieldName: String) {
        // hasValue retries, unlike a one-shot inputValue() read
        try {
            assertThat(field).not().hasValue("")
        } catch (e: AssertionFailedError) {
            throw AssertionError("Expected $fieldName to have a value, but it was empty", e)
        }
    }

    /**
     * Assert that a select element has the specified option selected.
     *
     * Matches by the option's `label` attribute, falling back to its text content.
     */
    protected fun assertSelectOption(selectId: String, value: String) {
        waitForSelectToBeLoaded(selectId)
        retryAssertion {
            val actual = selectedOptionLabel(selectId)
            if (actual != value) {
                throw AssertionError("Expected $selectId to have option '$value' selected, but it is '$actual'")
            }
        }
    }

    /**
     * Assert that a select element contains the specified option.
     */
    protected fun assertSelectHasOption(selectId: String, value: String) {
        waitForSelectToBeLoaded(selectId)
        retryAssertion {
            if (!optionLabels(selectId).contains(value)) {
                throw AssertionError("Expected $selectId to have option '$value', but it has not")
            }
        }
    }

    /**
     * Assert that a select element does not contain the specified option.
     *
     * Waits for the select's first option to be attached before checking.
     */
    protected fun assertSelectDoesNotHaveOption(selectId: String, value: String) {
        waitForSelectToBeLoaded(selectId)
        retryAssertion {
            if (optionLabels(selectId).contains(value)) {
                throw AssertionError("Expected $selectId to not have option '$value', but it has")
            }
        }
    }

    /**
     * Retries [check] until it passes or the attempts run out, then rethrows whatever it failed with last so the
     * message reports the final observed state rather than a generic "ran out of attempts".
     *
     * Used for the `select` assertions, which have no auto-retrying Playwright equivalent to delegate to the way
     * the element assertions above do.
     */
    private fun retryAssertion(check: () -> Unit) = retry(
        onFail = { lastError -> throw lastError ?: AssertionError("Assertion failed without reporting an error") },
        block = check,
    )

    private fun selectedOptionLabel(selectId: String): String {
        val option = page.locator("#$selectId > option:checked")
        return option.getAttribute("label")?.takeIf { it.isNotEmpty() } ?: option.textContent().trim()
    }

    private fun optionLabels(selectId: String): List<String> =
        page.locator("#$selectId > option").all().map { option ->
            option.textContent().trim().ifEmpty { option.getAttribute("label") ?: "" }
        }

    private fun waitForSelectToBeLoaded(selectId: String) {
        page.locator("#$selectId > option:first-child")
            .waitFor(Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED))
    }
}