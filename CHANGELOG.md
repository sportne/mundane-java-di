# Changelog

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
