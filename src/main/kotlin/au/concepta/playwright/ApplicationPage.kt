@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package au.concepta.playwright

import com.deque.html.axecore.playwright.AxeBuilder
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.PlaywrightException
import com.microsoft.playwright.TimeoutError
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.WaitForSelectorState
import org.opentest4j.AssertionFailedError

abstract class ApplicationPage<T : ApplicationPage<T>>(val page: Page, elementToWaitFor: Locator) {
    init {
        println("- ${this::class.java.simpleName} ${page.url()}")
        elementToWaitFor.waitFor()
    }

    fun reload(): T {
        page.reload()
        return downcast()
    }

    /*
     * To have the fluid API, we need to stay at the specific type, and we expect all implementations to reference
     * themselves. It would be nice to have a cleaner way of doing this, but we don't have one yet.
     */
    @Suppress("UNCHECKED_CAST")
    protected fun downcast(): T {
        return this as T
    }

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

    protected fun assertElementNotVisible(element: Locator, name: String) {
        try {
            if (element.isVisible) {
                throw AssertionError("Expected $name to be not visible, but it is")
            }
        } catch (e: PlaywrightException) {
            if (e.message?.contains("strict mode violation") == true) {
                throw AssertionError("Expected $name to be not visible, but we found multiple", e)
            }
            throw e
        }
    }

    protected fun assertElementVisible(element: Locator, name: String) {
        // do not use element.isVisible() as it is flaky -- see https://playwright.dev/docs/api/class-locator#locator-is-visible
        try {
            assertThat(element).isVisible()
        } catch (e: AssertionFailedError) {
            throw AssertionError("Expected $name to be visible, but it is not")
        }
    }

    protected fun assertElementDisabled(element: Locator, name: String) {
        try {
            assertThat(element).isDisabled()
        } catch (e: AssertionFailedError) {
            throw AssertionError("Expected $name to be disabled, but it is not")
        } catch (e: TimeoutError) {
            throw AssertionError("Expected $name to be disabled, but it was not found")
        }
    }

    protected fun assertElementEnabled(element: Locator, name: String) {
        try {
            assertThat(element).isEnabled()
        } catch (e: AssertionFailedError) {
            throw AssertionError("Expected $name to be enabled, but it is not")
        } catch (e: TimeoutError) {
            throw AssertionError("Expected $name to be disabled, but it was not found")
        }
    }

    protected fun assertTextContent(element: Locator, expected: String, name: String, exact: Boolean = false) {
        val actual = element.textContent()
        val match = if (exact) actual == expected else actual.contains(expected)
        if (!match) {
            throw AssertionError("Expected $name to contain '$expected', but got '$actual'")
        }
    }

    protected fun assertFieldContent(field: Locator, expected: String, fieldName: String) {
        val actual = field.inputValue()
        if (expected != actual) {
            throw AssertionError("Expected asset to have $fieldName $expected, but we got $actual")
        }
    }

    protected fun assertFieldNotEmpty(field: Locator, fieldName: String) {
        val actual = field.inputValue()
        if (actual == "") {
            throw AssertionError("Expected asset to have nothing set for $fieldName")
        }
    }

    protected fun assertSelectOption(selectId: String, value: String) {
        val option = page.locator("#$selectId > option:checked")
        val actual = option.getAttribute("label").ifEmpty { option.textContent().trim() }
        if (actual != value) {
            throw AssertionError("Expected $selectId to have option '$value' selected, but it is '$actual'")
        }
    }

    protected fun assertSelectHasOption(selectId: String, value: String) {
        val options = page.querySelectorAll("#$selectId > option").map { it.textContent().trim() }
        if (!options.contains(value)) {
            throw AssertionError("Expected $selectId to have option '$value', but it has not")
        }
    }

    protected fun assertSelectDoesNotHaveOption(selectId: String, value: String) {
        waitForSelectToBeLoaded(selectId)
        val options = page.querySelectorAll("#$selectId > option").map {
            it.textContent().trim().ifEmpty { it.getAttribute("label") }
        }
        if (options.contains(value)) {
            throw AssertionError("Expected $selectId to not have option '$value', but it has")
        }
    }

    private fun waitForSelectToBeLoaded(selectId: String) {
        page.locator("#$selectId > option:first-child")
            .waitFor(Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED))
    }
}