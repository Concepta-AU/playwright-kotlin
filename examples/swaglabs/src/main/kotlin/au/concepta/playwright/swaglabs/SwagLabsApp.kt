package au.concepta.playwright.swaglabs

import au.concepta.playwright.Application
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.AriaRole
import au.concepta.playwright.util.*

class SwagLabsApp : Application<LoginPage>() {
    override val defaultBaseUrl: String = "https://www.saucedemo.com/"

    override fun configureNewPage(page: Page) {
        expectError("Failed to load resource: the server responded with a status of 401 (Unauthorized)")
        expectError { it.contains("submit.backtrace.io") }
        expectError("Failed to load resource: net::ERR_FAILED")
    }

    override fun getInitialApplicationPage(page: Page): LoginPage =
        LoginPage(page, page.getByRole(AriaRole.TEXTBOX, name = "Username"))

    fun loginIfNeeded(username: String = "standard_user"): InventoryPage =
        start().loginAs(username)

    fun inventory(username: String = "standard_user"): InventoryPage =
        loginIfNeeded(username)

    fun cart(username: String = "standard_user"): CartPage =
        loginIfNeeded(username).goToCart()
}
