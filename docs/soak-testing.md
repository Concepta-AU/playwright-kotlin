# Soak Testing

A single-page application is never reloaded after login, so anything it forgets to release — a listener on
`window`, a rich-text editor, a subscription — accumulates for the whole working session. An ordinary end-to-end
test cannot see that: it runs in a fresh browser context and lives for seconds.

A soak test drives one flow many times in a *single* context, forces garbage collection, and compares the
browser's retained footprint before and after.

## Writing one

```kotlin
class VersionPageSoak : TestBase<MyApp>() {
    @Test
    fun `opening and closing a version does not retain memory`() {
        val app = getApplication()
        val list = app.specifications()

        app.soak(iterations = 100, warmup = 5, checkpointEvery = 10) {
            list.openVersion("KOTLIN").back()
        }.also { println(it.trendTable()) }
            .assertNoLeak(nodeTolerance = 400, listenerTolerance = 20)
    }
}
```

`soak` returns a [`SoakResult`](#reading-the-result); `assertNoLeak` fails the test when the growth from the
baseline exceeds the tolerances. `measureMemory()` takes a single reading if you want to measure something the
loop shape does not fit.

Two properties of the loop matter more than anything else:

* **Loop over the same data.** A flow that opens a different record each time makes the application's caches grow
  legitimately, and no tolerance can tell that apart from a leak.
* **Warm up first.** The first few iterations allocate for real — root services, lazily created singletons, first
  render. `warmup` runs them before the baseline reading is taken.

Turn tracing off for the test, by overriding [`Application.recordTracing`](../src/main/kotlin/au/concepta/playwright/Application.kt)
with `false`. Tracing snapshots the DOM on every action: a hundred iterations of that both bloats the trace and
does in-page work that skews the counters being asserted on. Turn it back on to diagnose a failure.

## What is measured

Over the Chrome DevTools Protocol (`Performance.getMetrics`, after `HeapProfiler.collectGarbage` twice, so the
reading is taken after a settled collection):

| `MemoryMetrics` | CDP metric | Asserted on |
|---|---|---|
| `nodes` | `Nodes` | yes — live DOM nodes |
| `listeners` | `JSEventListeners` | yes — registered event listeners |
| `documents` | `Documents` | yes — live documents, including frames |
| `heapBytes` | `JSHeapUsedSize` | **no** — reported for context only |

Heap size varies between runs regardless of what the application does, so asserting on it produces noise rather
than findings. The three counts move monotonically when something is retained, which is what makes them usable.

CDP is Chromium's protocol; measuring against another engine fails with a message saying so.

## Reading the result

`SoakResult` carries the baseline, a checkpoint every `checkpointEvery` iterations, and the final reading.
`trendTable()` renders the series:

```
Iteration                              Nodes         Listeners         Documents    Heap MB
-------------------------------------------------------------------------------------------
baseline (after 3 warm-up)               158                16                 1        1.1
5                                 413 (+255)           21 (+5)            1 (+0)        1.1
10                                668 (+510)          26 (+10)            1 (+0)        1.1
final (20)                      1178 (+1020)          36 (+20)            1 (+0)        1.1
```

The intermediate checkpoints are what make a failure actionable: growth that steps once and then flattens is a
cache filling up, while growth that keeps climbing at the same rate is a leak. `assertNoLeak` puts the whole table
in its failure message for that reason, and printing it on a passing run is worth doing too — tolerances can only
be tightened against recorded numbers.

## Choosing tolerances

Prefer stability over sensitivity. A check that catches small leaks but cries wolf every third run gets muted, and
then it may as well not exist. Measure the flow first, set the tolerance generously above what a clean run shows,
and tighten it later against history.

A soak run is minutes rather than seconds, and a memory assertion is inherently more variance-prone than the rest
of a suite. Give soak tests their own tag and Gradle task rather than adding them to a suite that gates a merge,
and run them serially — a soak deliberately holds one context that grows.
