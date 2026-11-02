# context-threadlocal

A `CurrentContextStorage` that confines the current context to the thread that attached it.
Opt in by depending on this module and passing its storage to the SDK:

```kotlin
createOpenTelemetry {
    context { storage = createThreadLocalCurrentContextStorage() }
}
```

Or install it yourself without an SDK:

```kotlin
installContextStorage(createThreadLocalCurrentContextStorage())
```

`context-coroutines`' `asCoroutineContextElement()` needs this storage, because it re-attaches
the context on every thread a coroutine resumes on. On JS, which is single-threaded, the storage
degrades to a single shared slot.
