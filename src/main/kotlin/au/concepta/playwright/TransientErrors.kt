package au.concepta.playwright

/**
 * Chromium network error codes that [Application] treats as *transient*: the browser is reporting that the
 * **connection** died, not that the application under test did something wrong.
 *
 * Every failed request the browser logs looks the same to the console-error guard -
 * `Failed to load resource: net::ERR_...` - whether the connection broke underneath an otherwise healthy request
 * or the application asked for something that does not exist. Only the first kind belongs here. Failing the
 * currently running test on such an event says nothing about what that test was doing; it is whichever test
 * happened to be running when the network hiccuped.
 *
 * Why each code is on the list:
 *
 * - `ERR_NETWORK_CHANGED` - the host's network configuration changed mid-request (an interface appearing or
 *   disappearing, a VPN connecting, container bridges being created and removed by a parallel build).
 * - `ERR_NETWORK_IO_SUSPENDED` - the operating system suspended network I/O, e.g. across a sleep/resume.
 * - `ERR_INTERNET_DISCONNECTED` - the host briefly had no route at all.
 * - `ERR_HTTP2_PING_FAILED` - an HTTP/2 keepalive ping went unanswered, so Chromium tore the connection down.
 *   A dead keepalive is a statement about the link, not about the response; high-latency links make it likely.
 * - `ERR_CONNECTION_RESET` / `ERR_CONNECTION_CLOSED` / `ERR_CONNECTION_TIMED_OUT` - an *established* connection
 *   died before the response completed: the TCP-level equivalents of the HTTP/2 case above, and the usual way a
 *   long-haul link or an intermediary drops a request in flight.
 *
 * Deliberately **not** on the list, because tolerating them would hide real defects:
 *
 * - `ERR_NAME_NOT_RESOLVED`, `ERR_CONNECTION_REFUSED`, `ERR_ADDRESS_UNREACHABLE` - nothing was ever connected;
 *   the application asked for a host or port that is not there. That is a broken asset URL or a misconfigured
 *   endpoint, i.e. exactly what the guard exists to catch.
 * - `ERR_ABORTED` - the client cancelled the request; tolerating it would mask requests the application really
 *   does abandon.
 * - `ERR_CERT_*`, `ERR_BLOCKED_BY_*` - deliberate refusals by the browser, always worth failing on.
 *
 * Ordinary HTTP failures carry no `net::` code at all (Chromium logs "the server responded with a status of
 * 404/500"), so a failing API call can never match this list.
 */
val TRANSIENT_NET_ERROR_CODES: Set<String> = setOf(
    "ERR_NETWORK_CHANGED",
    "ERR_NETWORK_IO_SUSPENDED",
    "ERR_INTERNET_DISCONNECTED",
    "ERR_HTTP2_PING_FAILED",
    "ERR_CONNECTION_RESET",
    "ERR_CONNECTION_CLOSED",
    "ERR_CONNECTION_TIMED_OUT",
)

/** Matches a whole Chromium net error code, so an unlisted code can never match as a prefix of a listed one. */
private val NET_ERROR_CODE = Regex("""net::(ERR_[A-Z0-9_]+)""")

/**
 * Whether [text] is a browser error message about the connection failing rather than about the application.
 *
 * Used by [Application] to decide whether an error is tolerated instead of failing the test, unless
 * `tolerateTransientErrors` is turned off. See [TRANSIENT_NET_ERROR_CODES] for the list and the reasoning
 * behind it.
 */
fun isTransientError(text: String): Boolean =
    NET_ERROR_CODE.findAll(text).any { it.groupValues[1] in TRANSIENT_NET_ERROR_CODES }
