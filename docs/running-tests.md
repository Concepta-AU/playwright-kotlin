Running Tests
=============

Prerequisites
-------------

Install Playwright browsers:

```bash
./gradlew playwrightMain
./gradlew playwrightTest
```

Run tests with Gradle:

```bash
./gradlew test
```

Tests run in parallel by default (up to half the available CPU cores).

Environment Variables
---------------------

| Variable | Purpose | Default |
|---|---|---|
| `BASE_URL` | Override the base URL navigated to on `Application.start()` | `defaultBaseUrl` property of your `Application` subclass |
| `VIEW_SPEED` | If set, runs the browser in headed mode with slow-motion (value in ms). If unset, runs headless with no delay | headless, no delay |
| `VIDEO_DIR` | If set, records a video to this directory for each browser context | no recording |
| `SAVE_ALL_TRACES` | If `"true"`, saves a Playwright trace (`.zip`) for every test. Otherwise, traces are saved only when a test fails | only on failure |

### Trace Files

Traces are written to `traces/<package>/<TestClassName>/<testName>.zip`. By default, this happens only on failed tests,
set `SAVE_ALL_TRACES` if you want all.

### Browser Context

Each call to `Application.start()` creates a fresh `BrowserContext`. The underlying Playwright driver and Chromium 
browser are shared per thread across all tests of the same `Application` subclass. The browser is recreated 
automatically if it disconnects.

### Console and Page Errors

By default, any `console.error` or `pageerror` emitted during a test will fail it. To expect and suppress specific errors:

```kotlin
app.expectError("Script error.")
app.expectError { it.contains("Expected error pattern") }
```

Collected console messages and page errors are printed to stdout after each test.

### Transient Transport Errors

Errors where the browser reports that the *connection* died - `net::ERR_NETWORK_CHANGED` when the host's network
configuration changes mid-request, `net::ERR_HTTP2_PING_FAILED` when a keepalive goes unanswered, and the other codes
listed in `TRANSIENT_NET_ERROR_CODES` - are **tolerated by default**. Without that, a network hiccup fails whichever
test happens to be running, with a message about a transport event rather than about what the test was doing.

They are not silently dropped: each one is printed as it happens and again in a per-test summary,

```
==== 2 Transient Error(s) Tolerated in default ====
 Failed to load resource: net::ERR_NETWORK_CHANGED
```

and collected in `Application.transientErrors`, so a run that swallowed twenty of them is visibly different from a
clean one. The scope is deliberately narrow: errors saying the application asked for something wrong
(`ERR_NAME_NOT_RESOLVED`, `ERR_CONNECTION_REFUSED`, an ordinary 404 or 500) still fail the test - see the KDoc on
`TRANSIENT_NET_ERROR_CODES` for the reasoning per code. Note that the failed request is *not* retried, so a test
may still fail afterwards because its data never arrived - on its own assertion, which is the point.

Turn it off by overriding the property in your `Application` subclass:

```kotlin
override val tolerateTransientErrors = false
```

Accessibility Testing
---------------------

Call `validateAccessibility()` on any `ApplicationPage` to run axe-core checks:

```kotlin
dashboard.validateAccessibility()
```

The test fails if any violations are found, with a summary of each violation's ID, impact level, and description.
