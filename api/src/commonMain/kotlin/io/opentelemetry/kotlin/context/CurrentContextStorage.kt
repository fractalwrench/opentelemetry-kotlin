package io.opentelemetry.kotlin.context

import io.opentelemetry.kotlin.ExperimentalApi

/**
 * Stores the current [Context]. This is the mechanism behind [currentContext] and
 * [Context.attach], and is equivalent to opentelemetry-js's `ContextManager`.
 *
 * One storage is installed per process with [installContextStorage]. Until then,
 * [currentContext] returns [rootContext] and [Context.attach] does nothing. The SDK installs one
 * at init if none is installed yet, so most users only pick a storage in the SDK's
 * `context { storage = ... }` config.
 *
 * Implementations must follow this contract:
 * - [current] returns the [rootContext] instance when nothing is attached.
 * - The [Scope] returned by [attach] restores this storage's previous state when detached.
 * - [attach] is not required to make its argument the result of [current]. A layered storage, for
 *   example, may attach to a slot that a higher-precedence slot shadows.
 *
 * Implementations should not throw. If they do, the exception is reported to the SDK's
 * error handler and the call falls back to [rootContext] or a no-op [Scope].
 *
 * https://opentelemetry.io/docs/specs/otel/context/#optional-global-operations
 */
@ExperimentalApi
public interface CurrentContextStorage {

    /**
     * Returns the current context, or the [rootContext] instance if nothing is attached.
     */
    public fun current(): Context

    /**
     * Attaches [context] to this storage. Detaching the returned [Scope] restores the state this
     * storage had beforehand.
     */
    public fun attach(context: Context): Scope
}
