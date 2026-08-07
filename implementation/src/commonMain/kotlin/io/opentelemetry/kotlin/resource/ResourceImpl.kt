package io.opentelemetry.kotlin.resource

import io.opentelemetry.kotlin.attributes.AttributeContainer
import io.opentelemetry.kotlin.attributes.AttributesModel
import io.opentelemetry.kotlin.attributes.NO_ATTRIBUTE_LIMIT
import io.opentelemetry.kotlin.error.SdkError
import io.opentelemetry.kotlin.error.SdkErrorHandler
import io.opentelemetry.kotlin.error.SdkErrorSeverity

internal class ResourceImpl(
    container: AttributeContainer,
    override val schemaUrl: String?,
    private val sdkErrorHandler: SdkErrorHandler,
) : Resource {

    override val attributes: Map<String, Any> = container.attributes

    override fun asNewResource(action: MutableResource.() -> Unit): Resource {
        val impl = MutableResourceImpl(attributes.toMutableMap(), schemaUrl)
        impl.apply(action)
        val container = AttributesModel(attributeLimit = NO_ATTRIBUTE_LIMIT, attrs = impl.attributes)
        return ResourceImpl(container, impl.schemaUrl, sdkErrorHandler)
    }

    override fun merge(other: Resource): Resource {
        val mergedAttrs = (attributes + other.attributes).toMutableMap()
        val mergedSchema = mergeSchemaUrls(sdkErrorHandler, schemaUrl, other.schemaUrl)
        return ResourceImpl(
            AttributesModel(attributeLimit = NO_ATTRIBUTE_LIMIT, attrs = mergedAttrs),
            mergedSchema,
            sdkErrorHandler,
        )
    }
}

/**
 * Resolves the schema URL of a merged resource, as described in the OTel specification.
 *
 * Merging two resources whose schema URLs are both non-null and different is a merging error, so
 * this reports an [SdkError.ApiMisuse] and returns null.
 *
 * https://opentelemetry.io/docs/specs/otel/resource/sdk/#merge
 */
private fun mergeSchemaUrls(
    sdkErrorHandler: SdkErrorHandler,
    schemaUrl: String?,
    otherSchemaUrl: String?,
): String? {
    if (schemaUrl == null) {
        return otherSchemaUrl
    }
    if (otherSchemaUrl == null || schemaUrl == otherSchemaUrl) {
        return schemaUrl
    }
    sdkErrorHandler.onError(
        SdkError.ApiMisuse(
            "Resource.merge",
            "Cannot merge resources with conflicting schema URLs ($schemaUrl and " +
                "$otherSchemaUrl). The merged resource will have no schema URL.",
            SdkErrorSeverity.WARNING,
        )
    )
    return null
}
