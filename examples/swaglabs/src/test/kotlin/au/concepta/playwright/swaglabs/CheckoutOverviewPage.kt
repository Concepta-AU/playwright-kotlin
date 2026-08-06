package au.concepta.playwright.swaglabs

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.AriaRole
import au.concepta.playwright.ApplicationPage
import au.concepta.playwright.util.*

class CheckoutOverviewPage(
    page: Page,
    pageElement: Locator
) : ApplicationPage<CheckoutOverviewPage>(page, pageElement) {

    data class OverviewItem(val name: String, val price: String)

    fun getOverviewItems(): List<OverviewItem> {
        val itemLocators = page.locator("[data-test='inventory-item']")
        val count = itemLocators.count()
        val items = mutableListOf<OverviewItem>()
        for (i in 0 until count) {
            val item = itemLocators.nth(i)
            val name = item.locator("[data-test='inventory-item-name']").textContent()?.trim() ?: ""
            val price = item.locator("[data-test='inventory-item-price']").textContent()?.trim() ?: ""
            items.add(OverviewItem(name, price))
        }
        return items
    }

    fun getTotal(): String {
        return page.locator("[data-test='subtotal-label']").textContent()?.trim()?.removePrefix("Item total: ") ?: ""
    }

    fun finish(): CheckoutCompletePage {
        page.locator("[data-test='finish']").click()
        return CheckoutCompletePage(page, page.locator("[data-test='title']"))
    }

    fun backToProducts(): InventoryPage {
        page.locator("[data-test='cancel']").click()
        return InventoryPage(page, page.locator("[data-test='title']"))
    }
}
