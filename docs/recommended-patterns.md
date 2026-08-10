Recommended Patterns
====================

Below is a number of patterns that are recommended to be used with this library. None of these are necessary to make use
of the library, but they work well in combination and have been used successfully in other projects.

Make sure to read [the Getting Started Document](getting-started.md) first.


Use an Application-Wide Page Base Class
---------------------------------------

To model the global aspects of your application, having a subclass of `ApplicationPage` is helpful. Global aspects can
include a main navigation, handling toasts or other feedback mechanisms, loading states, etc.

Declare such a base class like this:

```kotlin
open class MyApplicationPage<T : KonceptaPage<T>>(page: Page, elementToWaitFor: Locator) :
    ApplicationPage<T>(page, elementToWaitFor) {
    // global content here
}
```

Then add access methods for the main navigation there:

```kotlin
   private val navBar = page.locator("my nav bar locator")

fun goToExports(): ExportsPage {
    navBar.getByRole(AriaRole.LINK, name = "Exports", exact = true).click()
    return ExportsPage(page)
}
```

Split Code for Test API from Tests
----------------------------------

Consider the page objects and utility methods an API for testing and put that into the `src/main` structure of your 
build. The `src/test` can then contain only the tests. This makes it easier to review tests on their own, and it allows
having a separation of ownership: `src/main` may be owned by the development team, `src/test` by the QA team.

Use a Fluent API Style
----------------------

By chaining methods through page objects, tests can become quite legible to a non-technical user. Unlike
Cucumber/Gherkin this is not intended to be written by a non-technical team member, but having a nice fluent API can
allow for a broader review.

This is an example from a project that uses the library:

```kotlin
    @Test
fun `validate key generation`() {
    app.specifications()
        .startNewSpecification()
        .setTitle("A")
        .assertKey("A")
        .setTitle("Longerthansix")
        .assertKey("LONGER")
        .setTitle("Multiple Words")
        .assertKey("MULWOR")
        .setTitle("More than two words here")
        .assertKey("MTTWH")
        .setKey("MANUAL")
        .setTitle("This should not change key")
        .assertKey("MANUAL")
}
```

The fluent API style with dedicated page objects for each page also means that any good IDE should provide support in
choosing the next step, as the available methods should match the expected state the application should be in. In the
example, the `setTitle` and `assertKey` methods are available only since the page object at that point is the one
representing the new specification page.

The way this is set up is that the `SpecificationsPage` representing the list of all specifications has this method:

```kotlin
    fun startNewSpecification(): SpecificationEditPage {
    page.locator("#newSpecificationLink").click()
    return SpecificationEditPage(page)
}
```

The `SpecificationEditPage` validates the new page opened via the page locator (passed into the constructor of
`ApplicationPage`). That implicitly waits for the page to load. The `SpecificationEditPage` then offers the methods that
interact with that page, plus the generic ones from the shared superclass.


Use Application Methods for the Main Navigation
-----------------------------------------------

An application's main navigation often creates a separate context for each item. To establish that context in a test,
having multiple entry points on the `Application` subclass you are using can be useful - like the `app.specifications()`
in the example above.

The code way this is set up in the codebase from the example looks like this:

```kotlin
    fun specifications(): SpecificationsPage = loginIfNeeded().goToSpecifications()
    fun externalResources(): ExternalResourcesPage = loginIfNeeded().goToExternalResources()
    fun exports(): ExportsPage = loginIfNeeded().goToExports()
    fun settings(): SettingsPage = loginIfNeeded().goToSettings()
```

This is on the subclass of `Application` that is used. Each of the methods represents one main menu entry. The
`loginIfNeeded()` will check if we are on the login page and go through the login process if we are. Then the matching
navigation link is clicked.


Use Consistent Method Names
---------------------------

Make sure to establish a good naming pattern for the methods in the fluent API, so that it becomes clear what each step
does. These patterns are the recommended style:

