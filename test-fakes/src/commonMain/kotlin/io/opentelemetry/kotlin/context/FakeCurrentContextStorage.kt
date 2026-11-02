package io.opentelemetry.kotlin.context

/**
 * A single-slot [CurrentContextStorage] that records every attach and detach.
 */
class FakeCurrentContextStorage : CurrentContextStorage {

    var context: Context? = null
    val attached = mutableListOf<Context>()
    var detachCount = 0

    override fun current(): Context = context ?: rootContext()

    override fun attach(context: Context): Scope {
        val previous = this.context
        this.context = context
        attached += context
        return FakeScope {
            this.context = previous
            detachCount++
            true
        }
    }
}
