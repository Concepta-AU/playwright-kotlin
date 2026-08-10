Getting Started
===============

Prerequisites
-------------

- JDK 21+
- Gradle (or your build tool of choice)
- Playwright browsers installed (`playwright install`)

Add the dependency
------------------

Find the latest version on [Maven Central](https://central.sonatype.com/artifact/au.concepta/playwright-kotlin).

**Gradle (Kotlin DSL):**

```kotlin
dependencies {
    testImplementation("au.concepta:playwright-kotlin:<version>")
}
```

**Maven:**

```xml
<dependency>
    <groupId>au.concepta</groupId>
    <artifactId>playwright-kotlin</artifactId>
    <version>...</version>
    <scope>test</scope>
</dependency>
```

Define an Application
---------------------

Subclass `Application` to represent the application under test. Set `defaultBaseUrl` and return your initial page from `getInitialApplicationPage()`:

```kotlin
class MyApp : Application<MyAppPage>() {
    override val defaultBaseUrl: String = "https://example.com"

    override fun getInitialApplicationPage(page: Page): MyAppPage = MyAppPage(page, page.getByRole(AriaRole.HEADING, name = "Welcome"))
}
```

Optionally override `modifyBrowserLaunchOptions()` or `modifyBrowserContext()` to customize the browser configuration,
typically the defaults should be good enough to start with.


Define an ApplicationPage
-------------------------

Subclass `ApplicationPage<T>` to represent a single page. Pass a `Locator` that identifies when the page is ready - this
locator will be waited for automatically when the page object is created. Always use the page class itself as the generic
type for the base class.

```kotlin
class MyAppPage(page: Page) : ApplicationPage<MyAppPage>(
    page, page.locator(".. something that appears when the page is ready ...")
) {

    fun login(email: String, password: String): DashboardPage {
        page.getByLabel("Email").fill(email)
        page.getByLabel("Password").fill(password)
        page.getByRole(AriaRole.BUTTON, name = "Sign in").click()
        return DashboardPage(page, page.getByRole(AriaRole.HEADING, name = "Dashboard"))
    }

    fun getHeading(): String = page.getByRole(AriaRole.HEADING).textContent() ?: ""
}
```

A core pattern recommended here is to use a fluent API style for all navigation and other interaction, where the return
value of a method is the expected page the browser should be on after. This not only allows for a concise style in the
tests. See [recommended patterns](recommended-patterns.md) for more detail.

Optionally override `configureNewPage(Page)` if your application needs to do something on every page, like configuring
known JavaScript errors to ignore. Typically, that should not be needed but can help in some situations.

Write a Test
------------

Extend `TestBase` to manage the application lifecycle automatically:

```kotlin
class LoginTest : TestBase<MyApp>() {

    private val app = getApplication()

    @Test
    fun `user can log in`() {
        app.start()
            .login("user@example.com", "secret")
    }
}
```

Note that while there is no explicit assertion in the test, the fact that the login page returns a `DashboardPage`
implies that the page level locator of the dashboard page was found. As long as these are defined correctly, the tests
do no longer need to constantly assert elements just to confirm a page opened.


Use Extension Functions
-----------------------

The library provides Kotlin-friendly extension functions that replace the need to explicitly create Playwright options 
objects. Named parameters with defaults make the API concise:

```kotlin
// normal: page.getByText("Welcome", Page.GetByTextOptions().setExact(true))
page.getByText("Welcome", exact = true)

// normal: page.click("button.submit", Page.ClickOptions().setButton(MouseButton.LEFT).setModifiers(listOf(KeyboardModifier.ALT)))
page.click("button.submit", button = MouseButton.LEFT, modifiers = listOf(KeyboardModifier.ALT))
```

All methods on `Page`, `Frame`, and `Locator` that use parameter objects have matching convenience methods for this.


Waiting / Retries
-----------------

The `Waiting.kt` file has a number of methods to allow for higher level waiting or retries using lambdas such as this:

```kotlin
        retry {
            val actual = // fetch some data from the page
            assertThat(actual).containsExactly(expected)
        }
```

Similarly `waitUntil` can wait for a condition to hold, and `waitFor` can repeatedly try and retrieve data from a page.
All of these have additional options to control the number of attempts, the delay between attempts, and other details.


Assertions
----------

The recommended way of asserting something is to either use Playwright's inbuild assertions, or any library that throws
assertion errors that jUnit understands. In the examples we use [AssertJ](https://github.com/assertj/assertj), but other
choices will work as well.
