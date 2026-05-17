# mundane-java-di

`mundane-java-di` is a small Java dependency injection library for projects that care about
GraalVM Native Image compatibility.

The design is intentionally plain:

- application code registers providers in modules;
- the runtime looks up objects from an `AppContext`;
- generated modules call constructors directly;
- the runtime avoids reflection, classpath scanning, dynamic proxies, and other features that make
  native-image builds harder to reason about.

## Project Layout

- `modules/core` contains the runtime API.
- `modules/generator` contains build-time source generation support.
- `examples/basic` contains a tiny example module and usage test.
- `docs/architecture/architecture-rule-catalog.md` documents the rules enforced by ArchUnit.
- `build-logic` contains small Gradle convention plugins shared by the modules.

## Quick Start

Run the normal local gate:

```bash
./gradlew qualityGate
```

Run a narrower JVM test pass:

```bash
./gradlew checkAll
```

Run the native-image smoke lane when GraalVM `native-image` is available:

```bash
./gradlew nativeSmoke
```

Print planned published coordinates:

```bash
./gradlew printPublishedArtifacts
```

## Dependency Injection Model

The core runtime is provider-based. A module receives a `Binder` and registers bindings:

```java
binder.bind(Repository.class, context -> new Repository());
binder.bind(Service.class, context -> new Service(context.get(Repository.class)));
```

`@Inject` and `@Named` are metadata for generator and architecture tests. The runtime core does not
scan annotations or use reflection to create objects.

## Architecture Rules

Architecture rules are part of the project, not just comments in code. They are documented in
`docs/architecture/architecture-rule-catalog.md` and enforced with ArchUnit and generated-source
tests.

The rules are grouped into:

- rules specific to this project;
- rules for GraalVM Native Image friendly Java code;
- baseline Java rules that keep the library predictable.

## Javadocs

Public production APIs include Javadocs written for new contributors. The goal is to explain what
each type does in normal language before a contributor has to understand every implementation
detail.

Build Javadocs locally with:

```bash
./gradlew javadoc
```

## Use Of Coding Agents

This project openly uses coding agents as part of development. Agents may draft code, tests, docs,
and build configuration. Human review still matters: changes should be checked against the
architecture catalog, tests, generated output, and native-image goals before they are accepted.

Agent-generated work should be treated like any other contribution: readable, tested, documented,
and small enough to review.
