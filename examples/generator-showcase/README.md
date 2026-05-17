# Generator Showcase Example

This example shows three ways to use the `mundane-java-di` source generator:

- run a Gradle task that scans compiled application classes with `--scan-path`;
- inspect the generated module with `--dry-run`;
- call the scanner and source generator from Java code for custom build tooling.

The generator runs at build time. It scans compiled classes with `@Inject` constructors, writes a
plain `AppPluginModule`, and that generated module is compiled like ordinary application source.
The runtime application only needs `modules/core`.

## Generate From Gradle

```bash
./gradlew :examples:generator-showcase:generateMjdiModule
```

The generated source is written under:

```text
examples/generator-showcase/build/generated/sources/mjdi
```

The task uses an explicit class output directory:

```bash
--scan-path examples/generator-showcase/build/classes/java/main
```

That shape is the recommended build lifecycle: compile application classes first, scan those
classes, generate direct-constructor module source, then compile the generated module.

## Inspect Without Writing

```bash
./gradlew :examples:generator-showcase:dryRunMjdiModule
```

This prints the generated `GeneratedShowcaseModule` source to standard output without writing a
source file.

## Run The Example Checks

```bash
./gradlew :examples:generator-showcase:check
```

The tests compile the generated module separately, put it on the test runtime classpath, load it as
an `AppPluginModule`, and bootstrap it together with `ShowcaseConfigModule`.

The example graph demonstrates:

- constructor injection from `CheckoutService` to `InventoryRepository`;
- class-level `@Named("standard")` on `ShippingRates`;
- named scalar constructor parameters supplied by `ShowcaseConfigModule`;
- singleton bindings emitted by generated source.
