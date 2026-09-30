package io.opentelemetry.kotlin.propagation

import io.opentelemetry.kotlin.ExperimentalApi

/**
 * Creates a [TextMapPropagator] that sequentially delegates to each of [propagators], in the
 * order supplied. Its fields are the union of the fields of each propagator.
 *
 * This does not require an [io.opentelemetry.kotlin.OpenTelemetry] instance.
 *
 * https://opentelemetry.io/docs/specs/otel/context/api-propagators/#composite-propagator
 */
@Suppress("UnusedParameter") // POC: signature only
@ExperimentalApi
public fun createCompositePropagator(vararg propagators: TextMapPropagator): TextMapPropagator = TODO()

/**
 * Creates a [TextMapPropagator] that injects and extracts the current span context
 * via the W3C `traceparent` and `tracestate` HTTP headers.
 *
 * This does not require an [io.opentelemetry.kotlin.OpenTelemetry] instance.
 *
 * https://www.w3.org/TR/trace-context/
 */
@ExperimentalApi
public fun createW3CTraceContextPropagator(): TextMapPropagator = TODO()

/**
 * Creates a [TextMapPropagator] that injects and extracts [io.opentelemetry.kotlin.baggage.Baggage]
 * via the W3C `baggage` HTTP header.
 *
 * This does not require an [io.opentelemetry.kotlin.OpenTelemetry] instance.
 *
 * https://www.w3.org/TR/baggage/
 */
@ExperimentalApi
public fun createW3CBaggagePropagator(): TextMapPropagator = TODO()
