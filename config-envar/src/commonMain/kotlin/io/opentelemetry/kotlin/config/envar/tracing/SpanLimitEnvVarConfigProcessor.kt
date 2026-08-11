package io.opentelemetry.kotlin.config.envar.tracing

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.config.envar.processor.EnvVarConfigProcessor
import io.opentelemetry.kotlin.config.model.SpanLimitsConfigModel

@ExperimentalApi
abstract class SpanLimitEnvVarConfigProcessor : EnvVarConfigProcessor<SpanLimitsConfigModel, Int>()
