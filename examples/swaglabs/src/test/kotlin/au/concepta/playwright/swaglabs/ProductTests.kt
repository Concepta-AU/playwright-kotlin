package au.concepta.playwright.swaglabs

import au.concepta.playwright.TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ProductTests : TestBase<SwagLabsApp>() {
    init {
        registerApplication(SwagLabsApp())
    }

    private val app = getApplication()

    @Test
    fun `products are sorted by name A to Z by default`() {
        app.inventory()
            .assertSortOrder("Name (A to Z)")
            .checkProducts { products ->
                val names = products.map { it.name }
                assertThat(names).isSorted
            }
    }

    @Test
    fun `products can be sorted by name Z to A`() {
        app.inventory()
            .selectSortOrder("Name (Z to A)")
            .assertSortOrder("Name (Z to A)")
            .checkProducts { products ->
                val names = products.map { it.name }
                assertThat(names).isSortedAccordingTo(Comparator.reverseOrder())
            }
    }

    @Test
    fun `products can be sorted by price low to high`() {
        app.inventory()
            .selectSortOrder("Price (low to high)")
            .assertSortOrder("Price (low to high)")
            .checkProducts { products ->
                val prices = products.map { it.price.replace("[^0-9.]".toRegex(), "").toDouble() }
                assertThat(prices).isSorted
            }
    }

    @Test
    fun `products can be sorted by price high to low`() {
        app.inventory()
            .selectSortOrder("Price (high to low)")
            .assertSortOrder("Price (high to low)")
            .checkProducts { products ->
                val prices = products.map { it.price.replace("[^0-9.]".toRegex(), "").toDouble() }
                assertThat(prices).isSortedAccordingTo(Comparator.reverseOrder())
            }
    }

    @Test
    fun `can add multiple products to cart`() {
        app.inventory()
            .resetApp()
            .addProductToCartByIndex(0)
            .addProductToCartByIndex(2)
            .addProductToCartByIndex(4)
            .assertCartBadgeCount(3)
    }

    @Test
    fun `can add and remove single product from cart`() {
        app.inventory()
            .resetApp()
            .addProductToCartByIndex(1)
            .assertCartBadgeCount(1)
            .removeProductFromCartByIndex(1)
            .assertCartBadgeCount(0)
    }

    @Test
    fun `can navigate from inventory to cart`() {
        app.inventory()
            .resetApp()
            .addProductToCartByIndex(2)
            .goToCart()
            .checkCartItems { items ->
                assertThat(items).isNotEmpty
            }
    }

    @Test
    fun `can reset app state via burger menu`() {
        app.inventory()
            .addProductToCartByIndex(0)
            .addProductToCartByIndex(1)
            .assertCartBadgeCount(2)
            .resetApp()
            .assertCartBadgeCount(0)
    }

    @Test
    fun `six products are displayed on inventory page`() {
        app.inventory()
            .checkProducts { products ->
                assertThat(products).hasSize(6)
            }
    }
}
