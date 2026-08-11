package io.opentelemetry.kotlin.init

import io.opentelemetry.kotlin.clock.FakeClock
import io.opentelemetry.kotlin.tracing.Span
import io.opentelemetry.kotlin.tracing.model.SpanAdapter
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Covers span limits reaching the tracer from every configuration mechanism, and the precedence
 * between them, matching what `:implementation` does so the two backends agree.
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
        val limits = spanLimits(config = { tracerProvider { spanLimits { eventCountLimit = 3 } } })

        assertEquals(3, limits.eventCountLimit)
    }

    /**
     * Programmatic configuration is the more deliberate statement, so it outranks the environment.
     */
    @Test
    fun theDslWinsOverAnEnvironmentVariableForTheSameLimit() {
        val limits = spanLimits(
            env = mapOf("OTEL_SPAN_EVENT_COUNT_LIMIT" to "3"),
            config = { tracerProvider { spanLimits { eventCountLimit = 9 } } },
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
            config = { tracerProvider { spanLimits { attributeCountLimit = 5 } } },
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
     * The resolved limits must reach the Java SDK, which is what enforces them on exported spans.
     */
    @Test
    fun theResolvedLimitsReachTheJavaSdk() {
        val limits = spanLimits(env = mapOf("OTEL_SPAN_EVENT_COUNT_LIMIT" to "3"))

        assertEquals(3, limits.toOtelJavaSpanLimits().maxNumberOfEvents)
    }

    /**
     * And also the Kotlin-side span, which mirrors events and links back to the caller.
     */
    @Test
    fun anEnvironmentConfiguredEventLimitDropsEventsOnTheKotlinSpan() {
        val tracer = tracerProvider(env = mapOf("OTEL_SPAN_EVENT_COUNT_LIMIT" to "1"))
            .getTracer("test")

        val span = tracer.startSpan("span").apply {
            addEvent("kept")
            addEvent("dropped")
            end()
        }

        assertEquals(listOf("kept"), (span as SpanAdapter).events.map { it.name })
    }

    @Test
    fun theDslStillWinsOnTheKotlinSpan() {
        val tracer = tracerProvider(
            env = mapOf("OTEL_SPAN_EVENT_COUNT_LIMIT" to "1"),
            config = { tracerProvider { spanLimits { eventCountLimit = 5 } } },
        ).getTracer("test")

        val span: Span = tracer.startSpan("span").apply {
            repeat(3) { addEvent("event$it") }
            end()
        }

        assertEquals(3, (span as SpanAdapter).events.size)
    }

    private fun spanLimits(
        env: Map<String, String> = emptyMap(),
        config: OpenTelemetryConfigDsl.() -> Unit = {},
    ) = compatConfig(env, config).resolveSpanLimits()

    private fun tracerProvider(
        env: Map<String, String> = emptyMap(),
        config: OpenTelemetryConfigDsl.() -> Unit = {},
    ) = compatConfig(env, config).let { cfg ->
        cfg.tracerProviderConfig.build(
            clock = clock,
            idGenerator = cfg.resolveIdGenerator(),
            spanLimits = cfg.resolveSpanLimits(),
        )
    }

    private fun compatConfig(env: Map<String, String>, config: OpenTelemetryConfigDsl.() -> Unit) =
        CompatOpenTelemetryConfig(clock, readEnvVar = { env[it] }).apply(config)
}
