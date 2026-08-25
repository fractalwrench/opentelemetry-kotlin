package io.opentelemetry.kotlin.propagation

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.context.Context
import kotlin.concurrent.Volatile

/**
 * Entry point for obtaining the propagator implementations that ship with this library.
 */
@ExperimentalApi
public object Propagators {

    @Volatile
    private var enabled: Boolean = true

    /**
     * Returns the propagator implementations that ship with this library.
     *
     * The returned propagators hold no reference to any SDK. They read and write whichever object
     * implementations belong to the [Context] they are given, so a single instance is safe to
     * share and works with every backend, including [io.opentelemetry.kotlin.NoopOpenTelemetry].
     * Instrumentation can therefore keep a trace or baggage alive across a remote call even when
     * the application never configured a propagator.
     *
     * An application that does not want that can switch it off during SDK initialization by
     * setting `instrumentationPropagation = false`, which disables every propagator returned here.
     * Propagators the application configured explicitly are unaffected.
     */
    public fun create(): PropagatorApi = GatedPropagatorApi

    /**
     * Enables or disables the propagators returned by [create], process-wide. Called by SDK
     * initialization; the last SDK to set it wins.
     */
    @InternalPropagatorApi
    public fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    /**
     * Returns the propagator implementations without the [setEnabled] gate applied, so that a
     * propagator the application configured explicitly always propagates.
     */
    @InternalPropagatorApi
    public fun unrestricted(): PropagatorApi = PropagatorApiImpl

    internal fun isEnabled(): Boolean = enabled
}

@OptIn(ExperimentalApi::class)
private object PropagatorApiImpl : PropagatorApi {
    override fun w3cBaggage(): TextMapPropagator = W3CBaggagePropagator
}

@OptIn(ExperimentalApi::class)
private object GatedPropagatorApi : PropagatorApi {
    private val w3cBaggage = GatedPropagator(W3CBaggagePropagator)
    override fun w3cBaggage(): TextMapPropagator = w3cBaggage
}

/**
 * Consults the switch on every call rather than at creation time, so that a propagator obtained
 * before the SDK was initialized still honours the application's choice.
 */
@OptIn(ExperimentalApi::class)
private class GatedPropagator(private val delegate: TextMapPropagator) : TextMapPropagator {

    override fun fields(): Collection<String> = when {
        Propagators.isEnabled() -> delegate.fields()
        else -> emptyList()
    }

    override fun <T> inject(context: Context, carrier: T?, setter: TextMapSetter<T>) {
        if (Propagators.isEnabled()) {
            delegate.inject(context, carrier, setter)
        }
    }

    override fun <T> extract(context: Context, carrier: T?, getter: TextMapGetter<T>): Context =
        when {
            Propagators.isEnabled() -> delegate.extract(context, carrier, getter)
            else -> context
        }
}
