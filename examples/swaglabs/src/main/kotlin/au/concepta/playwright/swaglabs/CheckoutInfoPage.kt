package au.concepta.playwright.swaglabs

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import au.concepta.playwright.util.*

class CheckoutInfoPage(
    page: Page,
    pageElement: Locator
) : SwagLabsPage<CheckoutInfoPage>(page, pageElement) {

    data class CheckoutInfo(
        val firstName: String = "John",
        val lastName: String = "Doe",
        val zipCode: String = "12345"
    )

    fun setFirstName(firstName: String): CheckoutInfoPage {
        page.locator("[data-test='firstName']").fill(firstName)
        return downcast()
    }

    fun setLastName(lastName: String): CheckoutInfoPage {
        page.locator("[data-test='lastName']").fill(lastName)
        return downcast()
    }

    fun setPostalCode(zipCode: String): CheckoutInfoPage {
        page.locator("[data-test='postalCode']").fill(zipCode)
        return downcast()
    }

    fun submitInfo(): CheckoutOverviewPage {
        page.locator("[data-test='continue']").click()
        return CheckoutOverviewPage(page, page.locator("[data-test='title']"))
    }

    fun setCheckoutInfo(info: CheckoutInfo = CheckoutInfo()): CheckoutOverviewPage {
        setFirstName(info.firstName)
        setLastName(info.lastName)
        setPostalCode(info.zipCode)
        return submitInfo()
    }

    fun setCheckoutInfo(firstName: String, lastName: String, zipCode: String): CheckoutOverviewPage {
        return setCheckoutInfo(CheckoutInfo(firstName, lastName, zipCode))
    }
}
