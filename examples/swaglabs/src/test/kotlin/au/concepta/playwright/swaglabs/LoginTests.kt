package au.concepta.playwright.swaglabs

import au.concepta.playwright.TestBase
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class LoginTests : TestBase<SwagLabsApp>() {
    init {
        registerApplication(SwagLabsApp())
    }

    private val app = getApplication()

    @Test
    fun `standard user can log in`() {
        app.start()
            .loginAs("standard_user")
    }

    @Test
    fun `error shown for locked out user`() {
        app.start()
            .loginAsWithError("locked_out_user")
            .assertErrorMessageContains("Epic sadface")
    }

    @Test
    fun `error shown for invalid credentials`() {
        app.start()
            .loginWithInvalidCredentials()
            .assertErrorMessageContains("Epic sadface")
    }

    @Test
    fun `error message text is correct for locked out user`() {
        app.start()
            .loginAsWithError("locked_out_user")
            .assertErrorMessageContains("locked out")
    }

    @ParameterizedTest
    @ValueSource(strings = ["standard_user", "problem_user", "performance_glitch_user", "error_user", "visual_user"])
    fun `all standard users can log in with default password`(username: String) {
        app.start()
            .loginAs(username)
    }
}
