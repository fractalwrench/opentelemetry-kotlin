package io.opentelemetry.kotlin.config.dsl

import io.opentelemetry.kotlin.config.model.SpanLimitsConfigModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class BuildSpanLimitsModelTest {

    @Test
    fun declaresNothingWhenNeitherBlockWasUsed() {
        assertNull(buildSpanLimitsModel())
        assertNull(buildSpanLimitsModel(globalAttributeLimits = AttributeLimitsConfigModelBuilder()))
        assertNull(buildSpanLimitsModel(spanLimits = {}))
    }

    @Test
    fun usesGlobalAttributeLimitsWhenNoSpanLimitsWereDeclared() {
        val model = buildSpanLimitsModel(globalAttributeLimits = globalLimits { attributeCountLimit = 64 })

        assertEquals(SpanLimitsConfigModel(attributeCountLimit = 64), model)
    }

    @Test
    fun usesSpanLimitsWhenNoGlobalAttributeLimitsWereDeclared() {
        val model = buildSpanLimitsModel(spanLimits = { eventCountLimit = 32 })

        assertEquals(SpanLimitsConfigModel(eventCountLimit = 32), model)
    }

    /**
     * Span-specific limits are the more precise statement, so they win where the two overlap.
     */
    @Test
    fun spanLimitsOverrideGlobalAttributeLimits() {
        val model = buildSpanLimitsModel(
            globalAttributeLimits = globalLimits {
                attributeCountLimit = 64
                attributeValueLengthLimit = 256
            },
            spanLimits = { attributeCountLimit = 32 },
        )

        assertEquals(32, model?.attributeCountLimit)
        assertEquals(256, model?.attributeValueLengthLimit)
    }

    /**
     * A span-limits block that does not mention an attribute limit must not wipe out the global
     * one, otherwise `attributeLimits { }` would be silently ignored whenever `spanLimits { }` is
     * present.
     */
    @Test
    fun aPartialSpanLimitsBlockKeepsTheGlobalValue() {
        val model = buildSpanLimitsModel(
            globalAttributeLimits = globalLimits { attributeValueLengthLimit = 256 },
            spanLimits = { eventCountLimit = 8 },
        )

        assertEquals(256, model?.attributeValueLengthLimit)
        assertEquals(8, model?.eventCountLimit)
        assertNull(model?.attributeCountLimit)
    }

    @Test
    fun leavesLimitsNeitherBlockMentionedUnset() {
        val model = buildSpanLimitsModel(
            globalAttributeLimits = globalLimits { attributeCountLimit = 64 },
            spanLimits = { linkCountLimit = 4 },
        )

        assertNull(model?.eventCountLimit)
        assertNull(model?.attributeCountPerEventLimit)
        assertNull(model?.attributeCountPerLinkLimit)
        assertNull(model?.attributeValueLengthLimit)
    }

    @Test
    fun globalAttributeLimitsOnlyContributeAttributeLimits() {
        val model = buildSpanLimitsModel(
            globalAttributeLimits = globalLimits {
                attributeCountLimit = 64
                attributeValueLengthLimit = 256
            },
        )

        assertEquals(
            SpanLimitsConfigModel(attributeCountLimit = 64, attributeValueLengthLimit = 256),
            model,
        )
    }

    private fun globalLimits(action: AttributeLimitsConfigModelBuilder.() -> Unit) =
        AttributeLimitsConfigModelBuilder().apply(action)
}
