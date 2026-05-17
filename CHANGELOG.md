# Changelog

## 1.0.0 - 2026-05-17

Stable release of `mundane-java-di`.

Breaking changes from `0.1.0`:

- Raised the Java baseline to Java 21.
- Rejected null instance bindings and null provider results from `AppContext` lookups.
- Made binder installs transactional so failed module configuration no longer leaves partial
  bindings behind.
- Added dependency-cycle diagnostics for recursive provider lookups.

Runtime and generator changes:

- Hardened core runtime behavior for nulls, cycles, failed module installs, and concurrent
  singleton access.
- Clarified thread-safety and service-loader trust-boundary documentation.
- Improved unnamed class lookup and key hash-code behavior without changing the public API shape.
- Added generator CLI support for `--scan-path`, `--help`, and `--version`.
- Kept generated modules singleton by default and direct-constructor based.
- Added generated-source quality checks for warnings-as-errors compilation, Javadocs, Checkstyle,
  Google Java Format, architecture rules, and runtime singleton behavior.
- Adopted the project-family Java quality conventions from `mundane-java-orb`.

## 0.1.0 - 2026-05-17

Initial releasable version of `mundane-java-di`.

- Added a dependency-free core runtime with `Binder`, `AppContext`, `Key`, provider bindings,
  singleton bindings, instance bindings, strict install behavior, and explicit override installs.
- Added `@Inject` and `@Named` metadata annotations for build-time generation and architecture
  tests.
- Added named scalar lookup helpers on `AppContext`.
- Added a JDK-only build-time generator that scans the current Java classpath and emits
  singleton-by-default `AppPluginModule` source.
- Added architecture-rule documentation and ArchUnit checks for project-specific, GraalVM
  native-image, and baseline Java constraints.
- Added JVM tests, coverage gates, Javadocs, GitHub CI workflows, and GraalVM native smoke tests.
- Added a basic example project that demonstrates manual module usage and generated module checks.
