# api

This module contains the main public API of the SDK as prescribed by the OTel specification in the
[API section](https://opentelemetry.io/docs/specs/otel/overview/#api). This is intended for use by
instrumentation authors and application developers.

It is intended that library consumers will spend the vast majority of their time using symbols from
this module, with the exception of the entrypoints supplied in `compat`,
`implementation`, or  `noop`.

The intention of this module is to follow the OTel specification as closely as possible. Any
syntactic sugar that diverges from the spec should live in `api-ext` instead,
so that end-users can choose whether to use extensions to the base behavior.

## Guidelines for APIs

Please see the [contributing doc](../CONTRIBUTING.md) for design guidance on APIs. In short,
use interfaces to obscure concrete types & avoid default implementations as these tend to blend
separate concerns.

## Component Stability Matrix

Until the entire API can be marked stable, we are tracking the status of each
api component here. Components marked as "stable" may still change until the 1.0 release,
but we will take great pains to minimize and eliminate breaking changes before then.
Any component marked stable here should NOT have api surface marked with `@ExperimentalApi`.

| component   | development | stable |
|-------------|-------------|--------|
| attributes  |             | ✅     |
| baggage     | ⚠️          | ️      |
| context     | ⚠️          | ️      |
| factory     | ⚠️          | ️      |
| logging     | ⚠️          | ️      |
| metrics     | ⚠️          | ️      |
| propagation | ⚠️          | ️      |
| tracing     | ⚠️          | ️      |

## Context API

The Context API follows the [opentelemetry-js](https://opentelemetry.io/docs/languages/js/context/)
model. None of it needs an `OpenTelemetry` instance.

- `rootContext()` returns the root `Context`, which holds no values.
- `currentContext()` returns the current `Context` (JS: `context.active()`).
- `ctx.runWithContext { ... }` makes `ctx` current while the block runs, then restores the previous
  context, even if the block throws (JS: `context.with(ctx, fn)`).
- `ctx.attach()` returns a `Scope`; `scope.detach()` restores the previous context. Prefer
  `runWithContext`, which can't leak the scope.

### Where the current context lives

The current context is held by one process-wide `CurrentContextStorage` (JS: `ContextManager`).
**Nothing happens until a storage is installed**: `currentContext()` returns `rootContext()`, and
`attach()` does nothing. Explicit contexts, `set`/`get`, spans, baggage and propagators work
regardless.

The SDK installs a storage at init if none is installed yet, so most apps pick one in config:

```kotlin
createOpenTelemetry {
    context { storage = createThreadLocalCurrentContextStorage() }
}
```

Each mechanism ships in its own module:

| Module                | Factory                                   | Behaviour                             |
|-----------------------|-------------------------------------------|---------------------------------------|
| `context-single-slot` | `createSingleSlotCurrentContextStorage()` | One slot for every thread. Default.   |
| `context-threadlocal` | `createThreadLocalCurrentContextStorage()` | One slot per thread.                 |
| `compat`              | `createOtelJavaCurrentContextStorage()`   | Shares opentelemetry-java's storage.  |

You can also install a storage without an SDK (JS: `context.setGlobalContextManager()`):

```kotlin
installContextStorage(createThreadLocalCurrentContextStorage()) // false if one is already installed
```

Rules:

- The first install wins. An SDK created later uses the installed storage; if it was configured
  with a different one, it reports a WARNING to its error handler.
- `OpenTelemetrySdk.shutdown()` uninstalls the storage only if that SDK installed it.
- Every installed storage is wrapped so that scope misuse (detaching out of order or twice, attaching
  the context that's already current) and exceptions thrown by the storage are reported to the SDK's
  error handler. Misuse is only reported: the attach or detach still happens.

### Errors before an SDK exists

The API reports errors it catches (for example a throwing `createSpanContext { }` action, or a
throwing `TextMapGetter`) to a process-wide sink. Until an SDK installs its `SdkErrorHandler`, the
sink buffers up to 64 errors, then replays them in order to the first handler that is installed.

### Example usage

```kotlin
// Declare keys once and share the instance with whatever needs to read the value back.
// A key with a matching name is a different key - see the ContextKey docs.
private val USER_ID: ContextKey<String> = createContextKey("user.id")

internal fun exampleUsage(tracer: Tracer) {
    // 1. derive a new context from the current one. Nothing becomes current yet.
    val ctx = currentContext().set(USER_ID, "abc123")

    // 2. make it current while the block runs.
    ctx.runWithContext {
        // 3. tracers and loggers read currentContext() when no explicit parent is given.
        val span = tracer.startSpan("my_span")
        println(currentContext().get(USER_ID)) // abc123
        span.end()
    }
    // 4. the previous context is current again.
}
```

### Layered context storage

`CurrentContextStorage` is small enough to compose in user code. For example, a precedence storage
with any number of slots (here `hyper_local > local > global`) returns the first slot that isn't
empty, and routes each attach to a slot named by a `ContextKey` the context carries:

```kotlin
enum class Slot { HYPER_LOCAL, LOCAL, GLOBAL }

private val SLOT: ContextKey<Slot> = createContextKey("slot")

fun Context.inSlot(slot: Slot): Context = set(SLOT, slot)

class PrecedenceContextStorage : CurrentContextStorage {
    private val slots = mapOf(
        Slot.HYPER_LOCAL to createThreadLocalCurrentContextStorage(),
        Slot.LOCAL to createThreadLocalCurrentContextStorage(),
        Slot.GLOBAL to createSingleSlotCurrentContextStorage(),
    )

    override fun current(): Context =
        Slot.entries.map { slots.getValue(it).current() }.firstOrNull { it !== rootContext() } ?: rootContext()

    override fun attach(context: Context): Scope =
        slots.getValue(context.get(SLOT) ?: Slot.GLOBAL).attach(context)
}

installContextStorage(PrecedenceContextStorage())
rootContext().inSlot(Slot.LOCAL).storeSpan(span).runWithContext { /* ... */ }
```

This works because empty storages return the `rootContext()` instance, the factories return
independent instances, and the API never assumes that `attach(ctx)` makes `ctx` current. Note that:

- contexts derived from one in a slot keep its `SLOT` value, so they attach to the same slot;
- the whole context comes from one slot. `Context` can't list its keys, so there is no per-key
  fallthrough across slots (span and baggage can be combined by hand with `extractSpan()`/`extractBaggage()`).
