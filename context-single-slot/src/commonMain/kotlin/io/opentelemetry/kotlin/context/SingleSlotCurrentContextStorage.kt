package io.opentelemetry.kotlin.context

import io.opentelemetry.kotlin.ExperimentalApi
import kotlin.concurrent.Volatile

/**
 * Returns a new [CurrentContextStorage] that holds one current context shared by every thread and
 * coroutine. This is the SDK's default mechanism.
 *
 * Every call returns a new, independent instance.
 */
@ExperimentalApi
public fun createSingleSlotCurrentContextStorage(): CurrentContextStorage = SingleSlotCurrentContextStorage()

@OptIn(ExperimentalApi::class)
internal class SingleSlotCurrentContextStorage : CurrentContextStorage {

    @Volatile
    private var current: Context? = null

    override fun attach(context: Context): Scope {
        val previous = current
        current = context
        return object : Scope {
            override fun detach(): Boolean {
                current = previous
                return true
            }
        }
    }

    override fun current(): Context = current ?: rootContext()
}
