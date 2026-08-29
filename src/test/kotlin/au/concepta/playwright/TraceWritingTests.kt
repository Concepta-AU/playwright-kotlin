package au.concepta.playwright

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.ExtensionContext
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional
import kotlin.io.path.deleteIfExists
import kotlin.test.AfterTest
import kotlin.test.Test

/**
 * A stand-in for the context JUnit hands an extension, carrying only what [AppShutdown] reads from it:
 * the class and method being run, and the exception that failed them. Everything else throws, so a
 * change that starts depending on more of the context fails here rather than passing on a default.
 */
private class FakeExtensionContext(
    private val testClass: Class<*>,
    private val methodName: String,
    private val failure: Throwable? = null,
) : ExtensionContext {
    override fun getTestClass(): Optional<Class<*>> = Optional.of(testClass)
    override fun getTestMethod(): Optional<Method> =
        Optional.of(FakeExtensionContext::class.java.getDeclaredMethod(methodName))
    override fun getExecutionException(): Optional<Throwable> = Optional.ofNullable(failure)

    private fun unused(): Nothing = throw UnsupportedOperationException("not read by AppShutdown")
    override fun getParent(): Optional<ExtensionContext> = unused()
    override fun getRoot(): ExtensionContext = unused()
    override fun getUniqueId(): String = unused()
    override fun getDisplayName(): String = unused()
    override fun getTags(): MutableSet<String> = unused()
    override fun getElement(): Optional<AnnotatedElement> = unused()
    override fun getTestInstance(): Optional<Any> = unused()
    override fun getConfigurationParameter(key: String?): Optional<String> = unused()
    override fun getStore(namespace: ExtensionContext.Namespace?): ExtensionContext.Store = unused()
    override fun getStore(scope: ExtensionContext.StoreScope?, namespace: ExtensionContext.Namespace?):
        ExtensionContext.Store = unused()
    override fun <T : Any?> getConfigurationParameter(
        key: String?,
        transformer: java.util.function.Function<String, T>?,
    ): Optional<T> = unused()
    override fun getEnclosingTestClasses(): MutableList<Class<*>> = unused()
    override fun getTestInstanceLifecycle(): Optional<org.junit.jupiter.api.TestInstance.Lifecycle> = unused()
    override fun getTestInstances(): Optional<org.junit.jupiter.api.extension.TestInstances> = unused()
    override fun publishReportEntry(map: MutableMap<String, String>?) = unused()
    override fun publishFile(
        name: String?,
        mediaType: org.junit.jupiter.api.extension.MediaType?,
        action: org.junit.jupiter.api.function.ThrowingConsumer<Path>?,
    ) = unused()
    override fun publishFile(
        name: String?,
        mediaType: org.junit.jupiter.api.MediaType?,
        action: org.junit.jupiter.api.function.ThrowingConsumer<Path>?,
    ) = unused()
    override fun publishDirectory(
        name: String?,
        action: org.junit.jupiter.api.function.ThrowingConsumer<Path>?,
    ) = unused()
    override fun getExecutionMode(): org.junit.jupiter.api.parallel.ExecutionMode = unused()
    override fun getExecutableInvoker(): org.junit.jupiter.api.extension.ExecutableInvoker = unused()

    /** Named so [getTestMethod] has a real [Method] to hand back; never invoked. */
    @Suppress("unused")
    fun tracedMethod() = Unit
}

class TraceWritingTests {
    private val traceRoot: Path = Path.of("traces", TraceWritingTests::class.java.packageName)

    @AfterTest
    fun removeWrittenTraces() {
        traceRoot.resolve(TraceWritingTests::class.java.simpleName).let { dir ->
            if (Files.exists(dir)) Files.walk(dir).sorted(Comparator.reverseOrder()).forEach { it.deleteIfExists() }
        }
    }

    private fun traceFor(methodName: String): Path =
        traceRoot.resolve(TraceWritingTests::class.java.simpleName).resolve("$methodName.zip")

    private fun context(methodName: String, failure: Throwable? = null) =
        FakeExtensionContext(TraceWritingTests::class.java, methodName, failure)

    @Test
    fun `a trace is written for a test whose method never ran`() {
        val base = TestBase<DummyApp1>()
        val app = DummyApp1()
        base.registerApplication(app)
        app.start()

        // afterEach, and not afterTestExecution: JUnit skips the latter when set-up threw, which is
        // exactly the case this covers.
        base.appShutdown.afterEach(context("tracedMethod"))

        assertThat(traceFor("tracedMethod")).exists()
    }

    @Test
    fun `a trace is written for a test that failed in its body`() {
        val base = TestBase<DummyApp1>()
        val app = DummyApp1()
        base.registerApplication(app)
        app.start()

        base.appShutdown.afterTestExecution(context("tracedMethod", AssertionError("failed")))

        assertThat(traceFor("tracedMethod")).exists()
    }

    @Test
    fun `no trace is written for a test that passed`() {
        val base = TestBase<DummyApp1>()
        val app = DummyApp1()
        base.registerApplication(app)
        app.start()

        base.appShutdown.afterTestExecution(context("tracedMethod"))
        // The later callback must not resurrect a trace for a test that already finished cleanly.
        base.appShutdown.afterEach(context("tracedMethod"))

        assertThat(traceFor("tracedMethod")).doesNotExist()
    }
}
