package io.opentelemetry.kotlin.config.dsl

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.config.model.DEFAULT_ATTRIBUTE_COUNT_LIMIT
import io.opentelemetry.kotlin.config.model.DEFAULT_EVENT_COUNT_LIMIT
import io.opentelemetry.kotlin.config.model.DEFAULT_LINK_COUNT_LIMIT
import io.opentelemetry.kotlin.config.model.SpanLimitsConfigModel
import io.opentelemetry.kotlin.init.AttributeLimitsConfigDsl
import io.opentelemetry.kotlin.init.SpanLimitsConfigDsl

/**
 * Records what the DSL declared about span limits, so it can be contributed to the config model as
 * one configuration layer.
 *
 * Limits nobody assigned stay unset in [toModel], which is what lets a lower-precedence mechanism
 * such as an environment variable supply them instead. See
 * [AttributeLimitsConfigModelBuilder] for why the nullable backing fields are needed.
 */
@ExperimentalApi
class SpanLimitsConfigModelBuilder(
    private val attributeLimits: AttributeLimitsConfigModelBuilder = AttributeLimitsConfigModelBuilder(),
) : SpanLimitsConfigDsl, AttributeLimitsConfigDsl by attributeLimits {

    private var configuredLinkCountLimit: Int? = null
    private var configuredEventCountLimit: Int? = null
    private var configuredAttributeCountPerEventLimit: Int? = null
    private var configuredAttributeCountPerLinkLimit: Int? = null

    override var linkCountLimit: Int
        get() = configuredLinkCountLimit ?: DEFAULT_LINK_COUNT_LIMIT
        set(value) {
            configuredLinkCountLimit = value
        }

    override var eventCountLimit: Int
        get() = configuredEventCountLimit ?: DEFAULT_EVENT_COUNT_LIMIT
        set(value) {
            configuredEventCountLimit = value
        }

    override var attributeCountPerEventLimit: Int
        get() = configuredAttributeCountPerEventLimit ?: DEFAULT_ATTRIBUTE_COUNT_LIMIT
        set(value) {
            configuredAttributeCountPerEventLimit = value
        }

    override var attributeCountPerLinkLimit: Int
        get() = configuredAttributeCountPerLinkLimit ?: DEFAULT_ATTRIBUTE_COUNT_LIMIT
        set(value) {
            configuredAttributeCountPerLinkLimit = value
        }

    /**
     * Returns what the DSL declared, or `null` if it declared nothing at all.
     */
    fun toModel(): SpanLimitsConfigModel? {
        val model = SpanLimitsConfigModel(
            attributeCountLimit = attributeLimits.configuredAttributeCountLimit,
            attributeValueLengthLimit = attributeLimits.configuredAttributeValueLengthLimit,
            linkCountLimit = configuredLinkCountLimit,
            eventCountLimit = configuredEventCountLimit,
            attributeCountPerEventLimit = configuredAttributeCountPerEventLimit,
            attributeCountPerLinkLimit = configuredAttributeCountPerLinkLimit,
        )
        return model.takeIf { it != SpanLimitsConfigModel() }
    }
}
