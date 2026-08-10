package au.concepta.playwright.swaglabs

import au.concepta.playwright.TestBase
import au.concepta.playwright.swaglabs.CheckoutInfoPage.CheckoutInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CheckoutTests : TestBase<SwagLabsApp>() {
    init {
        registerApplication(SwagLabsApp())
    }

    private val app = getApplication()

    @Test
    fun `can complete checkout with single item`() {
        app.inventory()
            .resetApp()
            .addProductToCartByIndex(0)
            .startCheckout()
            .setCheckoutInfo(CheckoutInfo(firstName = "John", lastName = "Doe", zipCode = "12345"))
            .finishCheckout()
            .assertOrderComplete()
    }

    @Test
    fun `can complete checkout with multiple items`() {
        app.inventory()
            .resetApp()
            .addProductToCartByIndex(0)
            .addProductToCartByIndex(2)
            .addProductToCartByIndex(5)
            .startCheckout()
            .setCheckoutInfo(CheckoutInfo(firstName = "Jane", lastName = "Smith", zipCode = "90210"))
            .finishCheckout()
            .assertOrderComplete()
    }

    @Test
    fun `checkout overview shows correct items`() {
        app.inventory()
            .resetApp()
            .addProductToCartByIndex(1)
            .addProductToCartByIndex(3)
            .startCheckout()
            .setCheckoutInfo(CheckoutInfo(firstName = "Alice", lastName = "Johnson", zipCode = "54321"))
            .checkOverviewItems { items ->
                assertThat(items).hasSize(2)
            }
    }

    @Test
    fun `can go back from checkout overview to products`() {
        app.inventory()
            .resetApp()
            .addProductToCartByIndex(0)
            .startCheckout()
            .setCheckoutInfo(CheckoutInfo(firstName = "Bob", lastName = "Brown", zipCode = "11111"))
            .cancelCheckout()
            .checkProducts { products ->
                assertThat(products).isNotEmpty
            }
    }

    @Test
    fun `can checkout from cart page`() {
        app.inventory()
            .resetApp()
            .addProductToCartByIndex(2)
            .goToCart()
            .checkCartItems { items ->
                assertThat(items).hasSize(1)
            }
            .startCheckout()
            .setCheckoutInfo(CheckoutInfo(firstName = "Carol", lastName = "White", zipCode = "67890"))
            .finishCheckout()
            .assertOrderComplete()
    }

    @Test
    fun `cart total matches overview total`() {
        var cartTotal = 0.0

        app.inventory()
            .resetApp()
            .addProductToCartByIndex(0)
            .addProductToCartByIndex(4)
            .goToCart()
            .checkCartItems { items ->
                cartTotal = items.sumOf { it.price.replace("[^0-9.]".toRegex(), "").toDouble() }
            }
            .startCheckout()
            .setCheckoutInfo(CheckoutInfo(firstName = "Dave", lastName = "Wilson", zipCode = "33333"))
            .checkTotal { total ->
                val overviewTotal = total.replace("[^0-9.]".toRegex(), "").toDouble()
                assertThat(String.format("%.2f", cartTotal)).isEqualTo(String.format("%.2f", overviewTotal))
            }
    }
}
