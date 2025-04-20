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

fun havingName(name: String) = Page.GetByRoleOptions().setName(name)!!

fun Locator.setInputValue(value: String) {
    clear()
    fill(value)
}

// based on https://github.com/microsoft/playwright/issues/10667#issuecomment-2051477138
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

// assumption: we are not running from a JAR so we can just use a path to the `src/test/resources` files
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
