@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package au.concepta.playwright

import com.microsoft.playwright.*
import java.nio.file.Path

typealias ErrorPredicate = (String) -> Boolean

/**
 * An instance of this class represents the interactions with an application under test.
 *
 * Each test interacts with one or more instances of subclasses of this class. Multiple different implementations of this
 * base class may exist to represent different applications of a larger system, for example, a point-of-sale and a
 * back-of-house system in a store, or a customer and an administration view of a single system.
 */
abstract class Application<T: ApplicationPage<T>> {
    /**
     * Provide the default URL to open if no value was explicitly specified.
     */
    abstract val defaultBaseUrl: String

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
    protected val baseUrl = findBaseUrl()
    var testRunning = false
        private set
    val consoleMessages: MutableList<ConsoleMessage> = mutableListOf()
    val pageErrors: MutableList<String> = mutableListOf()
    private val expectedErrors: MutableList<ErrorPredicate> = mutableListOf()

    protected fun findBaseUrl(): String {
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

    fun expectError(pred: ErrorPredicate) {
        expectedErrors += pred
    }

    fun expectError(message: String) {
        expectError { it == message }
    }

    /**
     * Provide the representation of the page the application will start with.
     *
     * This should match the browser's view after the base URL was opened.
     */
    fun start(): T = getInitialApplicationPage(getBrowserPage())

    abstract fun getInitialApplicationPage(page: Page): T

    private fun getBrowserPage(): Page  = if (!testRunning) {
        startTest()
        context.tracing().group("Set up")
        try {
            val new = context.newPage()
            configureNewPage(new)
            new.onConsoleMessage {
                if (it.type() == "error") {
                    if (!expectedErrors.any { p -> p.invoke(it.text()) }) {
                        throw AssertionError(
                            "Caught logged error: ${it.text()}"
                        )
                    }
                }
                consoleMessages += it
            }
            new.onPageError {
                if (!expectedErrors.any { p -> p.invoke(it) }) {
                    throw AssertionError(
                        "Caught page error: $it"
                    )
                }
                pageErrors += it
            }
            new.navigate(baseUrl)
            new
        } finally {
            context.tracing().groupEnd()
        }
    } else {
        context.pages().first()
    }

    protected open fun configureNewPage(page: Page) {}

    fun stopTest(vararg hierarchicalName: String) {
        if (hierarchicalName.isEmpty()) {
            context.tracing().stop()
        } else {
            val folders = hierarchicalName.dropLast(1)
            val file = hierarchicalName.last()
            val traceLoc = folders.fold(Path.of("traces")) { acc, cur -> acc.resolve(cur) }.resolve("$file.zip")
            context.tracing().stop(Tracing.StopOptions().setPath(traceLoc))
        }
        context.close()
    }
}