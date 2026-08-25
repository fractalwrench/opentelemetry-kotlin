package io.opentelemetry.kotlin.propagation

import io.opentelemetry.kotlin.ExperimentalApi

/**
 * Entry point for obtaining the propagator implementations that ship with this library.
 */
@ExperimentalApi
public object Propagators {

    /**
     * Returns the propagator implementations that ship with this library.
     *
     * The returned propagators hold no reference to any SDK. They read and write whichever object
     * implementations belong to the [io.opentelemetry.kotlin.context.Context] they are given, so a
     * single instance is safe to share and works with every backend. A propagator used with a
     * no-op Context does nothing, so propagation stays switched off when the SDK is switched off.
     */
    public fun create(): PropagatorApi = PropagatorApiImpl
}

@OptIn(ExperimentalApi::class)
private object PropagatorApiImpl : PropagatorApi {
    override fun w3cBaggage(): TextMapPropagator = W3CBaggagePropagator
}
