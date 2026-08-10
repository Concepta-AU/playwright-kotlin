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
 *
 * Each thread shares a single Playwright driver process and browser instance per application subclass.
 * [close] releases the browser context associated with a test run; [TestBase] calls [close] for every registered
 * application after each test.
 */
abstract class Application<T: ApplicationPage<T>>: AutoCloseable {
    /**
     * Provide the default URL to open if no value was explicitly specified.
     */
    abstract val defaultBaseUrl: String

    private val browser: Browser
        get() = SharedBrowserManager.getOrCreateBrowser(this)

    internal fun createLaunchOptions(): BrowserType.LaunchOptions {
        val options = BrowserType.LaunchOptions()
        val speed = System.getenv()["VIEW_SPEED"]?.toDouble()
        options.headless = speed == null
        options.slowMo = speed
        return modifyBrowserLaunchOptions(options)
    }

    private lateinit var context: BrowserContext

    private fun createContext(): BrowserContext = browser.newContext(run {
            val options = Browser.NewContextOptions()
            options.recordVideoDir = System.getenv()["VIDEO_DIR"]?.let { Path.of(it) }
            options.locale = "en-AU"
            modifyBrowserContext(options)
        })

    /**
     * The effective base URL, resolved via [findBaseUrl]. Defaults to [defaultBaseUrl] but can be overridden
     * by the `BASE_URL` environment variable.
     */
    protected open val baseUrl: String get() = findBaseUrl()

    /**
     * Whether a test is currently running ([start] has been called and [stopTest] has not).
     *
     * Used by [TestBase] to determine whether to stop tracing and close the context after a test.
     */
    var testRunning = false
        private set

    /**
     * Console messages captured during the current test run.
     *
     * Printed to stdout after each test via [TestBase.printConsoleLogsAndErrors].
     */
    val consoleMessages: MutableList<ConsoleMessage> = mutableListOf()

    /**
     * Page errors captured during the current test run.
     *
     * Printed to stdout after each test. Errors that would normally fail the test are added here
     * only if they were registered with [expectError].
     */
    val pageErrors: MutableList<String> = mutableListOf()
    private val expectedErrors: MutableList<ErrorPredicate> = mutableListOf()

    /**
     * Transient transport errors tolerated during the current test run - see [tolerateTransientErrors].
     *
     * Each one is printed as it happens, and again after the test via [TestBase.printConsoleLogsAndErrors], so a
     * run that quietly swallowed twenty of them is not indistinguishable from a clean one.
     */
    val transientErrors: MutableList<String> = mutableListOf()

    /**
     * Whether browser errors reporting a *transient* transport failure are tolerated instead of failing the test.
     *
     * On by default. When the network hiccups the browser logs a console error such as
     * `Failed to load resource: net::ERR_NETWORK_CHANGED`, and without this whichever test happens to be running
     * dies on it - reporting a transport event rather than anything to do with what it was testing.
     * [isTransientError] decides which errors qualify: only those where the *connection* died, never those saying
     * the application asked for something wrong.
     *
     * Tolerating the error does not retry the request, so a test may still fail afterwards because its data never
     * arrived. That is the intent: it then fails on its own assertion, naming what it was actually doing.
     *
     * Override with `false` in a subclass to make every console and page error fail the test again.
     */
    protected open val tolerateTransientErrors: Boolean = true

    /**
     * Override the logic how a base URL is determined. Usually it is sufficient to just set the [defaultBaseUrl], but
     * replacing the implementation here can be used if calculations are needed.
     */
    protected open fun findBaseUrl(): String {
        if (System.getenv().containsKey("BASE_URL")) {
            return System.getenv()["BASE_URL"]!!
        }
        return defaultBaseUrl
    }

    /**
     * Can be used to adjust the options for the browser objects created. The object passed in is the options object
     * that would be normally used, it can be adjusted or replaced in this method. Adjusting is recommended to maintain
     * the standard logic used to handle command line options.
     */
    protected open fun modifyBrowserLaunchOptions(defaultOptions: BrowserType.LaunchOptions): BrowserType.LaunchOptions = defaultOptions

