package io.opentelemetry.kotlin.config

import io.opentelemetry.kotlin.config.dsl.buildSpanLimitsModel
import io.opentelemetry.kotlin.config.model.OpenTelemetryConfigModel
import io.opentelemetry.kotlin.config.model.SpanLimitsConfigModel
import io.opentelemetry.kotlin.config.model.TracerProviderConfigModel
import io.opentelemetry.kotlin.config.model.resolve
import io.opentelemetry.kotlin.config.schema.model.OpenTelemetryConfiguration
import io.opentelemetry.kotlin.config.schema.model.SpanLimits
import io.opentelemetry.kotlin.config.schema.model.TracerProvider
import io.opentelemetry.kotlin.config.yaml.toConfigModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Covers the precedence a user actually experiences, with each layer supplied the way the SDK
 * supplies it.
 */
internal class ResolveOpenTelemetryConfigTest {

    @Test
    fun leavesEverythingUnsetWhenNoMechanismConfiguresAnything() {
        val resolved = resolveOpenTelemetryConfig(readEnvVar = { null })

        assertEquals(OpenTelemetryConfigModel(), resolved)
    }

    @Test
    fun appliesTheDslOnItsOwn() {
        val resolved = resolveOpenTelemetryConfig(
            dsl = dslConfig { eventCountLimit = 5 },
            readEnvVar = { null },
        )

        assertEquals(5, resolved.spanLimits()?.eventCountLimit)
    }

    @Test
    fun appliesEnvironmentVariablesOnTheirOwn() {
        val resolved = resolveOpenTelemetryConfig(readEnvVar = envVars("OTEL_SPAN_EVENT_COUNT_LIMIT" to "5"))

        assertEquals(5, resolved.spanLimits()?.eventCountLimit)
    }

    @Test
    fun appliesTheDeclarativeFileOnItsOwn() {
        val resolved = resolveOpenTelemetryConfig(
            declarativeFile = fileConfig(SpanLimits(eventCountLimit = 5)),
            readEnvVar = { null },
        )

        assertEquals(5, resolved.spanLimits()?.eventCountLimit)
    }

    /**
     * Programmatic configuration is the most deliberate statement a user can make, so it outranks
     * the environment rather than the other way round.
     */
    @Test
    fun dslOverridesEnvironmentVariables() {
        val resolved = resolveOpenTelemetryConfig(
            dsl = dslConfig { eventCountLimit = 50 },
            readEnvVar = envVars(
                "OTEL_SPAN_EVENT_COUNT_LIMIT" to "5",
                "OTEL_SPAN_LINK_COUNT_LIMIT" to "6",
            ),
        )

        assertEquals(50, resolved.spanLimits()?.eventCountLimit)
        assertEquals(6, resolved.spanLimits()?.linkCountLimit)
    }

    @Test
    fun dslOverridesTheDeclarativeFile() {
        val resolved = resolveOpenTelemetryConfig(
            dsl = dslConfig { eventCountLimit = 50 },
            declarativeFile = fileConfig(SpanLimits(eventCountLimit = 5, linkCountLimit = 6)),
            readEnvVar = { null },
        )

        assertEquals(50, resolved.spanLimits()?.eventCountLimit)
        assertEquals(6, resolved.spanLimits()?.linkCountLimit)
    }

    /**
     * A declarative file is a complete description of the configuration, so it replaces the
     * environment rather than merging on top of it.
     */
    @Test
    fun theDeclarativeFileReplacesEnvironmentVariables() {
        val resolved = resolveOpenTelemetryConfig(
            declarativeFile = fileConfig(SpanLimits(eventCountLimit = 5)),
            readEnvVar = envVars("OTEL_SPAN_LINK_COUNT_LIMIT" to "6"),
        )

        assertEquals(5, resolved.spanLimits()?.eventCountLimit)
        assertNull(resolved.spanLimits()?.linkCountLimit)
    }

    @Test
    fun dslWinsOverBothLowerLayers() {
        val resolved = resolveOpenTelemetryConfig(
            dsl = dslConfig { eventCountLimit = 500 },
            declarativeFile = fileConfig(SpanLimits(eventCountLimit = 50)),
            readEnvVar = envVars("OTEL_SPAN_EVENT_COUNT_LIMIT" to "5"),
        )

        assertEquals(500, resolved.spanLimits()?.eventCountLimit)
    }

    /**
     * Whatever no mechanism configured must still end up at the specification default.
     */
    @Test
    fun unconfiguredLimitsResolveToTheirDefaults() {
        val resolved = resolveOpenTelemetryConfig(
            dsl = dslConfig { eventCountLimit = 5 },
            readEnvVar = { null },
        )

        val limits = resolved.spanLimits().resolve()

        assertEquals(5, limits.eventCountLimit)
        assertEquals(128, limits.attributeCountLimit)
        assertEquals(128, limits.linkCountLimit)
        assertEquals(Int.MAX_VALUE, limits.attributeValueLengthLimit)
    }

    private fun OpenTelemetryConfigModel.spanLimits(): SpanLimitsConfigModel? = tracerProvider?.spanLimits

    private fun dslConfig(spanLimits: io.opentelemetry.kotlin.init.SpanLimitsConfigDsl.() -> Unit) =
        OpenTelemetryConfigModel(
            tracerProvider = TracerProviderConfigModel(spanLimits = buildSpanLimitsModel(spanLimits = spanLimits)),
        )

    private fun fileConfig(spanLimits: SpanLimits) = OpenTelemetryConfiguration(
        fileFormat = "1.0",
        tracerProvider = TracerProvider(processors = emptyList(), limits = spanLimits),
    ).toConfigModel()

    private fun envVars(vararg entries: Pair<String, String>): (String) -> String? {
        val values = entries.toMap()
        return { values[it] }
    }
}
