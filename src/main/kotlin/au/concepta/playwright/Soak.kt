@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package au.concepta.playwright

import com.microsoft.playwright.CDPSession
import com.microsoft.playwright.Page

/**
 * The browser's retained footprint at one moment, as the Chrome DevTools Protocol reports it.
 *
 * [nodes], [listeners] and [documents] are counts, and a leak moves them monotonically upwards - they are what
 * [assertNoLeak] judges. [heapBytes] is carried for context only: it varies between runs regardless of what the
 * application does, so asserting on it produces noise rather than findings.
 */
data class MemoryMetrics(
    /** Live DOM nodes across all documents (`Nodes`). */
    val nodes: Int,
    /** Registered JavaScript event listeners (`JSEventListeners`). */
    val listeners: Int,
    /** Live documents, i.e. the main document plus any frame that has not been torn down (`Documents`). */
    val documents: Int,
    /** Used JavaScript heap in bytes (`JSHeapUsedSize`). */
    val heapBytes: Long,
)

/** A [MemoryMetrics] reading taken after [iteration] runs of a soaked flow. */
data class SoakCheckpoint(
    val iteration: Int,
    val metrics: MemoryMetrics,
)

/**
 * What a [soak][Application.soak] run measured: a [baseline] taken after the warm-up iterations, samples along the
 * way, and a [final] reading after the last iteration.
 *
 * The intermediate [checkpoints] are what make a failure actionable. Growth that steps once and then flattens is a
 * cache filling up; growth that keeps climbing at the same rate is a leak. A before-and-after pair cannot tell the
 * two apart, so [trendTable] prints the whole series and [assertNoLeak] puts it in the failure message.
 */
class SoakResult(
    /** Iterations run before [baseline] was taken, so first-render and lazy-singleton allocation is not counted. */
    val warmup: Int,
    /** Iterations run after [baseline] and before [final]. */
    val iterations: Int,
    /** The reading the run is judged against, taken after the warm-up. */
    val baseline: MemoryMetrics,
    /** Readings taken every `checkpointEvery` iterations, excluding the final one. */
    val checkpoints: List<SoakCheckpoint>,
    /** The reading after the last iteration. */
    val final: MemoryMetrics,
) {
    /** Growth from [baseline] to [final], per counter. */
    val nodeGrowth: Int get() = final.nodes - baseline.nodes

    /** Growth from [baseline] to [final], per counter. */
    val listenerGrowth: Int get() = final.listeners - baseline.listeners

    /** Growth from [baseline] to [final], per counter. */
    val documentGrowth: Int get() = final.documents - baseline.documents

    /**
     * The whole series as a fixed-width table, each row carrying its growth against the baseline.
     *
     * Printed by [assertNoLeak] on failure, and worth printing on a passing run too: the numbers are only useful
     * for tightening tolerances if they are recorded somewhere.
     */
    fun trendTable(): String {
        val rows =
            listOf(Row("baseline (after $warmup warm-up)", baseline)) +
                checkpoints.map { Row(it.iteration.toString(), it.metrics) } +
                Row("final ($iterations)", final)
        val labelWidth = maxOf(rows.maxOf { it.label.length }, "Iteration".length)
        val header =
            "${"Iteration".padEnd(labelWidth)}  ${"Nodes".padStart(16)}  ${"Listeners".padStart(16)}  " +
                "${"Documents".padStart(16)}  ${"Heap MB".padStart(9)}"
        return (
            listOf(header, "-".repeat(header.length)) +
                rows.map { it.render(labelWidth) }
        ).joinToString("\n")
    }

    private inner class Row(
        val label: String,
        val metrics: MemoryMetrics,
    ) {
        fun render(labelWidth: Int): String =
            "${label.padEnd(labelWidth)}  ${cell(metrics.nodes, baseline.nodes)}  " +
                "${cell(metrics.listeners, baseline.listeners)}  ${cell(metrics.documents, baseline.documents)}  " +
                String.format("%9.1f", metrics.heapBytes / (1024.0 * 1024.0))

        private fun cell(value: Int, base: Int): String {
            val delta = value - base
            val growth = if (metrics === baseline) "" else " (${if (delta < 0) "" else "+"}$delta)"
            return "$value$growth".padStart(16)
        }
    }

    override fun toString(): String = trendTable()
}

