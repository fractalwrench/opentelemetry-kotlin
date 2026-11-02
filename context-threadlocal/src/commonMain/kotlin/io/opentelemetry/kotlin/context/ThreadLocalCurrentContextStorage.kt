package io.opentelemetry.kotlin.context

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.ThreadLocal

/**
 * Returns a new [CurrentContextStorage] that confines the current context to the thread that
 * attached it. On single-threaded targets (JS) it degrades to a single shared slot.
 *
 * Every call returns a new, independent instance: two storages never share a thread-local slot.
 */
@ExperimentalApi
public fun createThreadLocalCurrentContextStorage(): CurrentContextStorage = ThreadLocalCurrentContextStorage()

@OptIn(ExperimentalApi::class)
internal class ThreadLocalCurrentContextStorage : CurrentContextStorage {

    private val current = ThreadLocal<Context>()

    override fun attach(context: Context): Scope {
        val previous = current.get()
        current.set(context)
        return object : Scope {
            override fun detach(): Boolean {
                current.set(previous)
                return true
            }
        }
    }

    override fun current(): Context = current.get() ?: rootContext()
}
