# AGENTS.md

This file gives coding agents the minimum project context needed to make useful changes.

## Project Goals

- Keep `modules/core` small, dependency-free at runtime, and friendly to GraalVM Native Image.
- Prefer explicit providers and generated direct constructor calls over runtime discovery.
- Keep public APIs documented for new Java contributors.
- Mechanize architectural rules with ArchUnit whenever practical.

## Build Commands

- Run the full local gate with `./gradlew qualityGate`.
- Run all JVM checks with `./gradlew checkAll`.
- Run coverage verification with `./gradlew jacocoTestCoverageVerification`.
- Run native smoke checks with `./gradlew nativeSmoke` only when GraalVM `native-image` is installed.
- Check whitespace with `git diff --check`.

## Editing Guidelines

- Put published library code under `modules/`.
- Put examples under `examples/`.
- Keep shared Gradle setup in `build-logic`.
- Add or update Javadocs for every public production API change.
- If a change needs reflection, class loading, serialization, process execution, or global JVM state,
  update `docs/architecture/architecture-rule-catalog.md` and explain the exception.
- Do not add third-party runtime dependencies to `modules/core`.

## Review Checklist

- Does `modules/core` remain provider-only and reflection-free?
- Are generated sources free of generator packages and native-image-hostile APIs?
- Are new rules documented and enforced?
- Did `qualityGate` pass, or is the reason it could not run documented?
