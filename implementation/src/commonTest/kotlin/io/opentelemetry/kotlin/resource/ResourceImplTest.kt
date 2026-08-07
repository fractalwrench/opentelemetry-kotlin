package io.opentelemetry.kotlin.resource

import io.opentelemetry.kotlin.attributes.AttributesModel
import io.opentelemetry.kotlin.attributes.DEFAULT_ATTRIBUTE_LIMIT
import io.opentelemetry.kotlin.error.FakeSdkErrorHandler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class ResourceImplTest {

    private val errorHandler = FakeSdkErrorHandler()

    private fun resource(schemaUrl: String?, vararg attrs: Pair<String, Any>) =
        ResourceImpl(AttributesModel(attrs = mutableMapOf(*attrs)), schemaUrl, errorHandler)

    @Test
    fun testResourceImpl() {
        val resource = resource("https://example.com/schema", "key" to "value")
        val another = resource.asNewResource {
            attributes["foo"] = "value"
            schemaUrl = "https://example.com/another"
        }

        assertEquals("value", resource.attributes["key"])
        assertEquals("value", another.attributes["key"])

        assertNull(resource.attributes["foo"])
        assertEquals("value", another.attributes["foo"])

        assertEquals("https://example.com/schema", resource.schemaUrl)
        assertEquals("https://example.com/another", another.schemaUrl)
    }

    @Test
    fun testEmptyResource() {
        val resource = resource("https://example.com/schema", "key" to "value")
        val another = resource.asNewResource {
            attributes.clear()
            schemaUrl = null
        }
        assertEquals(emptyMap(), another.attributes)
        assertNull(another.schemaUrl)
    }

    @Test
    fun testDefensiveCopy() {
        val resource = resource(null)
        lateinit var attrs: MutableMap<String, Any>
        val another = resource.asNewResource {
            attrs = attributes
        }
        attrs["key"] = "value"
        assertEquals(emptyMap(), another.attributes)
    }

    @Test
    fun testNewResourceNoAttributeLimit() {
        val count = DEFAULT_ATTRIBUTE_LIMIT + 3
        val attrs = (0 until count).map { "key$it" to "value$it" as Any }
        val resource = resource("https://example.com/schema", *attrs.toTypedArray())
        assertEquals(count, resource.attributes.size)
    }

    @Test
    fun testMutateResourceNoAttributeLimit() {
        val count = DEFAULT_ATTRIBUTE_LIMIT + 3
        val attrs = (0 until count).map { "key$it" to "value$it" as Any }
        val resource = resource("https://example.com/schema", *attrs.toTypedArray())
        val mutated = resource.asNewResource {
            attributes["extraKey"] = "extraValue"
        }
        assertEquals(count + 1, mutated.attributes.size)
        assertEquals("extraValue", mutated.attributes["extraKey"])
    }

    @Test
    fun testMergeNonOverlapping() {
        val base = resource("https://example.com/schema", "a" to "1")
        val other = resource("https://example.com/schema", "b" to "2")
        val merged = base.merge(other)
        assertEquals("1", merged.attributes["a"])
        assertEquals("2", merged.attributes["b"])
        assertFalse(errorHandler.hasErrors())
    }

    @Test
    fun testMergeOtherWinsOnConflict() {
        val base = resource(null, "key" to "base")
        val other = resource(null, "key" to "other")
        val merged = base.merge(other)
        assertEquals("other", merged.attributes["key"])
        assertFalse(errorHandler.hasErrors())
    }

    @Test
    fun testMergeSchemaUrlBothNull() {
        assertNull(resource(null).merge(resource(null)).schemaUrl)
        assertFalse(errorHandler.hasErrors())
    }

    @Test
    fun testMergeSchemaUrlBaseNull() {
        val merged = resource(null).merge(resource("https://example.com/other"))
        assertEquals("https://example.com/other", merged.schemaUrl)
        assertFalse(errorHandler.hasErrors())
    }

    @Test
    fun testMergeSchemaUrlOtherNull() {
        val merged = resource("https://example.com/base").merge(resource(null))
        assertEquals("https://example.com/base", merged.schemaUrl)
        assertFalse(errorHandler.hasErrors())
    }

    @Test
    fun testMergeSchemaUrlSame() {
        val merged = resource("https://example.com/schema").merge(resource("https://example.com/schema"))
        assertEquals("https://example.com/schema", merged.schemaUrl)
        assertFalse(errorHandler.hasErrors())
    }

    @Test
    fun testMergeConflictingSchemaUrlsDropsSchemaAndReportsError() {
        val base = resource("https://example.com/base", "a" to "1")
        val other = resource("https://example.com/other", "b" to "2")
        val merged = base.merge(other)

        assertNull(merged.schemaUrl)
        assertEquals("1", merged.attributes["a"])
        assertEquals("2", merged.attributes["b"])

        val error = errorHandler.apiMisuses.single()
        assertEquals("Resource.merge", error.api)
        assertTrue(error.message.contains("https://example.com/base"))
        assertTrue(error.message.contains("https://example.com/other"))
    }

    @Test
    fun testMergeNoAttributeLimit() {
        val baseAttrs = (0 until DEFAULT_ATTRIBUTE_LIMIT).map { "base$it" to "v$it" as Any }
        val base = resource(null, *baseAttrs.toTypedArray())
        val other = resource(null, "extra" to "value")
        val merged = base.merge(other)
        assertEquals(DEFAULT_ATTRIBUTE_LIMIT + 1, merged.attributes.size)
        assertEquals("value", merged.attributes["extra"])
    }
}