| Pattern                    | Purpose                                                                                                                                                                                                                                                                                                                                                  | Example                                                     |
|----------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------|
| `goToXxx()`                | Direct navigation to another page.                                                                                                                                                                                                                                                                                                                       | `goToSettings()`                                            |
| `openXxx()`                | Follow a link to a specific entity currently shown on page.                                                                                                                                                                                                                                                                                              | `openCustomer(name = "John Doe")`                           |
| `selectXxx()`              | Select an option visible on the page - can be a tab, a filter, or a menu option. The full name may contain the type selected.                                                                                                                                                                                                                            | `selectMetadataTab()`                                       |
| `startXxx()`               | Start an interaction, e.g. something opening up a modal - for a modal, the flow would likely end in a `submit()` returning to the original page. Combine with an action verb, such as `startCreateXxx()`, `startAddXxx()`.                                                                                                                               | `startAddCustomer()`                                        |
| `createXxx()`              | Process a full creation step of a business entity, often build from a `startNewXxx()`, followed by multiple `setYyy()` and a `submit()`. Convenience method to not do all steps explicitly in every test. Consider defaulting parameters empty or with fake data. Should always be a complete change, including all confirmation and other steps needed. | `createUser(name = "John Doe", email = "john@example.com")` |       
| `addXxx()` / `removeXxx()` | Similar to `createXxx()`, but for adding/removing entities on the page. Parameters defaulted to `null` can allow adding multiple filter options. The `create`/`add` distinction is mostly around which entities are considered primary: you may create a document, but add some tags to it.                                                              | `removeUser(name = "John Doe")`                             |
| `setXxx()`                 | Sets a particular value on a form.                                                                                                                                                                                                                                                                                                                       | `setName("John Doe")`                                       |
| `waitForXxx()`             | Waits for a state to be reached (typically using the methods from `Waiting.kt`).                                                                                                                                                                                                                                                                         | `waitForToast(title = "User added")`                        |
| `assertXxx()`              | Asserts a single fact about the page, e.g. a field having a particular value.                                                                                                                                                                                                                                                                            | `assertName("John Doe")`, `assertSubmitDisabled()`          |
| `checkXxx()`               | Reads some data of the page, then passes this as a data object into a lambda. The lambda can then run normal assertions on the data received (see below).                                                                                                                                                                                                | `checkCustomers { customers -> /* run assertions */ }`      | 

Use Data Classes to Capture Page Content
----------------------------------------

For single entities shown on a page, individually asserting fields can be enough. But in more complex situations such as
data tables or repeated elements, having a way to extract the data into objects allows checking the results more easily.

To allow for this, create a data class representing the business entity shown like this:

```kotlin
data class Customer(val name: String, val email: String, val lastLogin: LocalDateTime)
```

Then on a page showing a list of customers, use a method like this:

```kotlin
fun checkCustomers(block: (List<Customer>) -> Unit): CustomersPage {
    val customers = parseCustomers()
    block(customers)
    return this
}
```

This can then be used like this:

```kotlin
@Test
fun `some test checking customers`() {
    app.customers()
        .validateCustomers { customers ->
            assertThat(customers).anyMatch { it.name == "Target Value" }
        }
    // continue with more page methods here if wanted
}
```

Data classes can also be used to manage form entry, which is especially useful when combining it with faked default
values:

```kotlin
data class User(
    val name: String = faker.name(),
    val email: String = faker.email(),
    val phone: String = faker.mobilePhoneNumber(),
)
```

This can then be passed into methods that fill out forms but also track the expectations at a later point of a test. By
having all values faked as a default, a test can just create a new `User()`, or decide to control any of the fields
explicitly. This is not only convenient to write but also makes it clear what a test cares about. For example, if a test
creates a `User(email = "something@specific")`, then this test is likely to test something email-related.


Use Inner Classes for Modals
----------------------------

Since a modal dialog or other modal state will completely replace the available user interactions, it should be modeled
as a separate page object, with any method that causes the modal to close returning the original page object.

If that modal is used only on one page, then it should be written as an inner class of the relevant page object.


Make Sure Test Data is Distinct
-------------------------------

Running full E2E test suites can take significant time, which can be reduced by running multiple tests in parallel
against the same system under test. That requires the tests to be written in a way that they do not interfere. Part of
this is to generate unique IDs and keys in each test, and also to make sure anything that is used to filter data sets is
using such an identifier.

For example, do not create a customer "John Doe", then search for that name in the list of customers as this risks
interactions between tests. Create the customer as `John Doe ${testId}`, where the `testId` is a unique identifier
generated in the individual test.


Use Utility Methods for Component Access
----------------------------------------

When working with component libraries, it is useful to have one or more files that collect extension methods to the
`Page` and `Locator` objects that make it easier to interact with the components.

For example, if you are using PrimeNG, you may want to have a file `PrimeNgUtil.kt` that has extension methods like this
one:

```kotlin
/**
 * Selects a value from a PrimeNG auto-complete component.
 */
fun Locator.selectFromAutoComplete(searchValue: String, shownValue: String = searchValue) {
    locator(".p-autocomplete-input").fill(searchValue)
    locator(".p-autocomplete-option").first().waitFor()
    val item = locator(".p-autocomplete-option").all().find { it.textContent().contains(shownValue) }
        ?: throw AssertionFailedError("Could not find autocomplete option for $shownValue")
    item.click()
}
```

These files can collect any logic that is specific to your component library, but not the individual pages.
