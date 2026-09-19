package au.concepta.playwright

import com.microsoft.playwright.Page
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import kotlin.test.Test

/**
 * A page whose flow retains everything it builds: the nodes stay in the document and a reference on `window`
 * keeps them out of reach of the collector. Stands in for the real thing - a listener on `window` nobody removes,
 * an editor nobody destroys, a subscription nobody unsubscribes.
 */
private const val LEAKING_FLOW = """() => {
    window.retained = window.retained || [];
    const block = document.createElement('div');
    for (let i = 0; i < 50; i++) {
        block.appendChild(document.createElement('span'));
    }
    block.addEventListener('click', () => block.textContent);
    document.body.appendChild(block);
    window.retained.push(block);
}"""

/** The same work, released again at the end of the iteration - what a flow that cleans up after itself looks like. */
private const val CLEAN_FLOW = """() => {
    const block = document.createElement('div');
    for (let i = 0; i < 50; i++) {
        block.appendChild(document.createElement('span'));
    }
    const listener = () => block.textContent;
    block.addEventListener('click', listener);
    document.body.appendChild(block);
    block.removeEventListener('click', listener);
    block.remove();
}"""

class SoakApp : Application<DummyPage>() {
    override val defaultBaseUrl: String = "data:text/html,<html><body>Soak</body></html>"

    override fun getInitialApplicationPage(page: Page): DummyPage = DummyPage(page)

    // the point of the opt-out: tracing every iteration would perturb what is being measured
    override val recordTracing = false
}

/**
 * The soak helpers are only worth trusting if they are shown to fail on a leak and pass without one - a check
 * that cannot fail is indistinguishable from one that is not running. Both cases are driven here against pages
 * whose behaviour is known, rather than against an application whose behaviour is the open question.
 */
class SoakTests {
    // enough iterations for the leak to clear any tolerance, few enough to stay a unit test
    private val iterations = 20
    private val warmup = 3
    private val checkpointEvery = 5

    @Test
    fun `a flow that retains nodes and listeners fails the leak check`() {
        SoakApp().use { app ->
            val page = app.start().page
            val result = app.soak(iterations, warmup, checkpointEvery) { page.evaluate(LEAKING_FLOW) }

            // 51 nodes and one listener per iteration, so both counters are unmistakably past any sane tolerance
            assertThat(result.nodeGrowth).isGreaterThan(iterations * 50)
            assertThat(result.listenerGrowth).isGreaterThanOrEqualTo(iterations)

            assertThatThrownBy { result.assertNoLeak() }
                .isInstanceOf(AssertionError::class.java)
                .hasMessageContaining("The flow retained memory over $iterations iterations")
                .hasMessageContaining("nodes grew by")
                .hasMessageContaining("listeners grew by")
                // the trend, without which a failure is not actionable
                .hasMessageContaining("baseline (after $warmup warm-up)")
                .hasMessageContaining("final ($iterations)")
        }
    }

    @Test
    fun `a flow that releases what it builds passes the leak check`() {
        SoakApp().use { app ->
            val page = app.start().page
            val result = app.soak(iterations, warmup, checkpointEvery) { page.evaluate(CLEAN_FLOW) }

            result.assertNoLeak()
            assertThat(result.documentGrowth).isZero()
        }
    }

    @Test
    fun `the result carries a baseline, the intermediate checkpoints and a final reading`() {
        SoakApp().use { app ->
            val page = app.start().page
            val result = app.soak(iterations, warmup, checkpointEvery) { page.evaluate(CLEAN_FLOW) }

            // every checkpointEvery-th iteration except the last, which is the final reading instead
            assertThat(result.checkpoints.map { it.iteration }).containsExactly(5, 10, 15)
            assertThat(result.warmup).isEqualTo(warmup)
            assertThat(result.iterations).isEqualTo(iterations)
            assertThat(result.trendTable().lines()).hasSize(result.checkpoints.size + 4)
        }
    }

    @Test
    fun `a measurement reports the live document and a non-empty heap`() {
        SoakApp().use { app ->
            app.start()
            val metrics = app.measureMemory()

            assertThat(metrics.documents).isGreaterThanOrEqualTo(1)
            assertThat(metrics.nodes).isGreaterThan(0)
            assertThat(metrics.heapBytes).isGreaterThan(0)
            assertThat(metrics.listeners).isGreaterThanOrEqualTo(0)
        }
    }

    @Test
    fun `measuring before a test has started says so`() {
        SoakApp().use { app ->
            assertThatThrownBy { app.measureMemory() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("start()")
        }
    }
}