/**
 * Fails the test when a soaked flow grew the browser's retained footprint beyond the tolerances.
 *
 * The tolerances exist because a repeated flow legitimately allocates a little - a cache entry, a rendered widget
 * the application keeps by design - and because a check that cries wolf gets muted, at which point it may as well
 * not exist. Set them generously from measured numbers and tighten them against recorded history, rather than
 * guessing a tight value up front.
 *
 * Heap size is never asserted on - see [MemoryMetrics.heapBytes].
 *
 * @param nodeTolerance DOM nodes the flow may retain in total, across all [SoakResult.iterations].
 * @param listenerTolerance event listeners the flow may retain in total.
 * @param documentTolerance documents the flow may retain in total.
 */
fun SoakResult.assertNoLeak(
    nodeTolerance: Int = 100,
    listenerTolerance: Int = 5,
    documentTolerance: Int = 1,
) {
    val exceeded =
        buildList {
            if (nodeGrowth > nodeTolerance) add("nodes grew by $nodeGrowth (tolerance $nodeTolerance)")
            if (listenerGrowth > listenerTolerance) {
                add("listeners grew by $listenerGrowth (tolerance $listenerTolerance)")
            }
            if (documentGrowth > documentTolerance) {
                add("documents grew by $documentGrowth (tolerance $documentTolerance)")
            }
        }
    if (exceeded.isEmpty()) return
    throw AssertionError(
        "The flow retained memory over $iterations iterations:\n" +
            exceeded.joinToString("\n") { "  - $it" } +
            "\n\nA counter that steps once and then flattens is a cache filling; one that keeps climbing at the " +
            "same rate is a leak.\n\n" + trendTable(),
    )
}

/**
 * Reads the retained-footprint counters of one page over the Chrome DevTools Protocol.
 *
 * One session per page, reused for every reading: opening a session is not free, and a fresh one per measurement
 * would itself show up in the numbers being measured. [Page] objects are bound to a browser context, so a probe
 * lives exactly as long as the context its page belongs to.
 *
 * Garbage is collected twice before each reading. One pass leaves objects that only became unreachable during that
 * pass still counted, which reads as growth that is not there.
 */
internal class MemoryProbe(
    page: Page,
    browserTypeName: String,
) {
    private val session: CDPSession

    init {
        // The protocol is Chromium's own. Playwright happily hands out a CDP session against another engine and
        // then fails deep inside the transport, which is not a diagnosis anybody can act on.
        require(browserTypeName == "chromium") {
            "Memory measurement needs the Chrome DevTools Protocol, which only Chromium speaks - this browser is " +
                "'$browserTypeName'. Launch Chromium for soak tests, or skip them on other engines."
        }
        session = page.context().newCDPSession(page)
        session.send("Performance.enable")
    }

    fun measure(): MemoryMetrics {
        session.send("HeapProfiler.collectGarbage")
        session.send("HeapProfiler.collectGarbage")
        val values =
            session
                .send("Performance.getMetrics")
                .getAsJsonArray("metrics")
                .associate { entry ->
                    val metric = entry.asJsonObject
                    metric["name"].asString to metric["value"].asDouble
                }
        fun read(name: String) =
            values[name] ?: throw IllegalStateException(
                "Performance.getMetrics reported no '$name' - it returned ${values.keys.sorted()}",
            )
        return MemoryMetrics(
            nodes = read("Nodes").toInt(),
            listeners = read("JSEventListeners").toInt(),
            documents = read("Documents").toInt(),
            heapBytes = read("JSHeapUsedSize").toLong(),
        )
    }
}
