# mundane-java-di

`mundane-java-di` is a small Java dependency injection library for projects that care about
GraalVM Native Image compatibility. It provides a Guice-like binding experience without runtime
reflection, runtime classpath scanning, dynamic proxies, or external runtime dependencies.

The design is intentionally plain and explicit:

- application code registers providers in modules;
- the runtime looks up objects from an `AppContext`;
- optional generated modules call constructors directly;
- generated bindings are singleton by default;
- architecture tests keep native-image-hostile APIs out of runtime code.

## Status

Version `1.0.0` is the first stable release. The core runtime has no runtime dependencies, and the
generator depends only on the core module plus the JDK. Maven Central publishing is not set up;
release artifacts are built locally from this repository.

Artifact coordinates:

- `io.github.mundanej:mundane-java-di:1.0.0`
- `io.github.mundanej:mundane-java-di-generator:1.0.0`

## Requirements

- Java 21 or newer for normal JVM builds and generated-source quality checks.
- GraalVM with `native-image` for the native smoke lane.
- Gradle wrapper from this repository.

## Modules

- `modules/core` contains the runtime API.
- `modules/generator` contains JDK-only build-time source generation support.
- `examples/basic` contains a tiny example module and usage test.
- `examples/generator-showcase` demonstrates generator Gradle tasks, dry runs, and programmatic
  generator API usage.
- `docs/architecture/architecture-rule-catalog.md` documents the rules enforced by ArchUnit.
- `build-logic` contains small Gradle convention plugins shared by the modules.

## Manual Dependency Injection

The core runtime is provider-based. A module receives a `Binder` and registers bindings:

```java
import io.github.mundanej.mjdi.AppPluginModule;
import io.github.mundanej.mjdi.Binder;

public final class AppModule implements AppPluginModule {
    @Override
    public void configure(Binder binder) {
        binder.bindSingleton(Repository.class, context -> new Repository());
        binder.bindSingleton(Service.class, context -> new Service(context.get(Repository.class)));
    }
}
```

Create an `AppContext` from modules and request objects from it:

```java
import io.github.mundanej.mjdi.AppContext;
import io.github.mundanej.mjdi.BootstrapAppContext;
import java.util.List;

AppContext context = BootstrapAppContext.create(List.of(new AppModule()));
Service service = context.get(Service.class);
```

Normal `install(...)` calls are strict: binding the same key twice throws an exception. Use
`installOverride(...)` only when a later module should intentionally replace an earlier binding. A
failed install is rolled back, so partially configured module bindings are not kept.

```java
import io.github.mundanej.mjdi.Binder;

AppContext context = new Binder()
        .install(new AppModule())
        .installOverride(new TestOverrideModule())
        .build();
```

Named scalar values can be looked up with convenience methods:

```java
String mode = context.getNamedString("mode");
int port = context.getNamedInt("port");
boolean enabled = context.getNamedBool("enabled");
```

Bindings are expected to return non-null values. Use `Optional<T>` or a domain value when a
dependency needs to represent absence.

## Generated Modules

`@Inject` and `@Named` are metadata for generator and architecture tests. The runtime core does not
scan annotations or use reflection to create objects.

```java
import io.github.mundanej.mjdi.Inject;
import io.github.mundanej.mjdi.Named;

public final class Service {
    @Inject
    public Service(Repository repository, @Named("mode") String mode) {
        // ...
    }
}
```

The generator scans compiled classes at build time and writes an `AppPluginModule` source file. By
default it scans the Java process classpath. You can also pass explicit classpath directories or jars
with `--scan-path`, which is easier to use from build scripts:

```bash
java -cp "app-classes:mundane-java-di.jar:mundane-java-di-generator.jar" \
  io.github.mundanej.mjdi.generator.GeneratorCli \
  --output-dir build/generated/sources/mjdi \
  --module-package com.example.generated \
  --module-class GeneratedAppModule \
  --package-root com.example \
  --scan-path build/classes/java/main
```

Use `--dry-run` to print generated source without writing a file, `--overwrite` when replacing an
existing generated module with different content, `--help` for CLI usage, and `--version` for the
generator implementation version.

Generated source is intended to be checked like normal project source. It includes public Javadocs,
compiles with warnings as errors, passes the same Checkstyle and Google Java Format conventions used
by this repository, uses direct constructor calls, defaults to singleton bindings, and stays free of
runtime reflection, classpath scanning, service loading, and serialization.

Generated output looks like ordinary module code:

```java
binder.bindSingleton(Service.class,
        context -> new Service(context.get(Repository.class), context.getNamedString("mode")));
```

## Build And Verification

Run the normal local gate:

```bash
./gradlew qualityGate
```

The local gate includes tests, ArchUnit rules, JaCoCo coverage verification, Javadocs, Checkstyle,
SpotBugs, Error Prone compilation, and Spotless formatting checks.

Run all JVM checks:

```bash
./gradlew checkAll
```

Run the native-image smoke lane when GraalVM `native-image` is available:

```bash
./gradlew nativeSmoke
```

Build Javadocs:

```bash
./gradlew javadoc
```

Print artifact coordinates:

```bash
./gradlew printPublishedArtifacts
```

## Architecture Rules

Architecture rules are part of the project, not just comments in code. They are documented in
`docs/architecture/architecture-rule-catalog.md` and enforced with ArchUnit and generated-source
tests.

The rules are grouped into:

- rules specific to this project;
- rules for GraalVM Native Image friendly Java code;
- baseline Java rules that keep the library predictable.

## Use Of Coding Agents

This project openly uses coding agents as part of development. Agents may draft code, tests, docs,
and build configuration. Human review still matters: changes should be checked against the
architecture catalog, tests, generated output, and native-image goals before they are accepted.

Agent-generated work should be treated like any other contribution: readable, tested, documented,
and small enough to review.
