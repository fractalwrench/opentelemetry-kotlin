package io.opentelemetry.kotlin.init

import io.opentelemetry.kotlin.Clock
import io.opentelemetry.kotlin.config.dsl.AttributeLimitsConfigModelBuilder
import io.opentelemetry.kotlin.error.SdkErrorHandler
import io.opentelemetry.kotlin.init.config.LogLimitConfig
import io.opentelemetry.kotlin.init.config.LoggingConfig
import io.opentelemetry.kotlin.logging.LoggerConfigImpl
import io.opentelemetry.kotlin.logging.LoggerConfigurator
import io.opentelemetry.kotlin.logging.export.LogRecordProcessor
import io.opentelemetry.kotlin.platformLog
import io.opentelemetry.kotlin.resource.Resource

internal class LoggerProviderConfigImpl(
    private val clock: Clock,
    private val sdkErrorHandler: SdkErrorHandler,
    private val resourceConfigImpl: ResourceConfigImpl = ResourceConfigImpl()
) : LoggerProviderConfigDsl, ResourceConfigDsl by resourceConfigImpl {

    private var processor: LogRecordProcessor? = null
    private var logLimitsAction: LogLimitsConfigDsl.() -> Unit = {}
    private val defaultLoggerConfig = LoggerConfigImpl(true)
    private var loggerConfigurator: LoggerConfigurator = LoggerConfigurator {
        defaultLoggerConfig
    }

    override fun export(action: LogExportConfigDsl.() -> LogRecordProcessor) {
        if (processor != null) {
            platformLog("export() should only be called once.")
            return
        }
        processor = LogExportConfigImpl(clock, sdkErrorHandler).action()
    }

    override fun logLimits(action: LogLimitsConfigDsl.() -> Unit) {
        logLimitsAction = action
    }

    override fun loggerConfigurator(configurator: LoggerConfigurator) {
        loggerConfigurator = configurator
    }

    fun generateLoggingConfig(
        base: Resource,
        globalLimits: AttributeLimitsConfigModelBuilder? = null
    ): LoggingConfig = LoggingConfig(
        processor = processor,
        logLimits = generateLogLimitsConfig(globalLimits),
        resource = base.merge(resourceConfigImpl.generateResource()),
        sdkErrorHandler = sdkErrorHandler,
        loggerConfigurator = loggerConfigurator,
    )

    private fun generateLogLimitsConfig(globalLimits: AttributeLimitsConfigModelBuilder?): LogLimitConfig {
        val impl = LogLimitsConfigImpl()
        globalLimits?.configuredAttributeCountLimit?.let { impl.attributeCountLimit = it }
        globalLimits?.configuredAttributeValueLengthLimit?.let { impl.attributeValueLengthLimit = it }
        logLimitsAction(impl)
        return LogLimitConfig(
            attributeCountLimit = impl.attributeCountLimit,
            attributeValueLengthLimit = impl.attributeValueLengthLimit,
        )
    }
}
