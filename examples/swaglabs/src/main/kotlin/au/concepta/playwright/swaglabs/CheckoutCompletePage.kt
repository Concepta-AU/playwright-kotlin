package au.concepta.playwright.swaglabs

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import au.concepta.playwright.util.*
import org.junit.jupiter.api.Assertions.assertTrue

class CheckoutCompletePage(
    page: Page,
    pageElement: Locator
) : SwagLabsPage<CheckoutCompletePage>(page, pageElement) {

    fun goToProducts(): InventoryPage {
        page.locator("[data-test='back-to-products']").click()
        return InventoryPage(page, page.locator("[data-test='title']"))
    }

    fun backToProducts(): InventoryPage = goToProducts()

    fun assertOrderComplete(): CheckoutCompletePage {
        assertTrue(
            page.locator("[data-test='title']").isVisible(),
            "Expected checkout complete title to be visible"
        )
        return downcast()
    }
}
