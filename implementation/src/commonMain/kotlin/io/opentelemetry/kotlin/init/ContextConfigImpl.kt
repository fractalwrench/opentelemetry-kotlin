package io.opentelemetry.kotlin.init

import io.opentelemetry.kotlin.context.CurrentContextStorage

internal class ContextConfigImpl : ContextConfigDsl {
    override var storage: CurrentContextStorage? = null
}
