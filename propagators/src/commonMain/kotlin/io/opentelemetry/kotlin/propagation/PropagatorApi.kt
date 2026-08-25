package io.opentelemetry.kotlin.propagation

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.ThreadSafe

/**
 * Provides access to the [TextMapPropagator] implementations that ship with this library.
 *
 * Instrumentation authors can obtain an instance from [Propagators] without depending on an SDK
 * implementation module, and without an [io.opentelemetry.kotlin.OpenTelemetry] instance. The
 * returned propagators operate on whichever object implementations belong to the
 * [io.opentelemetry.kotlin.context.Context] they are given, so one instance works with every
 * backend. A no-op Context propagates nothing.
 *
 * https://opentelemetry.io/docs/specs/otel/context/api-propagators/
 */
@ExperimentalApi
@ThreadSafe
public interface PropagatorApi {

    /**
     * Returns a [TextMapPropagator] that injects and extracts [io.opentelemetry.kotlin.baggage.Baggage]
     * via the W3C `baggage` HTTP header.
     *
     * https://www.w3.org/TR/baggage/
     */
    public fun w3cBaggage(): TextMapPropagator
}
