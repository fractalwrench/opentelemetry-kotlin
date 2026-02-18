package io.opentelemetry.kotlin.logging

import io.opentelemetry.kotlin.Clock
import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.InstrumentationScopeInfo
import io.opentelemetry.kotlin.attributes.MutableAttributeContainer
import io.opentelemetry.kotlin.context.Context
import io.opentelemetry.kotlin.factory.SdkFactory
import io.opentelemetry.kotlin.init.config.LogLimitConfig
import io.opentelemetry.kotlin.logging.export.LogRecordProcessor
import io.opentelemetry.kotlin.logging.model.LogRecordModel
import io.opentelemetry.kotlin.logging.model.ReadWriteLogRecordImpl
import io.opentelemetry.kotlin.logging.model.SeverityNumber
import io.opentelemetry.kotlin.resource.Resource

@OptIn(ExperimentalApi::class)
internal class LoggerImpl(
    private val clock: Clock,
    private val processor: LogRecordProcessor?,
    sdkFactory: SdkFactory,
    private val key: InstrumentationScopeInfo,
    private val resource: Resource,
    private val logLimitConfig: LogLimitConfig,
) : Logger {

    private val contextFactory = sdkFactory.contextFactory
    private val root = contextFactory.root()
    private val invalidSpanContext = sdkFactory.spanContextFactory.invalid
    private val spanFactory = sdkFactory.spanFactory

    override fun enabled(
        context: Context?,
        severityNumber: SeverityNumber?,
        eventName: String?,
    ): Boolean {
        if (processor == null) {
            return false
        }
        val ctx = context ?: contextFactory.implicitContext()
        return processor.enabled(ctx, key, severityNumber, eventName)
    }

    @Deprecated(
        "Deprecated",
        replaceWith = ReplaceWith("emit(body, null, timestamp, observedTimestamp, context, severityNumber, severityText, attributes)")
    )
    override fun log(
        body: String?,
        timestamp: Long?,
        observedTimestamp: Long?,
        context: Context?,
        severityNumber: SeverityNumber?,
        severityText: String?,
        attributes: (MutableAttributeContainer.() -> Unit)?
    ) {
        emit(
            body = body,
            timestamp = timestamp,
            observedTimestamp = observedTimestamp,
            context = context,
            severityNumber = severityNumber,
            severityText = severityText,
            attributes = attributes
        )
    }

    @Deprecated(
        "Deprecated",
        replaceWith = ReplaceWith("emit(body, eventName, timestamp, observedTimestamp, context, severityNumber, severityText, attributes)")
    )
    override fun logEvent(
        eventName: String,
        body: String?,
        timestamp: Long?,
        observedTimestamp: Long?,
        context: Context?,
        severityNumber: SeverityNumber?,
        severityText: String?,
        attributes: (MutableAttributeContainer.() -> Unit)?
    ) {
        emit(
            body = body,
            eventName = eventName,
            timestamp = timestamp,
            observedTimestamp = observedTimestamp,
            context = context,
            severityNumber = severityNumber,
            severityText = severityText,
            attributes = attributes
        )
    }

    override fun emit(
        body: String?,
        eventName: String?,
        timestamp: Long?,
        observedTimestamp: Long?,
        context: Context?,
        severityNumber: SeverityNumber?,
        severityText: String?,
        attributes: (MutableAttributeContainer.() -> Unit)?
    ) {
        val ctx = context ?: contextFactory.implicitContext()
        val spanContext = when (ctx) {
            root -> invalidSpanContext
            else -> spanFactory.fromContext(ctx).spanContext
        }
        val now = clock.now()
        val log = LogRecordModel(
            resource = resource,
            instrumentationScopeInfo = key,
            timestamp = timestamp ?: now,
            observedTimestamp = observedTimestamp ?: now,
            body = body,
            severityText = severityText,
            severityNumber = severityNumber ?: SeverityNumber.UNKNOWN,
            spanContext = spanContext,
            logLimitConfig = logLimitConfig,
            eventName = eventName,
        )
        if (attributes != null) {
            attributes(log)
        }
        processor?.onEmit(ReadWriteLogRecordImpl(log), ctx)
    }
}
