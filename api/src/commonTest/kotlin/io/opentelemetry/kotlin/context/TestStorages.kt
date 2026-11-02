package io.opentelemetry.kotlin.context

import io.opentelemetry.kotlin.ExperimentalApi

/**
 * Single-slot storage for :api tests, which can't depend on :context-single-slot.
 */
@OptIn(ExperimentalApi::class)
internal class SlotStorage : CurrentContextStorage {
    private var slot: Context? = null

    override fun current(): Context = slot ?: rootContext()

    override fun attach(context: Context): Scope {
        val previous = slot
        slot = context
        return object : Scope {
            override fun detach(): Boolean {
                slot = previous
                return true
            }
        }
    }
}

/**
 * Two-slot precedence storage: the high slot shadows the low slot. Contexts are routed to the
 * high slot when they carry [HIGH]. This is the layered-storage pattern documented in the README.
 */
@OptIn(ExperimentalApi::class)
internal class PrecedenceStorage : CurrentContextStorage {
    companion object {
        val HIGH: ContextKey<Boolean> = createContextKey("high")
    }

    val high = SlotStorage()
    val low = SlotStorage()

    override fun current(): Context {
        val top = high.current()
        return if (top !== rootContext()) {
            top
        } else {
            low.current()
        }
    }

    override fun attach(context: Context): Scope = when (context.get(HIGH)) {
        true -> high.attach(context)
        else -> low.attach(context)
    }
}
