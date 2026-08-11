package io.opentelemetry.kotlin.config.envar

import io.opentelemetry.kotlin.config.model.SpanLimitsConfigModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class EnvVarConfigSourceTest {

    /**
     * The resolver treats `null` as "this mechanism supplied no configuration", which is what an
     * environment that mentions none of the SDK's variables should look like.
     */
    @Test
    fun suppliesNoConfigurationWhenTheEnvironmentDeclaresNothing() {
        assertNull(EnvVarConfigSource(readEnvVar = { null }).resolve())
    }

    @Test
    fun suppliesNoConfigurationWhenEveryVariableIsInvalid() {
        assertNull(EnvVarConfigSource(readEnvVar = { "not-a-number" }).resolve())
    }

    @Test
    fun nestsSpanLimitsUnderTheTracerProvider() {
        val model = EnvVarConfigSource(
            readEnvVar = { envVar -> "5".takeIf { envVar == "OTEL_SPAN_EVENT_COUNT_LIMIT" } },
        ).resolve()

        assertEquals(
            SpanLimitsConfigModel(eventCountLimit = 5),
            model?.tracerProvider?.spanLimits,
        )
    }

    @Test
    fun readsEnvironmentVariablesByTheirSpecifiedNames() {
        val requested = mutableListOf<String>()
        val recordingReader: (String) -> String? = { envVar ->
            requested += envVar
            null
        }

        EnvVarConfigSource(readEnvVar = recordingReader).resolve()

        assertEquals(
            listOf(
                "OTEL_SPAN_ATTRIBUTE_COUNT_LIMIT",
                "OTEL_SPAN_ATTRIBUTE_VALUE_LENGTH_LIMIT",
                "OTEL_SPAN_EVENT_COUNT_LIMIT",
                "OTEL_SPAN_LINK_COUNT_LIMIT",
                "OTEL_EVENT_ATTRIBUTE_COUNT_LIMIT",
                "OTEL_LINK_ATTRIBUTE_COUNT_LIMIT",
            ),
            requested,
        )
    }
}
