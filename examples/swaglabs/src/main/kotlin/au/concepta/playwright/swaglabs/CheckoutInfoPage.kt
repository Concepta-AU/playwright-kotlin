package au.concepta.playwright.swaglabs

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import au.concepta.playwright.ApplicationPage
import au.concepta.playwright.util.*

class CheckoutInfoPage(
    page: Page,
    pageElement: Locator
) : ApplicationPage<CheckoutInfoPage>(page, pageElement) {

    fun fillInfo(firstName: String, lastName: String, zipCode: String): CheckoutOverviewPage {
        page.locator("[data-test='firstName']").fill(firstName)
        page.locator("[data-test='lastName']").fill(lastName)
        page.locator("[data-test='postalCode']").fill(zipCode)
        page.locator("[data-test='continue']").click()
        return CheckoutOverviewPage(page, page.locator("[data-test='title']"))
    }
}
