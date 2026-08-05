@file:Suppress("unused")

package au.concepta.playwright.util

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.io.path.readBytes

/*
This file collects helper functions that make using Playwright's objects easier in Kotlin.
 */

@Deprecated("Use the 'name' parameter on Page.getByRole() instead, e.g. getByRole(role, name = \"foo\")", level = DeprecationLevel.WARNING)
fun havingName(name: String) = Page.GetByRoleOptions().setName(name)!!

/**
 * Clear the current value and set a new value on an input element.
 *
 * Equivalent to calling `clear()` followed by `fill(value)`.
 */
fun Locator.setInputValue(value: String) {
    clear()
    fill(value)
}

// based on https://github.com/microsoft/playwright/issues/10667#issuecomment-2051477138
/**
 * Dispatch a file drop event on the element using the file at the given path.
 *
 * The file contents are read, base64-encoded, and passed to a JavaScript snippet that creates
 * a `File` object and adds it to a `DataTransfer` instance, which is then used to dispatch
 * a `drop` event. Useful for testing drag-and-drop file upload flows.
 */
@OptIn(ExperimentalEncodingApi::class)
fun Locator.dropFile(path: Path) {
    val dataTransfer = page().evaluateHandle(
        """
            (encodedString) => {
                const dt = new DataTransfer();
                const hexString = Uint8Array.from(atob(encodedString), c => c.charCodeAt(0));
                const file = new File([hexString], '${path.fileName}', { type: '${Files.probeContentType(path)}' });
                dt.items.add(file);return dt;}
        """.trimIndent(),
        Base64.Mime.encode(path.readBytes())
    )
    dispatchEvent("drop", mapOf("dataTransfer" to dataTransfer))
}

/**
 * Drop a file from `src/test/resources` via a drag-and-drop event.
 *
 * Resolves the resource path from the classpath and delegates to [dropFile].
 * Assumes the project is not packaged as a JAR.
 */
fun Locator.dropResourceFile(resourceName: String) =
    dropFile(Path.of(Locator::class.java.getResource("/$resourceName")!!.toURI()))

/**
 * Gets the text directly contained in an element, ignoring child nodes.
 *
 * See https://stackoverflow.com/a/77767612 and https://stackoverflow.com/a/58187850
 */
fun Locator.directText() = evaluate(
    "element => Array.prototype.filter" +
            "    .call(element.childNodes, (child) => child.nodeType === Node.TEXT_NODE)" +
            "    .map((child) => child.textContent)" +
            "    .join('')"
).toString()

/**
 * Quotes the text and escapes single quotes for XPath expressions.
 *
 * XPath 1.0 (which is what we have available) doesn't have any good way of escaping single quotes at all. We follow
 * this advice: https://www.seleniumtests.com/2010/08/xpath-and-single-quotes.html
 */
fun String.quoteForXPath() = if (contains('\'')) {
    "concat(" + this.split('\'').joinToString(", \"'\", ") { "'$it'" } + ")"
} else {
    "'$this'"
}
