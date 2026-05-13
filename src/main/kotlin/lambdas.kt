import kotlin.test.assertEquals

fun validateCustomerData(block: (customer: Customer) -> Unit) {
    val customer = readCustomerFromPage()
    block(customer)
}

fun `test customer data shows correctly`() {
    // navigate to page showing customer data
    validateCustomerData { customer ->
        assertEquals("John", customer.firstName)
        assertEquals("Doe", customer.lastName)
        assertEquals("", customer.emailAddress)
        assertEquals("+61444555666", customer.mobileNumber)
    }
}


fun readCustomerFromPage() = Customer()
