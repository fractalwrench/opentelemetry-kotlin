package io.opentelemetry.kotlin.config.model

import kotlin.test.Test
import kotlin.test.assertEquals

internal class SpanLimitsTest {

    @Test
    fun appliesEveryDefaultWhenNoMechanismConfiguredAnything() {
        val nothingConfigured: SpanLimitsConfigModel? = null

        assertEquals(SpanLimits(), nothingConfigured.resolve())
    }

    @Test
    fun anEmptyModelResolvesTheSameAsNoModelAtAll() {
        assertEquals(SpanLimitsConfigModel().resolve(), (null as SpanLimitsConfigModel?).resolve())
    }

    @Test
    fun defaultsMatchTheSpecification() {
        val limits = SpanLimits()

        assertEquals(128, limits.attributeCountLimit)
        assertEquals(Int.MAX_VALUE, limits.attributeValueLengthLimit)
        assertEquals(128, limits.linkCountLimit)
        assertEquals(128, limits.eventCountLimit)
        assertEquals(128, limits.attributeCountPerEventLimit)
        assertEquals(128, limits.attributeCountPerLinkLimit)
    }

    @Test
    fun keepsDefaultsForTheLimitsAModelLeavesUnset() {
        val limits = SpanLimitsConfigModel(eventCountLimit = 7).resolve()

        assertEquals(7, limits.eventCountLimit)
        assertEquals(128, limits.attributeCountLimit)
        assertEquals(128, limits.linkCountLimit)
        assertEquals(Int.MAX_VALUE, limits.attributeValueLengthLimit)
    }

    /**
     * Zero is a legitimate limit meaning "capture none of these", so it must survive resolution
     * rather than being mistaken for an unset value and replaced by the default.
     */
    @Test
    fun preservesAConfiguredValueOfZero() {
        val limits = SpanLimitsConfigModel(
            attributeCountLimit = 0,
            attributeValueLengthLimit = 0,
            linkCountLimit = 0,
            eventCountLimit = 0,
            attributeCountPerEventLimit = 0,
            attributeCountPerLinkLimit = 0,
        ).resolve()

        assertEquals(
            SpanLimits(
                attributeCountLimit = 0,
                attributeValueLengthLimit = 0,
                linkCountLimit = 0,
                eventCountLimit = 0,
                attributeCountPerEventLimit = 0,
                attributeCountPerLinkLimit = 0,
            ),
            limits,
        )
    }

    @Test
    fun carriesEveryConfiguredLimitThrough() {
        val limits = SpanLimitsConfigModel(
            attributeCountLimit = 1,
            attributeValueLengthLimit = 2,
            linkCountLimit = 3,
            eventCountLimit = 4,
            attributeCountPerEventLimit = 5,
            attributeCountPerLinkLimit = 6,
        ).resolve()

        assertEquals(
            SpanLimits(
                attributeCountLimit = 1,
                attributeValueLengthLimit = 2,
                linkCountLimit = 3,
                eventCountLimit = 4,
                attributeCountPerEventLimit = 5,
                attributeCountPerLinkLimit = 6,
            ),
            limits,
        )
    }

    /**
     * Resolution happens after merging, so the value the highest-precedence layer supplied is the
     * one that must reach the SDK.
     */
    @Test
    fun resolvesTheResultOfAMerge() {
        val merged = SpanLimitsConfigModel(eventCountLimit = 1, linkCountLimit = 2)
            .mergeWith(SpanLimitsConfigModel(eventCountLimit = 9))

        val limits = merged.resolve()

        assertEquals(9, limits.eventCountLimit)
        assertEquals(2, limits.linkCountLimit)
        assertEquals(128, limits.attributeCountLimit)
    }
}
