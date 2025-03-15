@file:Suppress("unused")

package au.concepta.playwright

import com.microsoft.playwright.ConsoleMessage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.RegisterExtension

val saveAllTraces: Boolean = "true" == System.getenv("SAVE_ALL_TRACES")

open class TestBase {
    private val apps: MutableMap<String, Application<*>> = mutableMapOf()

    @RegisterExtension
    val appShutdown = AppShutdown(apps)

    @AfterEach
    fun checkConsoleLogs() {
        apps.forEach { (id, app) ->
            println("==== Browser Console Log for $id ====")
            app.consoleMessages.forEach {
                println(consoleLine(it))
            }
            println()
        }
    }

    private fun consoleLine(message: ConsoleMessage) = " ${message.type().uppercase().padEnd(8)} ${message.text()}"
}

class AppShutdown(private val apps: Map<String, Application<*>>): AfterTestExecutionCallback {
    override fun afterTestExecution(context: ExtensionContext) {
        if(saveAllTraces || context.executionException.isPresent) {
            apps.forEach { (id, app) ->
                val suffix = when (apps.size) {
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
            apps.forEach { (_, app) -> app.stopTest() }
        }
    }
}