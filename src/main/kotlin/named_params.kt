fun signUpCustomer(
    firstName: String = faker.firstName(),
    lastName: String = faker.lastName(),
    emailAddress: String = faker.emailAddress(),
    mobileNumber: String = faker.mobileNumber(),
) {
    // do something
}

fun testAnything() {
    signUpCustomer()
    // ...
}

fun testEmail() {
    signUpCustomer(emailAddress = "my@email.org")
    // ...
}

fun testPhone() {
    signUpCustomer(mobileNumber = "+49177463728")
    // ...
}

data class Customer(
    val firstName: String = faker.firstName(),
    val lastName: String = faker.lastName(),
    val emailAddress: String = faker.emailAddress(),
    val mobileNumber: String = faker.mobileNumber(),
)

fun generalCustomer() {
    val customer = Customer()
    // ...
}

fun emailCustomer() {
    val customer = Customer(emailAddress = "my@email.org")
    // ...
}

fun phoneCustomer() {
    val customer = Customer(mobileNumber = "+49177463728")
    // ...
}

class Faker {
    fun firstName() = "John"
    fun lastName() = "Doe"
    fun emailAddress() = "john@example.org"
    fun mobileNumber() = "+61444555666"
}

val faker = Faker()