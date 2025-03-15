@file:Suppress("unused")

package au.concepta.playwright

import com.microsoft.playwright.*
import java.nio.file.Path

/**
 * An instance of this class represents the interactions with an application under test.
 *
 * Each test interacts with one or more instances of subclasses of this class. Multiple different implementation of this
 * base class may exist to represent different applications of a larger system, for example a point-of-sale and a
 * back-of-house system in a store, or a customer and an administration view of a single system.
 */
abstract class Application<T: ApplicationPage<T>> {
    /**
     * Provide the default URL to open if no value was explicitly specified.
     */
    abstract val defaultBaseUrl: String
    val consoleMessages: MutableList<ConsoleMessage> = mutableListOf()
    var expectErrors = false

    private val playwright = Playwright.create()!!
    private val browser = playwright.chromium().launch(run {
        val options = BrowserType.LaunchOptions()
        val speed = System.getenv()["VIEW_SPEED"]?.toDouble()
        options.headless = speed == null
        options.slowMo = speed
        options
    })
    private val context = browser.newContext(run {
        val options = Browser.NewContextOptions()
        options.recordVideoDir = System.getenv()["VIDEO_DIR"]?.let { Path.of(it) }
        options.locale = "en-AU"
        options
    })
    private var page: ApplicationPage<*>? = null
    private val baseUrl = findBaseUrl()
    private var testRunning = false

    private fun findBaseUrl(): String {
        if (System.getenv().containsKey("BASE_URL")) {
            return System.getenv()["BASE_URL"]!!
        }
        return defaultBaseUrl
    }

    fun startTest() {
        context.setDefaultTimeout(10_000.0)
        context.tracing().start(
            Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
        )
        testRunning = true
    }

    /**
     * Provide the representation of the page the application will start with.
     *
     * This should match the browser's view after the base URL was opened.
     */
    fun start(): T = getInitialApplicationPage(getBrowserPage())

    abstract fun getInitialApplicationPage(page: Page): T

    private fun getBrowserPage(): Page {
        val page = if (!testRunning) {
            startTest()
            val new = context.newPage()
            new.onConsoleMessage {
                if (it.type() == "error") {
                    if (!expectErrors) {
                        throw AssertionError(
                            "Caught browser error:\n${it.text()}"
                        )
                    }
                }
                consoleMessages += it
            }
            new
        } else {
            context.pages().first()
        }
        page.navigate(baseUrl, Page.NavigateOptions().setTimeout(20_000.0))
        return page
    }

    fun stopTest(vararg hierarchicalName: String) {
        if (hierarchicalName.isEmpty()) {
            context.tracing().stop()
        } else {
            val folders = hierarchicalName.dropLast(1)
            val file = hierarchicalName.last()
            val traceLoc = folders.fold(Path.of("traces")) { acc, cur -> acc.resolve(cur) }.resolve("$file.zip")
            context.tracing().stop(Tracing.StopOptions().setPath(traceLoc))
        }
    }

    fun reload() {
        page?.page?.reload()
    }
}