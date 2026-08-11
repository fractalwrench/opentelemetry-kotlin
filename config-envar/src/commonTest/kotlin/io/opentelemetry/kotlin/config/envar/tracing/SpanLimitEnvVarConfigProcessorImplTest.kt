package io.opentelemetry.kotlin.config.envar.tracing

import io.opentelemetry.kotlin.config.envar.EnvVarConstants
import io.opentelemetry.kotlin.config.model.SpanLimitsConfigModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class SpanLimitEnvVarConfigProcessorImplTest {

    private val processor = SpanLimitEnvVarConfigProcessorImpl(
        envVars = EnvVarConstants.SpanLimits.envVars
    )

    @Test
    fun readsEverySpanLimitEnvironmentVariable() {
        val model = processor.configure(SpanLimitsConfigModel()) { envVar ->
            when (envVar) {
                "OTEL_SPAN_ATTRIBUTE_COUNT_LIMIT" -> "1"
                "OTEL_SPAN_ATTRIBUTE_VALUE_LENGTH_LIMIT" -> "2"
                "OTEL_SPAN_EVENT_COUNT_LIMIT" -> "3"
                "OTEL_SPAN_LINK_COUNT_LIMIT" -> "4"
                "OTEL_EVENT_ATTRIBUTE_COUNT_LIMIT" -> "5"
                "OTEL_LINK_ATTRIBUTE_COUNT_LIMIT" -> "6"
                else -> null
            }
        }

        assertEquals(
            SpanLimitsConfigModel(
                attributeCountLimit = 1,
                attributeValueLengthLimit = 2,
                eventCountLimit = 3,
                linkCountLimit = 4,
                attributeCountPerEventLimit = 5,
                attributeCountPerLinkLimit = 6,
            ),
            model,
        )
    }

    /**
     * An unset variable must leave its limit unset rather than filling in the SDK default,
     * otherwise the environment would silently outrank every other mechanism.
     */
    @Test
    fun declaresNothingWhenNoVariableIsSet() {
        assertEquals(SpanLimitsConfigModel(), processor.configure(SpanLimitsConfigModel()) { null })
    }

    @Test
    fun leavesUnsetVariablesUnsetWhileReadingTheOnesThatArePresent() {
        val model = processor.configure(SpanLimitsConfigModel()) { envVar ->
            "3".takeIf { envVar == "OTEL_SPAN_EVENT_COUNT_LIMIT" }
        }

        assertEquals(3, model.eventCountLimit)
        assertNull(model.attributeCountLimit)
        assertNull(model.attributeValueLengthLimit)
        assertNull(model.linkCountLimit)
        assertNull(model.attributeCountPerEventLimit)
        assertNull(model.attributeCountPerLinkLimit)
    }

    /**
     * Negative, non-numeric and out-of-`Int`-range values are not limits, so they are ignored
     * entirely rather than being clamped to something the caller did not ask for.
     */
    @Test
    fun ignoresValuesThatAreNotAValidLimit() {
        val model = processor.configure(SpanLimitsConfigModel()) { envVar ->
            when (envVar) {
                "OTEL_SPAN_ATTRIBUTE_COUNT_LIMIT" -> "-1"
                "OTEL_SPAN_ATTRIBUTE_VALUE_LENGTH_LIMIT" -> ""
                "OTEL_SPAN_EVENT_COUNT_LIMIT" -> "invalid"
                "OTEL_SPAN_LINK_COUNT_LIMIT" -> "2147483648"
                "OTEL_EVENT_ATTRIBUTE_COUNT_LIMIT" -> "-5"
                "OTEL_LINK_ATTRIBUTE_COUNT_LIMIT" -> "1.5"
                else -> null
            }
        }

        assertEquals(SpanLimitsConfigModel(), model)
    }

    @Test
    fun readsZeroAsAConfiguredLimit() {
        val model = processor.configure(SpanLimitsConfigModel()) { envVar ->
            "0".takeIf { envVar == "OTEL_SPAN_LINK_COUNT_LIMIT" }
        }

        assertEquals(SpanLimitsConfigModel(linkCountLimit = 0), model)
    }
}
