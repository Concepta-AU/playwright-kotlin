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
     * Page errors (uncaught exceptions in the page) captured during the current test run - every one of them, whether
     * it was expected, tolerated as transient, or failed the test.
     *
     * Printed to stdout after each test via [TestBase.printConsoleLogsAndErrors].
     */
    val pageErrors: MutableList<String> = mutableListOf()
    private val expected: MutableList<ErrorPredicate> = mutableListOf()

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
     * Whether a test run records a Playwright trace.
     *
     * On by default, and the right answer for an ordinary test: the trace is what explains a failure afterwards.
     * Turn it off for a test that repeats one flow hundreds of times - a soak test (see [soak]). Tracing snapshots
     * the DOM on every action, which both bloats the trace to little purpose and does in-page work of its own,
     * skewing the very counters such a test asserts on.
     *
     * Turn it back on in a subclass, or for a single run, to diagnose a soak failure; `SAVE_ALL_TRACES` still
     * decides whether a passing test's trace is kept.
     */
    protected open val recordTracing: Boolean = true

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
        if (recordTracing) {
            context.tracing().start(
                Tracing.StartOptions()
                    .setScreenshots(true)
                    .setSnapshots(true)
            )
        }
        // the previous test's probe belongs to a context that has since been closed
        memoryProbe = null
        testRunning = true
    }

    /**
     * Whether the console guard judges [message]. Page errors are always judged.
     *
     * A judged message fails the test unless it matches a predicate registered with [expect], or is a transient
     * transport error while [tolerateTransientErrors] is on. A message that is not judged is only recorded in
     * [consoleMessages].
     *
     * By default the guard judges console errors. Override to judge more - for example a warning a framework emits
     * for a defect it cannot report as an error - and keep the default by calling `super`:
     *
     * ```kotlin
     * override fun isGuarded(message: ConsoleMessage) =
     *     super.isGuarded(message) || (message.type() == "warning" && "NG0956" in message.text())
     * ```
     *
     * Only the selection is overridable: how a judged message is handled stays the same for every application, so an
     * override cannot switch off [expect] or the transient-error tolerance.
     */
    protected open fun isGuarded(message: ConsoleMessage): Boolean = message.type() == "error"

    /**
     * Register a predicate that keeps a matching judged console message or page error from failing the test.
     *
     * Matching messages and errors are still recorded in [consoleMessages] and [pageErrors]. Which console messages
     * are judged at all is decided by [isGuarded].
     */
    fun expect(pred: ErrorPredicate) {
        expected += pred
    }

    /**
     * Register an exact text that keeps a matching judged console message or page error from failing the test.
     *
     * Equivalent to `expect { it == message }`.
     */
    fun expect(message: String) {
        expect { it == message }
    }

    /** Equivalent to [expect]; renamed because it also covers judged warnings, not only errors. */
    @Deprecated("Renamed: it also covers judged warnings, not only errors", ReplaceWith("expect(pred)"))
    fun expectError(pred: ErrorPredicate) = expect(pred)

    /** Equivalent to [expect]; renamed because it also covers judged warnings, not only errors. */
    @Deprecated("Renamed: it also covers judged warnings, not only errors", ReplaceWith("expect(message)"))
    fun expectError(message: String) = expect(message)

    private enum class Verdict { EXPECTED, TRANSIENT, FAILURE }

    private fun verdictOn(text: String): Verdict = when {
        expected.any { it(text) } -> Verdict.EXPECTED
        tolerateTransientErrors && isTransientError(text) -> Verdict.TRANSIENT
        else -> Verdict.FAILURE
    }

    /**
     * Act on a judged console message or page error, already recorded by the caller.
     *
     * An expected one passes silently. A transient one is recorded in [transientErrors] and printed straight away,
     * so it stays visible in the test output even if the test then passes. Anything else fails the test: the
     * [AssertionError] thrown here, inside Playwright's event dispatch, surfaces from whichever Playwright call the
     * test is making.
     */
    private fun judge(kind: String, text: String) {
        when (verdictOn(text)) {
            Verdict.EXPECTED -> {}
            Verdict.TRANSIENT -> {
                transientErrors += text
                println("Tolerated transient error (#${transientErrors.size} of this test): $text")
            }
            Verdict.FAILURE -> throw AssertionError("$kind: $text")
        }
    }

    /**
     * Provide the representation of the page the application will start with.
     *
     * This should match the browser's view after the base URL was opened: the result is built with
     * [getInitialApplicationPage], so it waits for the start page. If the test is already running and the browser
     * may be somewhere else, use [currentPage] or [navigate] instead.
     */
    fun start(): T = getInitialApplicationPage(getBrowserPage())

    /**
     * The browser page of the current test, without checking it against the start page.
     *
     * Starts the test, opening the base URL, if none is running yet. Use it in subclasses to act on whatever page the
     * browser is showing, then wrap the result in the page object expected afterwards.
     */
    protected val currentPage: Page get() = getBrowserPage()

    /**
     * Navigate the current browser page to [url] and return it, starting the test first if none is running.
     *
     * Unlike `start().page.navigate(url)` this does not wait for the start page before leaving, so it works from any
     * page the browser happens to be on. Wrap the result in the page object the navigation leads to:
     *
     * ```kotlin
     * fun openHome(): HomePage = HomePage(navigate(baseUrl))
     * ```
     */
    protected fun navigate(url: String): Page = currentPage.also { it.navigate(url) }

    /**
     * Provide the representation of the page the application will start with.
     *
     * This should match the browser's view after the base URL was opened.
     */
    abstract fun getInitialApplicationPage(page: Page): T

    private fun getBrowserPage(): Page  = if (!testRunning) {
        startTest()
        if (recordTracing) context.tracing().group("Set up")
        try {
            val new = context.newPage()
            configureNewPage(new)
            new.onConsoleMessage {
                consoleMessages += it
                if (isGuarded(it)) judge("Caught logged ${it.type()}", it.text())
            }
            new.onPageError {
                pageErrors += it
                judge("Caught page error", it)
            }
            new.navigate(baseUrl)
            new
        } finally {
            if (recordTracing) context.tracing().groupEnd()
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
        memoryProbe = null
        try {
            // nothing to stop or write when the test was not recording - see recordTracing
            if (recordTracing) {
                if (hierarchicalName.isEmpty()) {
                    context.tracing().stop()
                } else {
                    val folders = hierarchicalName.dropLast(1)
                    val file = hierarchicalName.last()
                    val traceLoc = folders.fold(Path.of("traces")) { acc, cur -> acc.resolve(cur) }.resolve("$file.zip")
                    context.tracing().stop(Tracing.StopOptions().setPath(traceLoc))
                }
            }
        } finally {
            context.close()
        }
    }

    private var memoryProbe: MemoryProbe? = null

    /**
     * Reads the browser's retained footprint - DOM nodes, event listeners, documents and heap size - after
     * settling garbage collection.
     *
     * Needs a running test: the reading is taken on the page [start] opened. Chromium only, since the measurement
     * goes over the Chrome DevTools Protocol.
     */
    fun measureMemory(): MemoryMetrics {
        check(testRunning) { "Memory can only be measured while a test is running - call start() first." }
        val probe =
            memoryProbe ?: MemoryProbe(context.pages().first(), browser.browserType().name()).also {
                memoryProbe = it
            }
        return probe.measure()
    }

    /**
     * Runs [flow] repeatedly in the current browser context - never reloading - and measures what it retains.
     *
     * A single-page application is never reloaded after login, so anything a flow forgets to release accumulates
     * for the whole working session. An ordinary test cannot see that: it runs in a fresh context and lives for
     * seconds. Repeating one flow in one context and comparing counters across it can, provided two things:
     *
     *  - the flow loops over the *same* data, so growth that is simply the application caching what it was asked
     *    for is not read as a leak;
     *  - the baseline is taken after [warmup] iterations, because the first few legitimately allocate - root
     *    services, lazily created singletons, first-render DOM.
     *
     * Judge the result with [assertNoLeak], which prints the trend so a one-off step is distinguishable from a
     * straight line. Consider turning [recordTracing] off for the test: hundreds of traced actions cost a large
     * trace and perturb the measurement.
     *
     * @param iterations runs of [flow] after the warm-up.
     * @param warmup runs of [flow] before the baseline reading.
     * @param checkpointEvery how often, in iterations, to take an intermediate reading.
     */
    fun soak(
        iterations: Int = 100,
        warmup: Int = 5,
        checkpointEvery: Int = 10,
        flow: () -> Unit,
    ): SoakResult {
        require(iterations > 0) { "A soak run needs at least one iteration, not $iterations" }
        require(warmup >= 0) { "The warm-up cannot be negative, but was $warmup" }
        require(checkpointEvery > 0) { "Checkpoints cannot be taken every $checkpointEvery iterations" }
        repeat(warmup) { flow() }
        val baseline = measureMemory()
        val checkpoints = mutableListOf<SoakCheckpoint>()
        for (iteration in 1..iterations) {
            flow()
            if (iteration % checkpointEvery == 0 && iteration < iterations) {
                checkpoints += SoakCheckpoint(iteration, measureMemory())
            }
        }
        return SoakResult(warmup, iterations, baseline, checkpoints, measureMemory())
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