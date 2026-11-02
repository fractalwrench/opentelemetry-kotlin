package io.opentelemetry.kotlin.context

import io.opentelemetry.kotlin.ExperimentalApi

/**
 * Holds the process-wide [CurrentContextStorage]. The installed storage is kept alongside the
 * misuse-reporting decorator that wraps it, so that uninstalling can compare against the
 * caller's own instance.
 */
@OptIn(ExperimentalApi::class)
internal object ProcessWideContextStorageSlot {

    private class Installed(
        val original: CurrentContextStorage,
        val decorated: CurrentContextStorage,
    )

    private val slot = ContextStorageAtomicRef<Installed?>(null)

    fun installed(): CurrentContextStorage? = slot.get()?.decorated

    fun install(storage: CurrentContextStorage): Boolean {
        if (slot.get() != null) {
            return false
        }
        val entry = Installed(storage, MisuseReportingCurrentContextStorage(storage))
        return slot.compareAndSet(null, entry)
    }

    fun uninstall(storage: CurrentContextStorage): Boolean {
        val current = slot.get() ?: return false
        if (current.original !== storage) {
            return false
        }
        return slot.compareAndSet(current, null)
    }

    fun reset() {
        while (true) {
            val current = slot.get()
            if (slot.compareAndSet(current, null)) {
                return
            }
        }
    }
}
