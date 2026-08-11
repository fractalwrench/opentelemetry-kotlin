package io.opentelemetry.kotlin.config.model

import io.opentelemetry.kotlin.ExperimentalApi

/**
 * Merges two optional nodes, where [higher] comes from the higher-precedence layer.
 *
 * A `null` node means the layer said nothing about it, so the other layer's node is used as-is.
 */
@ExperimentalApi
fun <T : ConfigModel<T>> mergeNode(lower: T?, higher: T?): T? = when {
    lower == null -> higher
    higher == null -> lower
    else -> lower.mergeWith(higher)
}
