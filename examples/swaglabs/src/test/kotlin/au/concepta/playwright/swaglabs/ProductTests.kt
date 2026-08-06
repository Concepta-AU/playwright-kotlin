package au.concepta.playwright.swaglabs

import au.concepta.playwright.TestBase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import au.concepta.playwright.util.*

class ProductTests : TestBase<SwagLabsApp>() {
    init {
        registerApplication(SwagLabsApp())
    }

    private val app = getApplication()

    @Test
    fun `products are sorted by name A to Z by default`() {
        app.start()
            .loginAs("standard_user")
            .run {
                val products = getProducts()
                val names = products.map { it.name }
                assertEquals(names.sorted(), names, "Products should be sorted by name A to Z")
                assertEquals("Name (A to Z)", getSortOrder())
            }
    }

    @Test
    fun `products can be sorted by name Z to A`() {
        app.start()
            .loginAs("standard_user")
            .run {
                selectSortOrder("Name (Z to A)")
                val products = getProducts()
                val names = products.map { it.name }
                assertEquals(names.sorted().reversed(), names, "Products should be sorted by name Z to A")
                assertEquals("Name (Z to A)", getSortOrder())
            }
    }

    @Test
    fun `products can be sorted by price low to high`() {
        app.start()
            .loginAs("standard_user")
            .run {
                selectSortOrder("Price (low to high)")
                val products = getProducts()
                val prices = products.map { it.price.replace("[^0-9.]".toRegex(), "").toDouble() }
                assertEquals(prices.sorted(), prices, "Products should be sorted by price low to high")
                assertEquals("Price (low to high)", getSortOrder())
            }
    }

    @Test
    fun `products can be sorted by price high to low`() {
        app.start()
            .loginAs("standard_user")
            .run {
                selectSortOrder("Price (high to low)")
                val products = getProducts()
                val prices = products.map { it.price.replace("[^0-9.]".toRegex(), "").toDouble() }
                assertEquals(prices.sorted().reversed(), prices, "Products should be sorted by price high to low")
                assertEquals("Price (high to low)", getSortOrder())
            }
    }

    @Test
    fun `can add multiple products to cart`() {
        app.start()
            .loginAs("standard_user")
            .run {
                resetApp()
                addProductToCartByIndex(0)
                addProductToCartByIndex(2)
                addProductToCartByIndex(4)
                assertEquals(3, getCartBadgeCount())
            }
    }

    @Test
    fun `can add and remove single product from cart`() {
        app.start()
            .loginAs("standard_user")
            .run {
                resetApp()
                addProductToCartByIndex(1)
                assertEquals(1, getCartBadgeCount())
                removeProductFromCartByIndex(1)
                assertEquals(0, getCartBadgeCount())
            }
    }

    @Test
    fun `can navigate from inventory to cart`() {
        app.start()
            .loginAs("standard_user")
            .run {
                resetApp()
                addProductToCartByIndex(2)
                val cart = openCart()
                assertTrue(cart.getCartItems().isNotEmpty())
            }
    }

    @Test
    fun `can reset app state via burger menu`() {
        app.start()
            .loginAs("standard_user")
            .run {
                addProductToCartByIndex(0)
                addProductToCartByIndex(1)
                assertEquals(2, getCartBadgeCount())
                resetApp()
                assertEquals(0, getCartBadgeCount())
            }
    }

    @Test
    fun `six products are displayed on inventory page`() {
        app.start()
            .loginAs("standard_user")
            .run {
                val products = getProducts()
                assertEquals(6, products.size)
            }
    }
}
