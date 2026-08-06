package au.concepta.playwright.swaglabs

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.AriaRole
import au.concepta.playwright.ApplicationPage
import au.concepta.playwright.util.*

class LoginPage(
    page: Page,
    pageElement: Locator
) : ApplicationPage<LoginPage>(page, pageElement) {

    fun loginAs(username: String, password: String = "secret_sauce"): InventoryPage {
        page.getByRole(AriaRole.TEXTBOX, name = "Username").fill(username)
        page.getByRole(AriaRole.TEXTBOX, name = "Password").fill(password)
        page.getByRole(AriaRole.BUTTON, name = "Login").click()
        page.waitForLoadState()
        return InventoryPage(page, page.getByText("Products", exact = true))
    }

    fun loginWithInvalidCredentials(): LoginPage {
        page.getByRole(AriaRole.TEXTBOX, name = "Username").fill("invalid_user")
        page.getByRole(AriaRole.TEXTBOX, name = "Password").fill("wrong_password")
        page.getByRole(AriaRole.BUTTON, name = "Login").click()
        page.waitForLoadState()
        return downcast()
    }

    fun loginAsWithError(username: String, password: String = "secret_sauce"): LoginPage {
        page.getByRole(AriaRole.TEXTBOX, name = "Username").fill(username)
        page.getByRole(AriaRole.TEXTBOX, name = "Password").fill(password)
        page.getByRole(AriaRole.BUTTON, name = "Login").click()
        page.waitForLoadState()
        return downcast()
    }

    fun getErrorMessage(): String? = page.locator("[data-test='error']").textContent()
}