    /**
     * Can be used to adjust the options for the browser contexts created. The object passed in is the options object
     * that would be normally used, it can be adjusted or replaced in this method. Adjusting is recommended to maintain
     * the standard logic used to handle command line options.
     */
    protected open fun modifyBrowserContext(defaultOptions: Browser.NewContextOptions): Browser.NewContextOptions = defaultOptions

    private fun startTest() {
        context = createContext()
        context.setDefaultTimeout(10_000.0)
        context.tracing().start(
            Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
        )
        testRunning = true
    }

    /**
     * Register an error predicate that suppresses test failure for matching console errors or page errors.
     *
     * Errors matching the predicate are still recorded in [consoleMessages] or [pageErrors] respectively.
     */
    fun expectError(pred: ErrorPredicate) {
        expectedErrors += pred
    }

    /**
     * Register an exact error message that suppresses test failure for a matching console or page error.
     *
     * Equivalent to `expectError { it == message }`.
     */
    fun expectError(message: String) {
        expectError { it == message }
    }

    /**
     * Decide what to do with an error the browser reported: fail the test, or let it pass.
     *
     * Fails unless the error was registered with [expectError], or is a transient transport error and
     * [tolerateTransientErrors] is on - a tolerated one is recorded in [transientErrors] and printed straight
     * away, so it stays visible in the test output even if the test then passes.
     *
     * @return `true` if the caller should record the error as a regular error of the test run, `false` if it was
     *   tolerated as transient and has already been recorded in [transientErrors].
     */
    private fun checkError(kind: String, text: String): Boolean {
        if (expectedErrors.any { p -> p.invoke(text) }) return true
        if (tolerateTransientErrors && isTransientError(text)) {
            transientErrors += text
            println("Tolerated transient error (#${transientErrors.size} of this test): $text")
            return false
        }
        throw AssertionError("$kind: $text")
    }

    /**
     * Provide the representation of the page the application will start with.
     *
     * This should match the browser's view after the base URL was opened.
     */
    fun start(): T = getInitialApplicationPage(getBrowserPage())

    /**
     * Provide the representation of the page the application will start with.
     *
     * This should match the browser's view after the base URL was opened.
     */
    abstract fun getInitialApplicationPage(page: Page): T

    private fun getBrowserPage(): Page  = if (!testRunning) {
        startTest()
        context.tracing().group("Set up")
        try {
            val new = context.newPage()
            configureNewPage(new)
            new.onConsoleMessage {
                if (it.type() == "error") {
                    checkError("Caught logged error", it.text())
                }
                consoleMessages += it
            }
            new.onPageError {
                if (checkError("Caught page error", it)) {
                    pageErrors += it
                }
            }
            new.navigate(baseUrl)
            new
        } finally {
            context.tracing().groupEnd()
        }
    } else {
        context.pages().first()
    }

    /**
     * Configure a newly created page before navigation.
     *
     * Override this method to set up page-level listeners or other configuration.
     * Called by [getBrowserPage] after a new page is created but before it is navigated.
     */
    protected open fun configureNewPage(page: Page) {}

    /**
     * Stop the currently running test, closing its browser context and writing its trace.
     *
     * Does nothing if no test is running, so it is safe to call more than once. The browser itself stays open and
     * a subsequent [start] begins a new test in a fresh context; release the browser with [close].
     */
    fun stopTest(vararg hierarchicalName: String) {
        if (!testRunning) return
        testRunning = false
        try {
            if (hierarchicalName.isEmpty()) {
                context.tracing().stop()
            } else {
                val folders = hierarchicalName.dropLast(1)
                val file = hierarchicalName.last()
                val traceLoc = folders.fold(Path.of("traces")) { acc, cur -> acc.resolve(cur) }.resolve("$file.zip")
                context.tracing().stop(Tracing.StopOptions().setPath(traceLoc))
            }
        } finally {
            context.close()
        }
    }

    /**
     * Release application resources associated with the current test.
     *
     * Stops a still-running test and closes its browser context. The backing Playwright driver process and
     * Chromium instance are shared per thread and remain open for subsequent tests.
     */
    override fun close() {
        stopTest()
    }
}