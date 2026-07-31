package au.concepta.playwright

import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class DummyPage(page: Page) : ApplicationPage<DummyPage>(page, page.locator("body"))

class DummyApp1 : Application<DummyPage>() {
    override val defaultBaseUrl: String = "data:text/html,<html><body>App 1</body></html>"
    override fun getInitialApplicationPage(page: Page): DummyPage = DummyPage(page)
}

class DummyApp2 : Application<DummyPage>() {
    override val defaultBaseUrl: String = "data:text/html,<html><body>App 2</body></html>"
    override fun getInitialApplicationPage(page: Page): DummyPage = DummyPage(page)

    var launchOptionsModified = false

    override fun modifyBrowserLaunchOptions(defaultOptions: BrowserType.LaunchOptions): BrowserType.LaunchOptions {
        launchOptionsModified = true
        return super.modifyBrowserLaunchOptions(defaultOptions)
    }
}

class ApplicationSharingTests {

    @Test
    fun `applications of the same class share browser instance on the same thread`() {
        val app1 = DummyApp1()
        val app2 = DummyApp1()

        val page1 = app1.start()
        assertThat(page1.page.content()).contains("App 1")

        val browser1 = SharedBrowserManager.getOrCreateBrowser(app1)

        app1.close()

        val page2 = app2.start()
        assertThat(page2.page.content()).contains("App 1")

        val browser2 = SharedBrowserManager.getOrCreateBrowser(app2)

        assertThat(browser1).isSameAs(browser2)
        assertThat(browser1.isConnected).isTrue()

        app2.close()
    }

    @Test
    fun `separate contexts are created for separate application runs`() {
        val app1 = DummyApp1()
        val page1 = app1.start()

        val context1 = page1.page.context()
        app1.close()

        val app2 = DummyApp1()
        val page2 = app2.start()
        val context2 = page2.page.context()

        assertThat(context1).isNotSameAs(context2)

        app2.close()
    }

    @Test
    fun `different application classes can customize launch options and obtain connected browsers`() {
        val app = DummyApp2()
        val page = app.start()
        assertThat(page.page.content()).contains("App 2")
        assertThat(app.launchOptionsModified).isTrue()
        app.close()
    }

    @Test
    fun `recovers gracefully if browser is disconnected`() {
        val app1 = DummyApp1()
        app1.start()
        val oldBrowser = SharedBrowserManager.getOrCreateBrowser(app1)

        // Simulate browser disconnection
        oldBrowser.close()
        assertThat(oldBrowser.isConnected).isFalse()

        val app2 = DummyApp1()
        val page2 = app2.start()
        val newBrowser = SharedBrowserManager.getOrCreateBrowser(app2)

        assertThat(newBrowser).isNotSameAs(oldBrowser)
        assertThat(newBrowser.isConnected).isTrue()
        assertThat(page2.page.content()).contains("App 1")

        app2.close()
    }
}
