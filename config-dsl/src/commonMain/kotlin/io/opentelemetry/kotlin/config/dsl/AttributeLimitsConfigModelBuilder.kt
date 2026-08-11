package io.opentelemetry.kotlin.config.dsl

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.config.model.DEFAULT_ATTRIBUTE_COUNT_LIMIT
import io.opentelemetry.kotlin.config.model.DEFAULT_ATTRIBUTE_VALUE_LENGTH_LIMIT
import io.opentelemetry.kotlin.init.AttributeLimitsConfigDsl

/**
 * Records what the DSL declared about attribute limits.
 *
 * The DSL properties are non-null, so "the caller never mentioned this limit" cannot be expressed
 * through them. Each limit is therefore backed by a nullable field that stays `null` until
 * something assigns it: reads see the default, while [configuredAttributeCountLimit] and
 * [configuredAttributeValueLengthLimit] report what was actually declared, which is what the
 * config model needs so that a lower-precedence layer can still supply the limit.
 */
@ExperimentalApi
class AttributeLimitsConfigModelBuilder : AttributeLimitsConfigDsl {

    /**
     * The declared maximum attribute count, or `null` if the DSL did not set one.
     */
    var configuredAttributeCountLimit: Int? = null
        private set

    /**
     * The declared maximum attribute value length, or `null` if the DSL did not set one.
     */
    var configuredAttributeValueLengthLimit: Int? = null
        private set

    override var attributeCountLimit: Int
        get() = configuredAttributeCountLimit ?: DEFAULT_ATTRIBUTE_COUNT_LIMIT
        set(value) {
            configuredAttributeCountLimit = value
        }

    override var attributeValueLengthLimit: Int
        get() = configuredAttributeValueLengthLimit ?: DEFAULT_ATTRIBUTE_VALUE_LENGTH_LIMIT
        set(value) {
            configuredAttributeValueLengthLimit = value
        }
}
