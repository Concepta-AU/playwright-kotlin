package au.concepta.playwright

import com.microsoft.playwright.Browser
import com.microsoft.playwright.Playwright
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Manages shared [Playwright] and [Browser] instances per thread and application subclass.
 *
 * Playwright Java objects are bound to the thread that created them. This manager ensures that each
 * executing thread reuses a single driver process and browser instance per application subclass,
 * maintaining thread safety under parallel test execution while preventing browser launch overhead for every test.
 */
internal object SharedBrowserManager {
    private class SharedBrowser(
        val playwright: Playwright,
        val browser: Browser
    ) : AutoCloseable {
        override fun close() {
            runCatching { browser.close() }
            runCatching { playwright.close() }
        }
    }

    private val threadLocalBrowsers = ThreadLocal.withInitial {
        mutableMapOf<KClass<out Application<*>>, SharedBrowser>()
    }

    private val allBrowsers = ConcurrentHashMap.newKeySet<SharedBrowser>()

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            closeAll()
        })
    }

    fun getOrCreateBrowser(app: Application<*>): Browser {
        val key = app::class
        val map = threadLocalBrowsers.get()
        var shared = map[key]

        if (shared == null || !shared.browser.isConnected()) {
            if (shared != null) {
                allBrowsers.remove(shared)
                runCatching { shared.close() }
            }
            val playwright = Playwright.create()!!
            val launchOptions = app.createLaunchOptions()
            val browser = playwright.chromium().launch(launchOptions)

            shared = SharedBrowser(playwright, browser)
            map[key] = shared
            allBrowsers.add(shared)
        }

        return shared.browser
    }

    fun closeAll() {
        allBrowsers.forEach { shared ->
            runCatching { shared.close() }
        }
        allBrowsers.clear()
        threadLocalBrowsers.get()?.clear()
    }
}
