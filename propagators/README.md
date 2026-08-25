# propagators

This module contains `TextMapPropagator` implementations that are written against the `api` module
only, so they work with any `OpenTelemetry` instance regardless of whether compat/regular mode are used.

Instrumentation that needs to propagate context without depending on an SDK implementation module
can obtain them via `Propagators`:

```kotlin
val propagator = Propagators.create().w3cBaggage()
propagator.inject(context, headers, setter)
```

No SDK instance or object factory is needed: a propagator operates on whichever object
implementations belong to the `Context` it is given, so one instance is safe to share across
backends. `Context` and `Baggage` keep working when no SDK is installed (see `context-impl`), which
means instrumentation can keep a trace alive across a remote call even if the application never
configured a propagator. This matches opentelemetry-java, where `W3CBaggagePropagator.getInstance()`
works alongside `OpenTelemetry.noop()`.

An application that needs to guarantee no context leaves the process — for example where baggage
could carry data the user has not consented to sharing — can switch these propagators off during SDK
initialization:

```kotlin
createOpenTelemetry {
    instrumentationPropagation = false
}
```

That switch is process-wide, because the propagators it governs are obtained without an
`OpenTelemetry` instance. It does not affect propagators the application configured explicitly.

I've ported over W3C Baggage propagator only as an initial POC.
