@file:Suppress("unused")

package au.concepta.playwright

import com.microsoft.playwright.ConsoleMessage
import org.junit.jupiter.api.AfterEach
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
 * Base class for test fixtures that manage application instances and browser lifecycle.
 *
 * Subclasses register their application instances via [registerApplication]. A [TestBase]
 * instance is typically injected into tests via JUnit 5's `@RegisterExtension`, which ensures
 * browser contexts are closed and traces are written after each test.
 */
open class TestBase<T: Application<*>> {
    private val applications: MutableMap<String, T> = mutableMapOf()

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
        }
    }

    private fun consoleLine(message: ConsoleMessage) =
        " ${message.type().uppercase().padEnd(8)} ${message.text()}"
}

/**
 * JUnit 5 extension that closes browser contexts and writes traces after each test execution.
 *
 * Only closes contexts for applications where [Application.testRunning] is `true`.
 * Traces are written to disk for failed tests or all tests when `SAVE_ALL_TRACES` is set.
 */
class AppShutdown(private val apps: Map<String, Application<*>>): AfterTestExecutionCallback {
    /**
     * Called by JUnit 5 after each test method execution.
     *
     * Stops tracing, writes trace files for applicable tests, and closes all browser contexts.
     */
    override fun afterTestExecution(context: ExtensionContext) {
        try {
            val runningApps = apps.filter { it.value.testRunning }
            if(saveAllTraces || context.executionException.isPresent) {
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