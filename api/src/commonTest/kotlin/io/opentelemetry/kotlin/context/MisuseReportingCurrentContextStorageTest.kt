package io.opentelemetry.kotlin.context

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.error.RecordingSdkErrorHandler
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalApi::class, ProcessWideStateTestingApi::class)
internal class MisuseReportingCurrentContextStorageTest {

    private val key = createContextKey<String>("key")
    private lateinit var handler: RecordingSdkErrorHandler

    @BeforeTest
    fun setUp() {
        handler = RecordingSdkErrorHandler()
        handler.install()
    }

    @AfterTest
    fun tearDown() {
        resetContextStorageForTesting()
        handler.uninstall()
    }

    @Test
    fun testNestedInOrderReportsNothing() {
        installContextStorage(SlotStorage())
        val a = rootContext().set(key, "a")
        val b = a.set(key, "b")
        val scopeA = a.attach()
        val scopeB = b.attach()
        assertTrue(scopeB.detach())
        assertSame(a, currentContext())
        assertTrue(scopeA.detach())
        assertSame(rootContext(), currentContext())
        assertEquals(0, handler.errors.size)
    }

    @Test
    fun testOutOfOrderDetachIsReportedButStillDetaches() {
        installContextStorage(SlotStorage())
        val a = rootContext().set(key, "a")
        val b = a.set(key, "b")
        val scopeA = a.attach()
        b.attach()
        assertTrue(scopeA.detach())
        assertEquals(1, handler.misuses.size)
        assertEquals("Scope.detach", handler.misuses.single().api)
    }

    @Test
    fun testDoubleDetachIsReportedAndSkipped() {
        installContextStorage(SlotStorage())
        val scope = rootContext().set(key, "a").attach()
        assertTrue(scope.detach())
        assertFalse(scope.detach())
        assertEquals(1, handler.misuses.size)
    }

    @Test
    fun testSameContextAttachIsReportedButStillAttaches() {
        installContextStorage(SlotStorage())
        val a = rootContext().set(key, "a")
        val outer = a.attach()
        val inner = a.attach()
        assertEquals("Context.attach", handler.misuses.single().api)
        assertTrue(inner.detach())
        assertSame(a, currentContext())
        assertTrue(outer.detach())
    }

    @Test
    fun testThrowingStorageIsReportedAndFallsBack() {
        installContextStorage(object : CurrentContextStorage {
            override fun current(): Context = error("current")
            override fun attach(context: Context): Scope = error("attach")
        })
        assertSame(rootContext(), currentContext())
        assertTrue(rootContext().set(key, "a").attach().detach())
        assertTrue(handler.userCodeErrors.size >= 2)
    }

    @Test
    fun testThrowingScopeIsReported() {
        installContextStorage(object : CurrentContextStorage {
            override fun current(): Context = rootContext()
            override fun attach(context: Context): Scope = object : Scope {
                override fun detach(): Boolean = error("detach")
            }
        })
        assertFalse(rootContext().set(key, "a").attach().detach())
        assertEquals(1, handler.userCodeErrors.size)
    }

    @Test
    fun testLayeredStorageShadowedAttachReportsNothing() {
        val storage = PrecedenceStorage()
        installContextStorage(storage)
        val high = rootContext().set(PrecedenceStorage.HIGH, true).set(key, "high")
        val low = rootContext().set(key, "low")

        val highScope = high.attach()
        val lowScope = low.attach()
        assertSame(high, currentContext())
        assertSame(low, storage.low.current())

        assertTrue(lowScope.detach())
        assertSame(rootContext(), storage.low.current())
        assertTrue(highScope.detach())
        assertSame(rootContext(), currentContext())
        assertEquals(0, handler.errors.size)
    }

    @Test
    fun testLayeredStorageNestedAcrossSlots() {
        installContextStorage(PrecedenceStorage())
        val low = rootContext().set(key, "low")
        val high = rootContext().set(PrecedenceStorage.HIGH, true)

        val lowScope = low.attach()
        val highScope = high.attach()
        assertSame(high, currentContext())
        highScope.detach()
        assertSame(low, currentContext())
        lowScope.detach()
        assertEquals(0, handler.errors.size)
    }

    @Test
    fun testLayeredStorageOutOfOrderReportsOnce() {
        installContextStorage(PrecedenceStorage())
        val low = rootContext().set(key, "low")
        val high = rootContext().set(PrecedenceStorage.HIGH, true)

        val lowScope = low.attach()
        val highScope = high.attach()
        lowScope.detach()
        highScope.detach()
        assertEquals(1, handler.misuses.size)
    }
}
