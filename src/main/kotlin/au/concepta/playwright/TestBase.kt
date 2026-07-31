@file:Suppress("unused")

package au.concepta.playwright

import com.microsoft.playwright.ConsoleMessage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.RegisterExtension

val saveAllTraces: Boolean = "true" == System.getenv("SAVE_ALL_TRACES")

open class TestBase<T: Application<*>> {
    private val applications: MutableMap<String, T> = mutableMapOf()
    protected open val defaultApplicationId: String = "default"

    /**
     * Hand an application over to this base class, which closes its context after the test.
     *
     * Register an application immediately after constructing it and before any further set-up such as logging in.
     */
    fun registerApplication(application: T, id: String = defaultApplicationId) {
        applications[id] = application
    }

    fun getApplication(id: String = defaultApplicationId): T = applications[id] ?:
        throw IllegalArgumentException("No application registered with id: $id")

    @RegisterExtension
    val appShutdown = AppShutdown(applications)

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

class AppShutdown(private val apps: Map<String, Application<*>>): AfterTestExecutionCallback {
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