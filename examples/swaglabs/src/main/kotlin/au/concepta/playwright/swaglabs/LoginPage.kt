package au.concepta.playwright.swaglabs

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.AriaRole
import au.concepta.playwright.util.*
import org.junit.jupiter.api.Assertions.assertTrue

class LoginPage(
    page: Page,
    pageElement: Locator
) : SwagLabsPage<LoginPage>(page, pageElement) {

    fun loginAs(username: String, password: String = "secret_sauce"): InventoryPage {
        page.getByRole(AriaRole.TEXTBOX, name = "Username").fill(username)
        page.getByRole(AriaRole.TEXTBOX, name = "Password").fill(password)
        page.getByRole(AriaRole.BUTTON, name = "Login").click()
        page.waitForLoadState()
        return InventoryPage(page, page.getByText("Products", exact = true))
    }

    fun loginWithInvalidCredentials(): LoginPage {
        return loginAsWithError("invalid_user", "wrong_password")
    }

    fun loginAsWithError(username: String, password: String = "secret_sauce"): LoginPage {
        page.getByRole(AriaRole.TEXTBOX, name = "Username").fill(username)
        page.getByRole(AriaRole.TEXTBOX, name = "Password").fill(password)
        page.getByRole(AriaRole.BUTTON, name = "Login").click()
        page.waitForLoadState()
        return downcast()
    }

    fun getErrorMessage(): String? = page.locator("[data-test='error']").textContent()

    fun checkErrorMessage(block: (String?) -> Unit): LoginPage {
        block(getErrorMessage())
        return downcast()
    }

    fun assertErrorMessageContains(expectedText: String): LoginPage {
        val error = getErrorMessage()
        assertTrue(
            error?.contains(expectedText) == true,
            "Expected error message to contain '$expectedText', but was '$error'"
        )
        return downcast()
    }
}
