package io.opentelemetry.kotlin.docs.snippets

import io.opentelemetry.kotlin.context.Context
import io.opentelemetry.kotlin.context.ContextKey
import io.opentelemetry.kotlin.context.CurrentContextStorage
import io.opentelemetry.kotlin.context.Scope
import io.opentelemetry.kotlin.context.createContextKey
import io.opentelemetry.kotlin.context.createSingleSlotCurrentContextStorage
import io.opentelemetry.kotlin.context.createThreadLocalCurrentContextStorage
import io.opentelemetry.kotlin.context.installContextStorage
import io.opentelemetry.kotlin.context.rootContext
import io.opentelemetry.kotlin.context.runWithContext
import io.opentelemetry.kotlin.tracing.Span

// region context-storage-layered
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

fun useLayeredStorage(span: Span) {
    installContextStorage(PrecedenceContextStorage())
    rootContext().inSlot(Slot.LOCAL).storeSpan(span).runWithContext { /* ... */ }
}
// end-region
