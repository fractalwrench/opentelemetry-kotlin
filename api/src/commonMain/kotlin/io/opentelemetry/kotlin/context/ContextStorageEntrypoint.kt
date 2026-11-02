package io.opentelemetry.kotlin.context

import io.opentelemetry.kotlin.ExperimentalApi

/**
 * Installs [storage] as the process-wide [CurrentContextStorage], which backs [currentContext]
 * and [Context.attach]. This is equivalent to opentelemetry-js's `context.setGlobalContextManager`.
 *
 * Returns false, and changes nothing, if a storage is already installed. The SDK installs its
 * configured storage at init when none is installed, so call this before creating the SDK if you
 * want your storage to win.
 *
 * The installed storage is wrapped so that scope misuse (detaching out of order, detaching twice)
 * and exceptions thrown by [storage] are reported to the SDK's error handler instead of escaping.
 */
@ExperimentalApi
public fun installContextStorage(storage: CurrentContextStorage): Boolean =
    ProcessWideContextStorageSlot.install(storage)

/**
 * Uninstalls [storage] if it is the installed process-wide [CurrentContextStorage] (compared by
 * identity). Returns false, and changes nothing, otherwise. Afterwards [currentContext] returns
 * [rootContext] and [Context.attach] does nothing until another storage is installed.
 */
@ExperimentalApi
public fun uninstallContextStorage(storage: CurrentContextStorage): Boolean =
    ProcessWideContextStorageSlot.uninstall(storage)

/**
 * Returns the current [Context] from the installed [CurrentContextStorage], or [rootContext] if
 * none is installed. This is equivalent to opentelemetry-js's `context.active()`.
 *
 * This does not require an [io.opentelemetry.kotlin.OpenTelemetry] instance.
 * https://opentelemetry.io/docs/specs/otel/context/#get-current-context
 */
@ExperimentalApi
public fun currentContext(): Context = ProcessWideContextStorageSlot.installed()?.current() ?: rootContext()

/**
 * Makes this [Context] current while [block] runs, then restores the previous context, even if
 * [block] throws. This is equivalent to opentelemetry-js's `context.with(ctx, fn)`.
 *
 * Like [Context.attach], this does nothing to the current context if no [CurrentContextStorage]
 * is installed; [block] still runs.
 */
@ExperimentalApi
public inline fun <T> Context.runWithContext(block: () -> T): T {
    val scope = attach()
    try {
        return block()
    } finally {
        scope.detach()
    }
}

/**
 * Uninstalls any process-wide [CurrentContextStorage]. Call it in `@AfterTest`, never in a
 * `@BeforeTest` that runs alongside another `@BeforeTest` that creates an SDK.
 */
@ProcessWideStateTestingApi
public fun resetContextStorageForTesting() {
    ProcessWideContextStorageSlot.reset()
}

@OptIn(ExperimentalApi::class)
internal fun installedContextStorage(): CurrentContextStorage? = ProcessWideContextStorageSlot.installed()
