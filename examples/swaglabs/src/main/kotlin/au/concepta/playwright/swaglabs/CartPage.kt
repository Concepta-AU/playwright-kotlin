package au.concepta.playwright.swaglabs

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import au.concepta.playwright.ApplicationPage
import au.concepta.playwright.util.*

class CartPage(
    page: Page,
    pageElement: Locator
) : ApplicationPage<CartPage>(page, pageElement) {

    data class CartItem(val name: String, val price: String)

    fun getCartItems(): List<CartItem> {
        val itemLocators = page.locator("[data-test='inventory-item']")
        val count = itemLocators.count()
        val items = mutableListOf<CartItem>()
        for (i in 0 until count) {
            val item = itemLocators.nth(i)
            val name = item.locator("[data-test='inventory-item-name']").textContent()?.trim() ?: ""
            val price = item.locator("[data-test='inventory-item-price']").textContent()?.trim() ?: ""
            items.add(CartItem(name, price))
        }
        return items
    }

    fun continueToCheckout(): CheckoutInfoPage {
        page.locator("[data-test='checkout']").click()
        return CheckoutInfoPage(page, page.locator("[data-test='title']"))
    }

    fun isEmpty(): Boolean = page.locator("[data-test='inventory-item']").count() == 0
}
