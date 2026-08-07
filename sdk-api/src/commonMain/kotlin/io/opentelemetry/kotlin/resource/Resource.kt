package io.opentelemetry.kotlin.resource

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.ThreadSafe
import io.opentelemetry.kotlin.attributes.AttributeContainer

/**
 * Implementations of this interface hold a 'resource' as described in the OTel specification.
 *
 * https://opentelemetry.io/docs/specs/otel/resource/data-model/
 */
@ExperimentalApi
@ThreadSafe
public interface Resource : AttributeContainer {

    /**
     * A schema URL for this resource, if available.
     */
    public val schemaUrl: String?

    /**
     * Creates a new resource using the current instance as a template.
     */
    public fun asNewResource(action: MutableResource.() -> Unit): Resource

    /**
     * Merges this resource with [other], returning a new [Resource].
     * Attributes on [other] take precedence in the event of a conflict when merging.
     *
     * The [schemaUrl] of the merged resource is whichever of the two is non-null, or the shared
     * value if both are equal. Merging resources whose schema URLs are both non-null and different
     * is a merging error, which the specification leaves undefined; opentelemetry-kotlin SDK
     * produces a resource with no [schemaUrl].
     *
     * https://opentelemetry.io/docs/specs/otel/resource/sdk/#merge
     */
    public fun merge(other: Resource): Resource
}
