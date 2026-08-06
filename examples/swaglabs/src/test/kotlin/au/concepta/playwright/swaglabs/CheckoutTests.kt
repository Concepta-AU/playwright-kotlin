package au.concepta.playwright.swaglabs

import au.concepta.playwright.TestBase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import au.concepta.playwright.util.*

class CheckoutTests : TestBase<SwagLabsApp>() {
    init {
        registerApplication(SwagLabsApp())
    }

    private val app = getApplication()

    @Test
    fun `can complete checkout with single item`() {
        app.start()
            .loginAs("standard_user")
            .run {
                resetApp()
                addProductToCartByIndex(0)
                val overview = openCheckout()
                    .fillInfo("John", "Doe", "12345")
                val finish = overview.finish()
                assertTrue(finish.page.locator("[data-test='title']").isVisible())
            }
    }

    @Test
    fun `can complete checkout with multiple items`() {
        app.start()
            .loginAs("standard_user")
            .run {
                resetApp()
                addProductToCartByIndex(0)
                addProductToCartByIndex(2)
                addProductToCartByIndex(5)
                val overview = openCheckout()
                    .fillInfo("Jane", "Smith", "90210")
                val finish = overview.finish()
                assertTrue(finish.page.locator("[data-test='title']").isVisible())
            }
    }

    @Test
    fun `checkout overview shows correct items`() {
        app.start()
            .loginAs("standard_user")
            .run {
                resetApp()
                addProductToCartByIndex(1)
                addProductToCartByIndex(3)
                val overview = openCheckout()
                    .fillInfo("Alice", "Johnson", "54321")
                val items = overview.getOverviewItems()
                assertEquals(2, items.size)
            }
    }

    @Test
    fun `can go back from checkout overview to products`() {
        app.start()
            .loginAs("standard_user")
            .run {
                resetApp()
                addProductToCartByIndex(0)
                val overview = openCheckout()
                    .fillInfo("Bob", "Brown", "11111")
                val inventory = overview.backToProducts()
                inventory.page.waitForLoadState()
            }
    }

    @Test
    fun `can checkout from cart page`() {
        app.start()
            .loginAs("standard_user")
            .run {
                resetApp()
                addProductToCartByIndex(2)
                val cart = openCart()
                assertEquals(1, cart.getCartItems().size)
                val overview = cart.continueToCheckout()
                    .fillInfo("Carol", "White", "67890")
                overview.finish()
            }
    }

    @Test
    fun `cart total matches overview total`() {
        app.start()
            .loginAs("standard_user")
            .run {
                resetApp()
                addProductToCartByIndex(0)
                addProductToCartByIndex(4)
                val cart = openCart()
                val cartTotal = cart.getCartItems().sumOf { it.price.replace("[^0-9.]".toRegex(), "").toDouble() }
                val overview = cart.continueToCheckout()
                    .fillInfo("Dave", "Wilson", "33333")
                val overviewTotal = overview.getTotal().replace("[^0-9.]".toRegex(), "").toDouble()
                assertEquals(String.format("%.2f", cartTotal), String.format("%.2f", overviewTotal))
            }
    }
}
