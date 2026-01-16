package io.opentelemetry.kotlin.tracing.model

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.framework.OtelKotlinHarness
import io.opentelemetry.kotlin.tracing.export.FakeSpanProcessor
import org.junit.Test
import kotlin.test.BeforeTest
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalApi::class)
internal class SpanAdapterTest {

    private lateinit var harness: OtelKotlinHarness

    @BeforeTest
    fun setUp() {
        harness = OtelKotlinHarness()
    }

    @Test
    fun `span auto closeable`() {
        val processor = FakeSpanProcessor(
            startAction = { span, _ ->
                assertFalse(span.hasEnded)
            },
            endAction = { span ->
                assertTrue(span.hasEnded)
            },
        )
        harness.config.spanProcessors.add(processor)
        harness.tracer.createSpan("name").use { span ->
            assertTrue(span.isRecording())
        }
        harness.assertSpans(expectedCount = 1)
    }
}
