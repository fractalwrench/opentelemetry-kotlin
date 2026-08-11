package io.opentelemetry.kotlin.config.dsl

import io.opentelemetry.kotlin.config.model.SpanLimitsConfigModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class SpanLimitsConfigModelBuilderTest {

    @Test
    fun declaresNothingWhenTheDslNeverTouchedIt() {
        assertNull(SpanLimitsConfigModelBuilder().toModel())
    }

    @Test
    fun readsBackDefaultsBeforeAnythingIsAssigned() {
        val builder = SpanLimitsConfigModelBuilder()

        assertEquals(128, builder.attributeCountLimit)
        assertEquals(Int.MAX_VALUE, builder.attributeValueLengthLimit)
        assertEquals(128, builder.linkCountLimit)
        assertEquals(128, builder.eventCountLimit)
        assertEquals(128, builder.attributeCountPerEventLimit)
        assertEquals(128, builder.attributeCountPerLinkLimit)
    }

    /**
     * Assigning one limit must not implicitly declare the other five, otherwise the DSL would
     * smother every lower-precedence layer with SDK defaults.
     */
    @Test
    fun assigningOneLimitLeavesTheOthersUndeclared() {
        val model = SpanLimitsConfigModelBuilder().apply { eventCountLimit = 7 }.toModel()

        assertEquals(SpanLimitsConfigModel(eventCountLimit = 7), model)
    }

    @Test
    fun declaresEveryLimitThatWasAssigned() {
        val model = SpanLimitsConfigModelBuilder().apply {
            attributeCountLimit = 1
            attributeValueLengthLimit = 2
            linkCountLimit = 3
            eventCountLimit = 4
            attributeCountPerEventLimit = 5
            attributeCountPerLinkLimit = 6
        }.toModel()

        assertEquals(
            SpanLimitsConfigModel(
                attributeCountLimit = 1,
                attributeValueLengthLimit = 2,
                linkCountLimit = 3,
                eventCountLimit = 4,
                attributeCountPerEventLimit = 5,
                attributeCountPerLinkLimit = 6,
            ),
            model,
        )
    }

    /**
     * Zero is a real limit, not an absence of one, so assigning it must be reported as declared.
     */
    @Test
    fun treatsZeroAsADeclaredValue() {
        val model = SpanLimitsConfigModelBuilder().apply { linkCountLimit = 0 }.toModel()

        assertEquals(SpanLimitsConfigModel(linkCountLimit = 0), model)
    }

    @Test
    fun assigningTheDefaultStillCountsAsDeclaringIt() {
        val model = SpanLimitsConfigModelBuilder().apply { eventCountLimit = 128 }.toModel()

        assertEquals(SpanLimitsConfigModel(eventCountLimit = 128), model)
    }

    @Test
    fun readsBackWhateverWasLastAssigned() {
        val builder = SpanLimitsConfigModelBuilder().apply {
            eventCountLimit = 7
            eventCountLimit = 9
        }

        assertEquals(9, builder.eventCountLimit)
        assertEquals(SpanLimitsConfigModel(eventCountLimit = 9), builder.toModel())
    }
}
