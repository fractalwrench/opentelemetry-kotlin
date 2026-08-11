package io.opentelemetry.kotlin.config.yaml

import io.opentelemetry.kotlin.config.model.OpenTelemetryConfigModel
import io.opentelemetry.kotlin.config.model.SpanLimitsConfigModel
import io.opentelemetry.kotlin.config.schema.model.AttributeLimits
import io.opentelemetry.kotlin.config.schema.model.OpenTelemetryConfiguration
import io.opentelemetry.kotlin.config.schema.model.SpanLimits
import io.opentelemetry.kotlin.config.schema.model.TracerProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class OpenTelemetryConfigurationMapperTest {

    @Test
    fun declaresNothingForADocumentThatConfiguresNoLimits() {
        assertEquals(OpenTelemetryConfigModel(), config().toConfigModel())
    }

    @Test
    fun declaresNothingWhenTheTracerProviderHasNoLimitsBlock() {
        val model = config(tracerProvider = TracerProvider(processors = emptyList())).toConfigModel()

        assertNull(model.tracerProvider)
    }

    @Test
    fun declaresNothingWhenTheLimitsBlockIsEmpty() {
        val model = config(spanLimits = SpanLimits()).toConfigModel()

        assertNull(model.tracerProvider)
    }

    /**
     * The schema and the SDK model use different names for the per-event and per-link attribute
     * limits, so a mix-up here would silently apply the wrong limit.
     */
    @Test
    fun mapsEverySpanLimitOntoTheMatchingModelField() {
        val model = config(
            spanLimits = SpanLimits(
                attributeCountLimit = 1,
                attributeValueLengthLimit = 2,
                linkCountLimit = 3,
                eventCountLimit = 4,
                eventAttributeCountLimit = 5,
                linkAttributeCountLimit = 6,
            ),
        ).toConfigModel()

        assertEquals(
            SpanLimitsConfigModel(
                attributeCountLimit = 1,
                attributeValueLengthLimit = 2,
                linkCountLimit = 3,
                eventCountLimit = 4,
                attributeCountPerEventLimit = 5,
                attributeCountPerLinkLimit = 6,
            ),
            model.tracerProvider?.spanLimits,
        )
    }

    @Test
    fun leavesLimitsTheDocumentOmitsUnset() {
        val model = config(spanLimits = SpanLimits(eventCountLimit = 64)).toConfigModel()

        assertEquals(64, model.tracerProvider?.spanLimits?.eventCountLimit)
        assertNull(model.tracerProvider?.spanLimits?.attributeCountLimit)
        assertNull(model.tracerProvider?.spanLimits?.linkCountLimit)
    }

    @Test
    fun readsZeroAsAConfiguredLimit() {
        val model = config(spanLimits = SpanLimits(linkCountLimit = 0)).toConfigModel()

        assertEquals(SpanLimitsConfigModel(linkCountLimit = 0), model.tracerProvider?.spanLimits)
    }

    /**
     * The schema types limits as `Long`, so a document can hold values the SDK cannot apply. Those
     * are dropped rather than clamped, leaving the limit for another mechanism to supply.
     */
    @Test
    fun ignoresLimitsTheSdkCannotRepresent() {
        val model = config(
            spanLimits = SpanLimits(
                attributeCountLimit = Int.MAX_VALUE.toLong() + 1,
                linkCountLimit = Long.MAX_VALUE,
                eventCountLimit = -1,
                attributeValueLengthLimit = 8,
            ),
        ).toConfigModel()

        assertEquals(SpanLimitsConfigModel(attributeValueLengthLimit = 8), model.tracerProvider?.spanLimits)
    }

    @Test
    fun keepsTheLargestLimitTheSdkCanRepresent() {
        val model = config(spanLimits = SpanLimits(attributeValueLengthLimit = Int.MAX_VALUE.toLong())).toConfigModel()

        assertEquals(Int.MAX_VALUE, model.tracerProvider?.spanLimits?.attributeValueLengthLimit)
    }

    @Test
    fun appliesGeneralAttributeLimitsToSpansWhenThereIsNoLimitsBlock() {
        val model = config(
            attributeLimits = AttributeLimits(attributeCountLimit = 64, attributeValueLengthLimit = 256),
        ).toConfigModel()

        assertEquals(
            SpanLimitsConfigModel(attributeCountLimit = 64, attributeValueLengthLimit = 256),
            model.tracerProvider?.spanLimits,
        )
    }

    /**
     * The schema documents `tracer_provider.limits` as overriding `attribute_limits`.
     */
    @Test
    fun spanLimitsOverrideGeneralAttributeLimits() {
        val model = config(
            attributeLimits = AttributeLimits(attributeCountLimit = 64, attributeValueLengthLimit = 256),
            spanLimits = SpanLimits(attributeCountLimit = 32),
        ).toConfigModel()

        assertEquals(32, model.tracerProvider?.spanLimits?.attributeCountLimit)
        assertEquals(256, model.tracerProvider?.spanLimits?.attributeValueLengthLimit)
    }

    @Test
    fun mapsAParsedDocument() {
        val parsed = OpenTelemetryConfigurationParser().parse(
            """
            file_format: "1.0"
            attribute_limits:
              attribute_value_length_limit: 256
            tracer_provider:
              processors: []
              limits:
                attribute_count_limit: 32
                event_count_limit: 64
            """.trimIndent()
        )

        assertEquals(
            SpanLimitsConfigModel(
                attributeCountLimit = 32,
                attributeValueLengthLimit = 256,
                eventCountLimit = 64,
            ),
            parsed.toConfigModel().tracerProvider?.spanLimits,
        )
    }

    private fun config(
        attributeLimits: AttributeLimits? = null,
        spanLimits: SpanLimits? = null,
        tracerProvider: TracerProvider? = spanLimits?.let { TracerProvider(processors = emptyList(), limits = it) },
    ) = OpenTelemetryConfiguration(
        fileFormat = "1.0",
        attributeLimits = attributeLimits,
        tracerProvider = tracerProvider,
    )
}
