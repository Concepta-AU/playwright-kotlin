package au.concepta.playwright.swaglabs

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import au.concepta.playwright.util.*
import org.junit.jupiter.api.Assertions.assertTrue

class CartPage(
    page: Page,
    pageElement: Locator
) : SwagLabsPage<CartPage>(page, pageElement) {

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

    fun checkCartItems(block: (List<CartItem>) -> Unit): CartPage {
        block(getCartItems())
        return downcast()
    }

    fun startCheckout(): CheckoutInfoPage {
        page.locator("[data-test='checkout']").click()
        return CheckoutInfoPage(page, page.locator("[data-test='title']"))
    }

    fun continueToCheckout(): CheckoutInfoPage = startCheckout()

    fun isEmpty(): Boolean = page.locator("[data-test='inventory-item']").count() == 0

    fun assertEmpty(): CartPage {
        assertTrue(isEmpty(), "Expected cart to be empty")
        return downcast()
    }
}
