# config-model

This module contains the intermediate configuration model that every configuration mechanism
targets: the programmatic DSL, environment variables, and declarative (YAML) configuration.

Every field in the model is nullable, where `null` means *unset*. Models can be merged using the
following precedence rules:

```
SDK defaults  <  (envars or declarative config file)  <  DSL
```

Envars are ignored if a declarative config file is present.

This module also holds the resolved counterparts of those models, where every value is non-null
because the specification defaults have been applied to whatever no mechanism configured. These
are what the SDKs actually apply.

https://opentelemetry.io/docs/specs/otel/configuration/
