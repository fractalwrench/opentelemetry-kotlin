package io.opentelemetry.kotlin.init

import io.opentelemetry.kotlin.config.model.DEFAULT_EVENT_COUNT_LIMIT
import io.opentelemetry.kotlin.config.model.DEFAULT_LINK_COUNT_LIMIT
import io.opentelemetry.kotlin.config.model.SpanLimits
import org.junit.Test
import kotlin.test.assertEquals

internal class OtelJavaSpanLimitsExtTest {

    @Test
    fun `test default`() {
        val impl = SpanLimits().toOtelJavaSpanLimits()

        assertEquals(DEFAULT_EVENT_COUNT_LIMIT, impl.maxNumberOfEvents)
        assertEquals(DEFAULT_ATTR_LIMIT, impl.maxNumberOfAttributes)
        assertEquals(DEFAULT_LINK_COUNT_LIMIT, impl.maxNumberOfLinks)
        assertEquals(DEFAULT_ATTR_LIMIT, impl.maxNumberOfAttributesPerLink)
        assertEquals(DEFAULT_ATTR_LIMIT, impl.maxNumberOfAttributesPerEvent)
        assertEquals(DEFAULT_ATTR_VALUE_LENGTH_LIMIT, impl.maxAttributeValueLength)
    }

    /**
     * Every limit must land on the matching field of the Java SDK's own type, since that is what
     * actually enforces them on exported spans.
     */
    @Test
    fun `test span limits`() {
        val impl = SpanLimits(
            eventCountLimit = 1,
            attributeCountLimit = 2,
            linkCountLimit = 3,
            attributeCountPerLinkLimit = 4,
            attributeCountPerEventLimit = 5,
            attributeValueLengthLimit = 6,
        ).toOtelJavaSpanLimits()

        assertEquals(1, impl.maxNumberOfEvents)
        assertEquals(2, impl.maxNumberOfAttributes)
        assertEquals(3, impl.maxNumberOfLinks)
        assertEquals(4, impl.maxNumberOfAttributesPerLink)
        assertEquals(5, impl.maxNumberOfAttributesPerEvent)
        assertEquals(6, impl.maxAttributeValueLength)
    }
}
