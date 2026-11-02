package io.opentelemetry.kotlin.init

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.context.CurrentContextStorage

/**
 * Defines configuration for Context. This is ignored in compat mode, which always shares the
 * current context with opentelemetry-java.
 */
@ExperimentalApi
@ConfigDsl
public interface ContextConfigDsl {

    /**
     * The [CurrentContextStorage] the SDK installs at init, as the process-wide storage behind
     * [io.opentelemetry.kotlin.context.currentContext] and [io.opentelemetry.kotlin.context.Context.attach].
     *
     * Each mechanism ships in its own module, which provides a factory:
     * - `createSingleSlotCurrentContextStorage()` (`context-single-slot`): one slot shared by
     *   every thread. This is the default when [storage] is null.
     * - `createThreadLocalCurrentContextStorage()` (`context-threadlocal`): one slot per thread.
     * - `createOtelJavaCurrentContextStorage()` (`compat`): shares opentelemetry-java's storage.
     *
     * Any custom [CurrentContextStorage] also works, including one that layers the above.
     *
     * Only one storage is installed per process. If one is already installed when the SDK is
     * created (by another SDK, or by calling `installContextStorage()` directly), the SDK uses it
     * and reports a warning if [storage] was set.
     */
    public var storage: CurrentContextStorage?
}
