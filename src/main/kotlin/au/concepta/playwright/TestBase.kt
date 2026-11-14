@file:Suppress("unused")

package au.concepta.playwright

import com.microsoft.playwright.ConsoleMessage
import java.util.concurrent.atomic.AtomicInteger
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * Whether to record traces for all tests, not just failed ones.
 *
 * Controlled by the `SAVE_ALL_TRACES` environment variable. When disabled, traces are only captured for failed tests.
 */
val saveAllTraces: Boolean = "true" == System.getenv("SAVE_ALL_TRACES")

/**
 * Default strategy for generating test IDs, resolved from the `TEST_ID_STRATEGY` environment variable.
 */
val defaultTestIdStrategy: TestIdGenerator.Strategy =
    System.getenv("TEST_ID_STRATEGY")?.let { raw ->
        runCatching { TestIdGenerator.Strategy.valueOf(raw.uppercase()) }.getOrNull()
    } ?: TestIdGenerator.Strategy.COMPACT

/**
 * Base class for test fixtures that manage application instances and browser lifecycle.
 *
 * Subclasses register their application instances via [registerApplication]. A [TestBase]
 * instance is typically injected into tests via JUnit 5's `@RegisterExtension`, which ensures
 * browser contexts are closed and traces are written after each test.
 */
open class TestBase<T: Application<*>> {
    private val applications: MutableMap<String, T> = mutableMapOf()

    /**
     * Strategy used to generate [testId].
     *
     * Defaults to the value of the `TEST_ID_STRATEGY` environment variable (if set and valid),
     * or [TestIdGenerator.Strategy.COMPACT]. Subclasses can override this property.
     */
    protected open val testIdStrategy: TestIdGenerator.Strategy = defaultTestIdStrategy

    private var _testId: String? = null
    private val subIdCounter = AtomicInteger(0)

    /**
     * Unique identifier for this test execution.
     *
     * Generated on first access using [testIdStrategy]. Subsequent accesses within the same test
     * method return the same identifier. Automatically reset after each test.
     */
    val testId: String
        get() = _testId ?: TestIdGenerator.nextId(testIdStrategy).also { _testId = it }

    /**
     * Generate a unique string prefixed with [prefix] for test data naming.
     *
     * If [tag] is provided, it is incorporated into the ID to create distinct values for different
     * entities within the same test.
     *
     * Examples:
     * - `uniqueName("user")` -> `"user-4kvt31"`
     * - `uniqueName("user", "buyer")` -> `"user-4kvt31-buyer"`
     * - `uniqueName()` -> `"4kvt31"`
     *
     * @param prefix Optional prefix for the generated name.
     * @param tag Optional tag to identify a specific entity within the test.
     * @return A formatted unique name.
     */
    fun uniqueName(prefix: String = "", tag: String? = null): String {
        val base = if (tag != null) uniqueSubId(tag) else testId
        return if (prefix.isEmpty()) base else "$prefix-$base"
    }

    /**
     * Generate an additional unique identifier scoped to this test.
     *
     * Useful when a single test creates multiple distinct records or entities of the same type.
     *
     * Examples:
     * - `uniqueSubId("buyer")` -> `"4kvt31-buyer"`
     * - `uniqueSubId()` -> `"4kvt31-1"`, `"4kvt31-2"`
     *
     * @param tag Optional tag to identify the entity. If null, a sequential sub-counter is used.
     * @return A unique sub-identifier scoped to [testId].
     */
    fun uniqueSubId(tag: String? = null): String =
        if (tag != null) "$testId-$tag" else "$testId-${subIdCounter.incrementAndGet().toString(36)}"

    /**
     * The default application identifier used by [registerApplication] and [getApplication]
     * when no explicit id is provided.
     */
    protected open val defaultApplicationId: String = "default"

    /**
     * Hand an application over to this base class, which closes its context after the test.
     *
     * Register an application immediately after constructing it and before any further set-up such as logging in.
     */
    fun registerApplication(application: T, id: String = defaultApplicationId) {
        applications[id] = application
    }

    /**
     * Retrieve a previously registered application by its identifier.
     *
     * @param id The application identifier (defaults to [defaultApplicationId]).
     * @return The registered application instance.
     * @throws IllegalArgumentException if no application is registered with the given id.
     */
    fun getApplication(id: String = defaultApplicationId): T = applications[id] ?:
        throw IllegalArgumentException("No application registered with id: $id")

