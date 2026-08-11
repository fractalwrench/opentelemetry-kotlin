package io.opentelemetry.kotlin.config.envar

/**
 * Reads [envVar] from the process environment, or returns `null` on targets that have no
 * environment to read.
 */
expect fun getEnvVarValue(envVar: String): String?
