package au.concepta.playwright.swaglabs

import au.concepta.playwright.ApplicationPage
import au.concepta.playwright.util.*
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.AriaRole
import org.junit.jupiter.api.Assertions.assertEquals

open class SwagLabsPage<T : ApplicationPage<T>>(
    page: Page,
    elementToWaitFor: Locator
) : ApplicationPage<T>(page, elementToWaitFor) {

    inner class MenuOverlay(
        page: Page,
        private val resetLocator: Locator
    ) : ApplicationPage<MenuOverlay>(page, resetLocator) {

        fun resetAppState(): T {
            resetLocator.click()
            return this@SwagLabsPage.downcast()
        }
    }

    fun openBurgerMenu(): MenuOverlay {
        page.getByRole(AriaRole.BUTTON, name = "Open menu").click()
        return MenuOverlay(page, page.getByRole(AriaRole.LINK, name = "Reset App State"))
    }

    fun resetApp(): T {
        openBurgerMenu().resetAppState()
        return downcast()
    }

    fun goToCart(): CartPage {
        page.locator("[data-test='shopping-cart-link']").click()
        return CartPage(page, page.locator("[data-test='title']"))
    }

    fun openCart(): CartPage = goToCart()

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

    fun assertCartBadgeCount(expectedCount: Int): T {
        assertEquals(expectedCount, getCartBadgeCount(), "Expected cart badge count to be $expectedCount")
        return downcast()
    }
}
