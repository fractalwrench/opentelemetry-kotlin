package io.opentelemetry.kotlin.context

import io.opentelemetry.kotlin.ExperimentalApi
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalApi::class)
internal class SingleSlotCurrentContextStorageTest {

    private val key = createContextKey<String>("key")

    @Test
    fun testEmptyStorageReturnsRoot() {
        assertSame(rootContext(), createSingleSlotCurrentContextStorage().current())
    }

    @Test
    fun testAttachAndRestore() {
        val storage = createSingleSlotCurrentContextStorage()
        val a = rootContext().set(key, "a")
        val b = a.set(key, "b")
        val scopeA = storage.attach(a)
        val scopeB = storage.attach(b)
        assertSame(b, storage.current())
        assertTrue(scopeB.detach())
        assertSame(a, storage.current())
        assertTrue(scopeA.detach())
        assertSame(rootContext(), storage.current())
    }

    @Test
    fun testInstancesAreIndependent() {
        val first = createSingleSlotCurrentContextStorage()
        val second = createSingleSlotCurrentContextStorage()
        first.attach(rootContext().set(key, "a"))
        assertSame(rootContext(), second.current())
    }
}
