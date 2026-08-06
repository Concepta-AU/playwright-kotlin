package au.concepta.playwright.swaglabs

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.AriaRole
import au.concepta.playwright.ApplicationPage
import au.concepta.playwright.util.*

class InventoryPage(
    page: Page,
    pageElement: Locator
) : ApplicationPage<InventoryPage>(page, pageElement) {
    companion object {
        val PRODUCTS_LOCATOR = "Products"
        private val SORT_VALUE_TO_TEXT = mapOf(
            "az" to "Name (A to Z)",
            "za" to "Name (Z to A)",
            "lohi" to "Price (low to high)",
            "hilo" to "Price (high to low)"
        )
        private val SORT_TEXT_TO_VALUE = SORT_VALUE_TO_TEXT.map { it.value to it.key }.toMap()
    }

    data class Product(val name: String, val price: String, val index: Int)

    fun getProducts(): List<Product> {
        val productLocators = page.locator("[data-test='inventory-item']")
        val count = productLocators.count()
        val products = mutableListOf<Product>()
        for (i in 0 until count) {
            val item = productLocators.nth(i)
            val name = item.locator("[data-test='inventory-item-name']").textContent()?.trim() ?: ""
            val price = item.locator("[data-test='inventory-item-price']").textContent()?.trim() ?: ""
            products.add(Product(name, price, i))
        }
        return products
    }

    fun getProductNameByIndex(index: Int): String {
        return page.locator("[data-test='inventory-item']").nth(index)
            .locator("[data-test='inventory-item-name']").textContent() ?: ""
    }

    fun getProductPriceByIndex(index: Int): String {
        return page.locator("[data-test='inventory-item']").nth(index)
            .locator("[data-test='inventory-item-price']").textContent() ?: ""
    }

    fun addProductToCartByIndex(index: Int): InventoryPage {
        page.locator("[data-test='inventory-item']").nth(index)
            .getByRole(AriaRole.BUTTON, name = "Add to cart").click()
        return downcast()
    }

    fun removeProductFromCartByIndex(index: Int): InventoryPage {
        page.locator("[data-test='inventory-item']").nth(index)
            .getByRole(AriaRole.BUTTON, name = "Remove").click()
        return downcast()
    }

    fun selectSortOrder(order: String): InventoryPage {
        val value = SORT_TEXT_TO_VALUE[order] ?: order
        page.locator("select").selectOption(value)
        return downcast()
    }

    fun getSortOrder(): String? {
        val value = page.locator("select").inputValue()
        return SORT_VALUE_TO_TEXT[value]
    }

    fun openCart(): CartPage {
        page.locator("[data-test='shopping-cart-link']").click()
        return CartPage(page, page.locator("[data-test='title']"))
    }

    fun openCheckout(): CheckoutInfoPage {
        return openCart().continueToCheckout()
    }

    fun openBurgerMenu(): MenuOverlay {
        page.getByRole(AriaRole.BUTTON, name = "Open menu").click()
        return MenuOverlay(page, page.getByRole(AriaRole.LINK, name = "Reset App State"))
    }

    fun getCartBadgeCount(): Int {
        val badge = page.locator("[data-test='shopping-cart-badge']")
        return if (badge.isVisible()) {
            val badgeText = badge.textContent() ?: "0"
            try {
                badgeText.toInt()
            } catch (e: NumberFormatException) {
                0
            }
        } else {
            0
        }
    }

    fun resetApp(): InventoryPage {
        openBurgerMenu().resetAppState()
        return downcast()
    }
}
