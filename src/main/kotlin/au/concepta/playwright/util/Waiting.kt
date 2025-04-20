@file:Suppress("unused")

package au.concepta.playwright.util

import java.time.Duration

/**
 * Waits for a certain state to be reached by running the predicate a number of attempts with some pause in between.
 */
fun waitUntil(
    attempts: Int = 50,
    waitTime: Duration = Duration.ofMillis(100),
    errorMessage: String = "Failed waiting for $attempts attempts",
    predicate: () -> Boolean,
) {
    var count = 0
    while (count++ <= attempts) {
        if (predicate()) {
            return
        }
        Thread.sleep(waitTime.toMillis())
    }
    throw AssertionError(errorMessage)
}

/**
 * Waits for an item to be produced by running a constructor method repeatedly until it returns a non-null value (or the
 * attempts run out).
 */
fun <T> waitFor(
    attempts: Int = 50,
    waitTime: Duration = Duration.ofMillis(100),
    errorMessage: String = "Failed to create object for $attempts attempts",
    constructor: () -> T?,
): T {
    var count = 0
    while (count++ <= attempts) {
        val value = constructor()
        if (value != null) {
            return value
        }
        Thread.sleep(waitTime.toMillis())
    }
    throw AssertionError(errorMessage)
}

/**
 * Retries the check until it either succeeds or the attempts run out.
 */
fun retry(
    attempts: Int = 50,
    waitTime: Duration = Duration.ofMillis(100),
    errorMessage: String = "Failed validation for $attempts attempts",
    block: () -> Unit,
) {
    var count = 0
    var lastError: Throwable? = null
    while (count++ <= attempts) {
        try {
            block()
            return
        } catch (t: Throwable) {
            lastError = t
        }
        Thread.sleep(waitTime.toMillis())
    }
    throw AssertionError(errorMessage, lastError)
}