    /**
     * JUnit 5 extension that handles browser context cleanup and trace writing after each test.
     */
    @RegisterExtension
    val appShutdown = AppShutdown(applications)

    /**
     * Print captured console logs and page errors for all registered applications after each test.
     *
     * Output is prefixed with the application id and grouped by console messages and page errors.
     */
    @AfterEach
    fun printConsoleLogsAndErrors() {
        applications.forEach { (id, app) ->
            if (app.consoleMessages.isNotEmpty()) {
                println("==== Browser Console Log for $id ====")
                app.consoleMessages.forEach {
                    println(consoleLine(it))
                }
                println()
            }
            if (app.pageErrors.isNotEmpty()) {
                println("==== Page Errors Caught in $id ====")
                app.pageErrors.forEach {
                    println(" $it")
                }
                println()
            }
            if (app.transientErrors.isNotEmpty()) {
                // These did not fail the test (see Application.tolerateTransientErrors), so this summary is the
                // only way a reader of the output learns that the run was not a clean one.
                println("==== ${app.transientErrors.size} Transient Error(s) Tolerated in $id ====")
                app.transientErrors.forEach {
                    println(" $it")
                }
                println()
            }
        }
    }

    /**
     * Reset the test identifier and sub-identifier counter after each test.
     */
    @AfterEach
    fun resetTestId() {
        _testId = null
        subIdCounter.set(0)
    }

    private fun consoleLine(message: ConsoleMessage) =
        " ${message.type().uppercase().padEnd(8)} ${message.text()}"
}

/**
 * JUnit 5 extension that closes browser contexts and writes traces after each test.
 *
 * Only closes contexts for applications where [Application.testRunning] is `true`.
 * Traces are written to disk for failed tests or all tests when `SAVE_ALL_TRACES` is set.
 *
 * Both callbacks are implemented because they cover different failures. [afterTestExecution] runs only
 * when the test method actually executed, so a test whose set-up threw - a `@BeforeEach`, or a lifecycle
 * callback - never reaches it, and would otherwise leave no trace and an unclosed context. [afterEach]
 * runs in that case too, and writes a trace for anything still running when it does.
 *
 * A failure before the test body is the one a log can least explain on its own, so it is the one that
 * most needs its trace. Note the remaining gap: an exception thrown while *constructing* the test
 * instance leaves no instance for JUnit to register instance extensions on, so nothing here can run.
 * Set-up that can fail belongs in `@BeforeEach`, not in a constructor or field initialiser.
 */
class AppShutdown(private val apps: Map<String, Application<*>>): AfterTestExecutionCallback, AfterEachCallback {
    /**
     * Called by JUnit 5 after each test method execution.
     *
     * Stops tracing, writes trace files for applicable tests, and closes all browser contexts.
     */
    override fun afterTestExecution(context: ExtensionContext) =
        shutDown(context, saveAllTraces || context.executionException.isPresent)

    /**
     * Called by JUnit 5 after each test, including tests whose method never ran because set-up failed.
     *
     * Anything still running at this point belongs to such a test - a completed one was already stopped by
     * [afterTestExecution] - so its trace is always worth keeping.
     */
    override fun afterEach(context: ExtensionContext) = shutDown(context, writeTraces = true)

    private fun shutDown(context: ExtensionContext, writeTraces: Boolean) {
        try {
            val runningApps = apps.filter { it.value.testRunning }
            if(writeTraces) {
                runningApps.forEach { (id, app) ->
                    val suffix = when (runningApps.size) {
                        1 -> ""
                        else -> "-$id"
                    }
                    app.stopTest(
                        context.testClass.map { it.packageName }.orElse("UNKNOWN"),
                        context.testClass.map { it.simpleName }.orElse("UNKNOWN"),
                        context.testMethod.map { "${it.name}$suffix" }.orElse("UNKNOWN$suffix"),
                    )
                }
            } else {
                runningApps.forEach { (_, app) -> app.stopTest() }
            }
        } finally {
            // Close all registered applications (releasing per-test contexts and tracing), including any whose test
            // never started, even if writing a trace above failed.
            apps.values.forEach { app ->
                runCatching { app.close() }.onFailure { println("Failed to close application: $it") }
            }
        }
    }
}