package au.concepta.playwright.swaglabs

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import au.concepta.playwright.ApplicationPage
import au.concepta.playwright.util.*

class CheckoutCompletePage(
    page: Page,
    pageElement: Locator
) : ApplicationPage<CheckoutCompletePage>(page, pageElement) {

    fun backToProducts(): InventoryPage {
        page.locator("[data-test='back-to-products']").click()
        return InventoryPage(page, page.locator("[data-test='title']"))
    }
}
