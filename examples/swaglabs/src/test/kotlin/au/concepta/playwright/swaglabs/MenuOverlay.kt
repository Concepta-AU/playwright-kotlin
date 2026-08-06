package au.concepta.playwright.swaglabs

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.AriaRole
import au.concepta.playwright.ApplicationPage
import au.concepta.playwright.util.*

class MenuOverlay(
    page: Page,
    private val resetLocator: Locator
) : ApplicationPage<MenuOverlay>(page, resetLocator) {

    fun resetAppState(): InventoryPage {
        resetLocator.click()
        return InventoryPage(page, page.getByText("Products", exact = true))
    }
}
