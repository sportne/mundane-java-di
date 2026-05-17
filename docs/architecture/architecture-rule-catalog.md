# Architecture rule catalog

This catalog records architecture rules that should be mechanically enforced with ArchUnit or
generated-source tests where practical. Rules apply to production main code unless an entry says
otherwise. Any new exception must be documented here or in an ADR in the same change.

## Project-specific rules

| Rule | Rationale | Enforced scope | Allowed exceptions | ArchUnit/test evidence |
|---|---|---|---|---|
| `modules/core` must not depend on `modules/generator`, `examples`, test support, ClassGraph, ArchUnit, JUnit, or third-party runtime packages. | The runtime library must remain small and native-image friendly. | `modules/core` main code. | None. | `CoreArchitectureTest`. |
| `modules/core` production code must stay in `io.github.mundanej.mjdi`. | The public runtime surface is intentionally one package. | `modules/core` main code. | None. | `CoreArchitectureTest`. |
| Runtime construction is provider-only and must not inspect `Inject` or `Named`. | Annotation processing belongs in build-time generator paths, not runtime lookup. | `modules/core` main code. | The annotation declarations themselves may reference annotation APIs. | `CoreArchitectureTest`. |
| `ServiceLoader` is only allowed in `BootstrapAppContext`. | Service-loaded modules are an explicit bootstrap feature, but not a general runtime discovery mechanism. | `modules/core` main code. | `BootstrapAppContext` only. | `CoreArchitectureTest`. |
| `Binder`, `AppContext`, `ContextProvider`, and generated modules must not use reflection, classpath scanning, or dynamic class loading. | Binding and lookup should remain explicit, deterministic, and easy for Native Image static analysis. | `modules/core` main code and generated module source. | None. | `CoreArchitectureTest` and `GeneratedModuleSourceArchitectureTest`. |
| Generator-only dependencies such as ClassGraph are allowed only in `modules/generator` implementation code. | Build-time discovery can use heavier tools without leaking into runtime artifacts. | `modules/core`, `modules/generator`, and `examples` main code. | `modules/generator` main code may depend on ClassGraph. | Module architecture tests. |
| Generated `AppPluginModule` source must not reference generator packages, ClassGraph, reflection APIs, service loading, class loaders, or test packages. | Generated code must be runtime-only source that can be compiled into Native Image applications without generator reachability. | Generated source emitted by `generator` tests. | None. | `GeneratedModuleSourceArchitectureTest`. |
| `examples` may depend on core, generator, JUnit, and ArchUnit, but core and generator must not depend on examples. | Examples demonstrate usage without becoming a production dependency direction. | `modules/core`, `modules/generator`, and `examples` main code. | None. | Module architecture tests. |

## GraalVM Native Image rules

| Rule | Rationale | Enforced scope | Allowed exceptions | ArchUnit/test evidence |
|---|---|---|---|---|
| Native-targeted code must not use reflection, `java.lang.invoke`, dynamic proxies, `Class.forName`, `ClassLoader`, or `URLClassLoader`. | These mechanisms commonly need reachability metadata and hide runtime discovery. | `modules/core` main code and generated module source. | `BootstrapAppContext` may use `ServiceLoader`. | `CoreArchitectureTest` and `GeneratedModuleSourceArchitectureTest`. |
| Native-targeted code must not use classpath-scanning libraries. | Classpath scanning is runtime discovery and does not fit closed-world static analysis. | `modules/core` main code and generated module source. | `modules/generator` main code may use ClassGraph at build time. | Module architecture tests. |
| Native-targeted code must not use Java serialization mechanisms. | Serialization needs native-image metadata and is not part of dependency injection behavior. | `modules/core` main code and generated module source. | None. | `CoreArchitectureTest` and `GeneratedModuleSourceArchitectureTest`. |
| Native-targeted code must not use JNI/native methods, internal JDK APIs, or Unsafe. | These APIs are brittle across JDKs and native-image configurations. | `modules/core` main code and generated module source. | None. | `CoreArchitectureTest` and source-token checks. |
| Resource access must not become runtime discovery behavior. | Native images require intentional resource inclusion. | `modules/core` main code and generated module source. | None. | `CoreArchitectureTest` and source-token checks. |
| Native-targeted code must not use dynamic scripting or compiler APIs. | Runtime compilation and script engines conflict with the library's closed-world goals. | `modules/core` main code and generated module source. | `modules/generator` may perform build-time source generation. | `CoreArchitectureTest` and source-token checks. |
| `modules/core` must not ship reachability-metadata workaround files. | The core API should work in Native Image without reflection/resource configuration. | `modules/core` resources. | Any future metadata file requires this catalog to explain why. | `CoreArchitectureTest`. |

## General Java baseline rules

| Rule | Rationale | Enforced scope | Allowed exceptions | ArchUnit/test evidence |
|---|---|---|---|---|
| No `System.exit` outside a CLI entrypoint. | Libraries and reusable tools should return errors instead of terminating hosts. | All production modules. | A future CLI main class may be listed here. | Module architecture tests. |
| No `Runtime`, `ProcessBuilder`, or forced `System.gc`. | Process execution and forced GC are portability and operational boundaries. | All production modules. | None. | Module architecture tests. |
| No finalizers. | Finalization is deprecated and nondeterministic. | All production modules. | None. | Module architecture tests. |
| No public static mutable fields. | Global mutable state makes behavior order-dependent. | All production modules. | None. | Module architecture tests. |
| No internal JDK APIs. | Internal APIs are unstable across Java releases. | All production modules. | None. | Module architecture tests. |
| No Java object serialization streams. | Serialization is a risky default and unnecessary for this library. | All production modules. | None. | Module architecture tests. |
| No global JVM mutation. | Libraries must not mutate process-wide state such as properties, streams, locale, or timezone. | All production modules. | None. | Module architecture tests. |
| No deprecated thread control APIs. | `Thread.stop`, `suspend`, and `resume` are unsafe process-wide controls. | All production modules. | None. | Module architecture tests. |
