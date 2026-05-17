package io.github.mundanej.mjdi.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjdi.generator.fixtures.valid.Repository;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GeneratorCliTest {
    @TempDir
    Path tempDir;

    @Test
    void dryRunPrintsSourceWithoutWritingFile() throws Exception {
        CliResult result = withFixtureClasspath(() -> runCli(
                "--output-dir",
                tempDir.toString(),
                "--module-package",
                "com.example.generated",
                "--module-class",
                "GeneratedAppModule",
                "--package-root",
                "io.github.mundanej.mjdi.generator.fixtures.valid",
                "--dry-run"));

        assertEquals(0, result.exitCode());
        assertTrue(result.out().contains("public final class GeneratedAppModule implements AppPluginModule"));
        assertTrue(result.out().contains("binder.bindSingleton("));
        assertFalse(Files.exists(tempDir.resolve("com/example/generated/GeneratedAppModule.java")));
    }

    @Test
    void writesGeneratedSourceToPackagePath() throws Exception {
        CliResult result = withFixtureClasspath(() -> runCli(
                "--output-dir",
                tempDir.toString(),
                "--module-package",
                "com.example.generated",
                "--module-class",
                "GeneratedAppModule",
                "--package-root",
                "io.github.mundanej.mjdi.generator.fixtures.valid"));

        Path sourceFile = tempDir.resolve("com/example/generated/GeneratedAppModule.java");
        assertEquals(0, result.exitCode());
        assertTrue(Files.readString(sourceFile).contains("package com.example.generated;"));
    }

    @Test
    void missingRequiredArgsFailWithUsage() {
        CliResult result = runCli("--module-package", "com.example.generated");

        assertEquals(1, result.exitCode());
        assertTrue(result.err().contains("--output-dir is required"));
        assertTrue(result.err().contains("usage: GeneratorCli"));
    }

    @Test
    void missingModulePackageFails() {
        CliResult result = runCli("--output-dir", tempDir.toString(), "--module-class", "GeneratedAppModule");

        assertEquals(1, result.exitCode());
        assertTrue(result.err().contains("--module-package is required"));
    }

    @Test
    void missingModuleClassFails() {
        CliResult result = runCli(
                "--output-dir", tempDir.toString(), "--module-package", "com.example.generated");

        assertEquals(1, result.exitCode());
        assertTrue(result.err().contains("--module-class is required"));
    }

    @Test
    void unknownArgumentFails() {
        CliResult result = runCli("--unknown");

        assertEquals(1, result.exitCode());
        assertTrue(result.err().contains("unknown argument"));
    }

    @Test
    void missingOptionValueFails() {
        CliResult result = runCli("--output-dir");

        assertEquals(1, result.exitCode());
        assertTrue(result.err().contains("--output-dir requires a value"));
    }

    @Test
    void invalidGeneratedJavaNamesFailBeforeWritingFile() throws Exception {
        CliResult result = withFixtureClasspath(() -> runCli(
                "--output-dir",
                tempDir.toString(),
                "--module-package",
                "com.example.generated",
                "--module-class",
                "class",
                "--package-root",
                "io.github.mundanej.mjdi.generator.fixtures.valid"));

        assertEquals(1, result.exitCode());
        assertTrue(result.err().contains("moduleClassName must be a simple Java name"));
        assertFalse(Files.exists(tempDir.resolve("com/example/generated/class.java")));
    }

    @Test
    void existingDifferentFileRequiresOverwrite() throws Exception {
        Path sourceFile = tempDir.resolve("com/example/generated/GeneratedAppModule.java");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, "different", StandardCharsets.UTF_8);

        CliResult blocked = withFixtureClasspath(() -> runCli(
                "--output-dir",
                tempDir.toString(),
                "--module-package",
                "com.example.generated",
                "--module-class",
                "GeneratedAppModule",
                "--package-root",
                "io.github.mundanej.mjdi.generator.fixtures.valid"));
        CliResult overwritten = withFixtureClasspath(() -> runCli(
                "--output-dir",
                tempDir.toString(),
                "--module-package",
                "com.example.generated",
                "--module-class",
                "GeneratedAppModule",
                "--package-root",
                "io.github.mundanej.mjdi.generator.fixtures.valid",
                "--overwrite"));

        assertEquals(1, blocked.exitCode());
        assertTrue(blocked.err().contains("already exists with different content"));
        assertEquals(0, overwritten.exitCode());
        assertTrue(Files.readString(sourceFile).contains("binder.bindSingleton("));
    }

    @Test
    void existingSameFileCanBeRegeneratedWithoutOverwrite() throws Exception {
        CliResult first = withFixtureClasspath(() -> runCli(
                "--output-dir",
                tempDir.toString(),
                "--module-package",
                "com.example.generated",
                "--module-class",
                "GeneratedAppModule",
                "--package-root",
                "io.github.mundanej.mjdi.generator.fixtures.valid"));
        CliResult second = withFixtureClasspath(() -> runCli(
                "--output-dir",
                tempDir.toString(),
                "--module-package",
                "com.example.generated",
                "--module-class",
                "GeneratedAppModule",
                "--package-root",
                "io.github.mundanej.mjdi.generator.fixtures.valid"));

        assertEquals(0, first.exitCode());
        assertEquals(0, second.exitCode());
    }

    private static CliResult runCli(String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int exitCode = new GeneratorCli()
                .run(
                        args,
                        new PrintStream(out, true, StandardCharsets.UTF_8),
                        new PrintStream(err, true, StandardCharsets.UTF_8));
        return new CliResult(
                exitCode,
                out.toString(StandardCharsets.UTF_8),
                err.toString(StandardCharsets.UTF_8));
    }

    private static <T> T withFixtureClasspath(ThrowingSupplier<T> supplier) throws Exception {
        String originalClasspath = System.getProperty("java.class.path");
        System.setProperty("java.class.path", fixtureClasspathRoot().toString());
        try {
            return supplier.get();
        } finally {
            System.setProperty("java.class.path", originalClasspath);
        }
    }

    private static Path fixtureClasspathRoot() throws URISyntaxException {
        return Path.of(Repository.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    private record CliResult(int exitCode, String out, String err) {}

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
