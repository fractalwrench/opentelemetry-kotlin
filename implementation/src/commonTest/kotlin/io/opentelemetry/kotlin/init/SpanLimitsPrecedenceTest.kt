package io.opentelemetry.kotlin.init

import io.opentelemetry.kotlin.clock.FakeClock
import io.opentelemetry.kotlin.factory.ContextFactoryImpl
import io.opentelemetry.kotlin.factory.IdGeneratorImpl
import io.opentelemetry.kotlin.factory.SpanContextFactoryImpl
import io.opentelemetry.kotlin.factory.SpanFactoryImpl
import io.opentelemetry.kotlin.factory.TraceFlagsFactoryImpl
import io.opentelemetry.kotlin.factory.TraceStateFactoryImpl
import io.opentelemetry.kotlin.tracing.TracerProviderImpl
import io.opentelemetry.kotlin.tracing.export.FakeSpanProcessor
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers span limits reaching the tracer from every configuration mechanism, and the precedence
 * between them.
 *
 * The environment is injected rather than read from the process so that the test does not depend
 * on how it was launched.
 */
internal class SpanLimitsPrecedenceTest {

    private val clock = FakeClock()

    @Test
    fun environmentVariablesConfigureTheLimits() {
        val limits = spanLimits(env = mapOf("OTEL_SPAN_EVENT_COUNT_LIMIT" to "3"))

        assertEquals(3, limits.eventCountLimit)
    }

    @Test
    fun theDslConfiguresTheLimits() {
        val limits = spanLimits(dsl = { eventCountLimit = 3 })

        assertEquals(3, limits.eventCountLimit)
    }

    /**
     * Programmatic configuration is the more deliberate statement, so it outranks the environment.
     */
    @Test
    fun theDslWinsOverAnEnvironmentVariableForTheSameLimit() {
        val limits = spanLimits(
            env = mapOf("OTEL_SPAN_EVENT_COUNT_LIMIT" to "3"),
            dsl = { eventCountLimit = 9 },
        )

        assertEquals(9, limits.eventCountLimit)
    }

    @Test
    fun eachMechanismSuppliesTheLimitsTheOtherLeavesUnset() {
        val limits = spanLimits(
            env = mapOf(
                "OTEL_SPAN_EVENT_COUNT_LIMIT" to "3",
                "OTEL_SPAN_LINK_COUNT_LIMIT" to "4",
            ),
            dsl = { attributeCountLimit = 5 },
        )

        assertEquals(3, limits.eventCountLimit)
        assertEquals(4, limits.linkCountLimit)
        assertEquals(5, limits.attributeCountLimit)
        assertEquals(128, limits.attributeCountPerEventLimit)
    }

    @Test
    fun globalAttributeLimitsFromTheDslWinOverTheEnvironment() {
        val limits = spanLimits(
            env = mapOf("OTEL_SPAN_ATTRIBUTE_COUNT_LIMIT" to "3"),
            config = { attributeLimits { attributeCountLimit = 9 } },
        )

        assertEquals(9, limits.attributeCountLimit)
    }

    @Test
    fun unconfiguredLimitsFallBackToTheSpecificationDefaults() {
        val limits = spanLimits()

        assertEquals(128, limits.eventCountLimit)
        assertEquals(128, limits.linkCountLimit)
        assertEquals(128, limits.attributeCountLimit)
        assertEquals(Int.MAX_VALUE, limits.attributeValueLengthLimit)
    }

    /**
     * The limits must reach the span itself, not just the config object: an event beyond an
     * environment-configured limit is dropped and counted.
     */
    @Test
    fun anEnvironmentConfiguredEventLimitDropsEventsOnRealSpans() {
        val processor = FakeSpanProcessor()

        recordEvents(processor, env = mapOf("OTEL_SPAN_EVENT_COUNT_LIMIT" to "1"), events = 2)

        val span = processor.endCalls.single()
        assertEquals(listOf("kept0"), span.events.map { it.name })
        assertEquals(1, span.droppedEventsCount)
    }

    @Test
    fun theDslStillWinsOnRealSpans() {
        val processor = FakeSpanProcessor()

        recordEvents(
            processor,
            env = mapOf("OTEL_SPAN_EVENT_COUNT_LIMIT" to "1"),
            events = 3,
            config = { tracerProvider { spanLimits { eventCountLimit = 5 } } },
        )

        val span = processor.endCalls.single()
        assertEquals(3, span.events.size)
        assertEquals(0, span.droppedEventsCount)
    }

    private fun recordEvents(
        processor: FakeSpanProcessor,
        env: Map<String, String>,
        events: Int,
        config: OpenTelemetryConfigDsl.() -> Unit = {},
    ) {
        val tracer = tracerProvider(env) {
            tracerProvider { export { processor } }
            config()
        }.getTracer("test")

        tracer.startSpan("span").apply {
            repeat(events) { addEvent("kept$it") }
            end()
        }
    }

    private fun spanLimits(
        env: Map<String, String> = emptyMap(),
        dsl: (SpanLimitsConfigDsl.() -> Unit)? = null,
        config: OpenTelemetryConfigDsl.() -> Unit = {},
    ) = openTelemetryConfig(env, dsl, config).generateTracingConfig().spanLimits

    private fun tracerProvider(
        env: Map<String, String> = emptyMap(),
        config: OpenTelemetryConfigDsl.() -> Unit = {},
    ): TracerProviderImpl {
        val traceFlags = TraceFlagsFactoryImpl()
        val spanContext = SpanContextFactoryImpl(IdGeneratorImpl(), traceFlags, TraceStateFactoryImpl())
        val spanFactory = SpanFactoryImpl(spanContext)
        return TracerProviderImpl(
            clock = clock,
            tracingConfig = openTelemetryConfig(env, dsl = null, config).generateTracingConfig(),
            contextFactory = ContextFactoryImpl(spanFactory),
            spanContextFactory = spanContext,
            traceFlagsFactory = traceFlags,
            spanFactory = spanFactory,
            idGenerator = IdGeneratorImpl(),
        )
    }

    private fun openTelemetryConfig(
        env: Map<String, String>,
        dsl: (SpanLimitsConfigDsl.() -> Unit)?,
        config: OpenTelemetryConfigDsl.() -> Unit,
    ) = OpenTelemetryConfigImpl(clock, readEnvVar = { env[it] }).apply {
        dsl?.let { action -> tracerProvider { spanLimits(action) } }
        config()
    }
}
