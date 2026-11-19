# <img src="https://opentelemetry.io/img/logos/opentelemetry-logo-nav.png" alt="OpenTelemetry Icon" width="45" height=""> OpenTelemetry Kotlin
[![codecov](https://codecov.io/github/open-telemetry/opentelemetry-kotlin/branch/main/graph/badge.svg?token=GQJYEOUSAU)](https://codecov.io/github/open-telemetry/opentelemetry-kotlin)

An implementation of the [OpenTelemetry specification](https://opentelemetry.io/docs/specs/otel/) as a Kotlin
Multiplatform Library.

## Quickstart

Add the BOM, the SDK and the OTLP exporter:

```kotlin getting-started-install
dependencies {
    implementation(platform("io.opentelemetry.kotlin:bom:0.10.0"))
    implementation("io.opentelemetry.kotlin:implementation")
    implementation("io.opentelemetry.kotlin:exporters-otlp")
}
```

Create the SDK and record a span:

```kotlin readme-quickstart
suspend fun main() {
    val otel = createOpenTelemetry {
        serviceName = "my-service"
        tracerProvider {
            export {
                batchSpanProcessor(
                    otlpHttpSpanExporter {
                        endpoint = "http://localhost:4318" // override with your collector's URL
                    }
                )
            }
        }
    }
    otel.tracerProvider.getTracer("com.example").startSpan("hello").end()
    otel.shutdown() // flushes buffered spans before stopping
}
```

The APIs are marked `@ExperimentalApi` while the project is pre-1.0, so this compiles with a warning
until you [opt in](docs/getting-started.md#3-opt-in-to-experimental-apis).

## Documentation

See the [docs](docs/README.md) for guides on getting started, context, propagation and compat mode.

## Artifacts

All artifacts are published under the `io.opentelemetry.kotlin` group. Import the `bom` and omit the
version on the others.

| Purpose                 | Artifact                                                                                                                                                   |
|-------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| BOM                     | [`bom`](bom), or [`compat-bom`](compat-bom) when using `compat`                                                                                            |
| SDK entry point         | [`implementation`](implementation) or [`compat`](compat) (pick one)                                                                                        |
| API for library authors | [`api`](api), with [`noop`](noop) as a no-op implementation                                                                                                |
| Exporters               | [`exporters-otlp`](exporters-otlp), [`exporters-persistence`](exporters-persistence), [`exporters-in-memory`](exporters-in-memory) (tests)                 |
| Configuration           | [`config-declarative`](config-declarative) (YAML config files)                                                                                             |
| Context storage         | [`context-threadlocal`](context-threadlocal), [`context-single-slot`](context-single-slot), `context-coroutines`                                           |
| Propagators             | [`propagator-b3`](propagator-b3)                                                                                                                           |
| Sampling and processing | [`sampler-remote`](sampler-remote), [`span-event-bridge`](span-event-bridge)                                                                               |
| Instrumentation         | [`instrumentation-ktor-client`](instrumentation/instrumentation-ktor-client), [`instrumentation-ktor-server`](instrumentation/instrumentation-ktor-server) |
| Semantic conventions    | [`semconv`](semconv)                                                                                                                                       |
| Extensions              | `sdk-ext`                                                                                                                                                  |

Other published artifacts are internal. Don't depend on them directly.

### Supported targets

| Platform              | Tier | Tests run in CI | Example app | Production use | Notes                                            |
|-----------------------|------|-----------------|-------------|----------------|--------------------------------------------------|
| JVM                   | 1    | ✅              | ✅          | ✅             | Heavily tested and supported for production use. |
| Android (API 21+)     | 1    | ✅              | ✅          | ✅             | Heavily tested and supported for production use. |
| iOS                   | 2    | ✅              | ✅          | ❌             | Compiles.                                        |
| tvOS                  | 2    | ❌              | ❌          | ❌             | Compiles. Shares iOS's Apple implementation.     |
| JS                    | 2    | ✅              | ✅          | ❌             | Compiles.                                        |
| Linux (x64 and ARM64) | 2    | ✅              | ❌          | ❌             | Compiles. CI runs tests on x64 only.             |

Tier 1 targets are best-supported for production use. Tier 2 targets compile and pass shared tests, but
have received limited real-world testing. Support may be dropped for targets if their
maintenance burden becomes too great.

Interested in adding support for another platform?
See [adding a new target](CONTRIBUTING.md#adding-a-new-target).

### Example Apps

Example usage of the library can be found [here](examples).

## Feedback/bugs

Got feedback or found a bug? Please open a GitHub issue and we'll get back to you.

## Roadmap

Read our [roadmap here](ROADMAP.md).

# Contributing

We are currently resource constrained and are actively seeking new contributors interested in working towards [approver](https://github.com/open-telemetry/community/blob/main/guides/contributor/membership.md#approver) / [maintainer](https://github.com/open-telemetry/community/blob/main/guides/contributor/membership.md#maintainer) roles.
In addition to the documentation for approver / maintainer roles and the [contributing](./CONTRIBUTING.md) guide, here are some additional notes on engaging:

- [Pull request](https://github.com/open-telemetry/opentelemetry-kotlin/pulls) reviews are equally or more helpful than code contributions. Comments and approvals are valuable with or without a formal project role. They're also a great forcing function to explore a fairly complex codebase.
- Attending the Special Interest Group (SIG) is a great way to get to know community members and learn about project priorities. The SIG meets weekly on Mondays at 09:00 PT -- see the [OpenTelemetry community calendar](https://github.com/open-telemetry/community#implementation-sigs) for the Zoom link, and meeting notes are available in this [Google doc](https://docs.google.com/document/d/1dzYPuJTV5usXHKAFL596SRwSlATEj_Z37GPODB73Oms).
- Issues labeled [help wanted](https://github.com/open-telemetry/opentelemetry-kotlin/issues?q=is%3Aopen+is%3Aissue+label%3A%22help+wanted%22) are project priorities. Code contributions (or pull request reviews when a PR is linked) for these issues are particularly important.
- Triaging / responding to new issues and discussions is a great way to engage with the project.
- We are available in the [#otel-kotlin](https://cloud-native.slack.com/archives/C08NRCD4R4G) channel in the [CNCF Slack](https://slack.cncf.io/). Please join us there for further discussions.

The SIG meeting is open for all to join. We invite everyone to join our meeting, regardless of your experience level. Whether you're a seasoned OpenTelemetry contributor, just starting your journey, or simply curious about the work we do, you're more than welcome to participate!

## Community roles

Ongoing contributions are formally acknowledged via OpenTelemetry's [membership levels](https://github.com/open-telemetry/community/blob/main/guides/contributor/membership.md). Generally the maintainers will reach out and offer a specific role after seeing evidence of continued contributions that match a given role. However, do feel free to contact the maintainers if you think you already meet the requirements, or if you want guidance on doing so.

## Maintainers

- [Jamie Lynch](https://github.com/fractalwrench), Embrace
- [Jason Plumb](https://github.com/breedx-splk), Splunk

For more information about the maintainer role, see the [community repository](https://github.com/open-telemetry/community/blob/main/guides/contributor/membership.md#maintainer).

## Approvers

- [Hanson Ho](https://github.com/bidetofevil), Embrace
- [Masaki Sugimoto](https://github.com/Msksgm), Henry, Inc
- [Francisco Prieto](https://github.com/priettt), Canary Technologies

For more information about the approver role, see the [community repository](https://github.com/open-telemetry/community/blob/main/guides/contributor/membership.md#approver).

## Triagers
- [Juan Vega](https://github.com/jdvr), New Relic
- [Andrey Chernov](https://github.com/inetand)
- [Viorel Alexandrescu](https://github.com/viorel-alexandrescu)
- [Ilia Liferov](https://github.com/iliferov)
- [Rafael Nicolau](https://github.com/Rxfa) OLX

For more information about the triager role, see the [community repository](https://github.com/open-telemetry/community/blob/main/guides/contributor/membership.md#triager).

### Thanks to all of our contributors!

<a href="https://github.com/open-telemetry/opentelemetry-kotlin/graphs/contributors">
  <img alt="Repo contributors" src="https://contrib.rocks/image?repo=open-telemetry/opentelemetry-kotlin" />
</a>
