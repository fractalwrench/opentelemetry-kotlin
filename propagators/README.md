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
backends. A propagator used with a no-op `Context` does nothing, so propagation stays switched off
when the SDK is switched off.

I've ported over W3C Baggage propagator only as an initial POC.
