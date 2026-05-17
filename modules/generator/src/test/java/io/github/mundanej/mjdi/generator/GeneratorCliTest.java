package io.github.mundanej.mjdi.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjdi.generator.fixtures.valid.Repository;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GeneratorCliTest {
  @TempDir Path tempDir;

  @Test
  void helpPrintsUsageWithoutRequiredArguments() {
    CliResult result = runCli("--help");

    assertEquals(0, result.exitCode());
    assertTrue(result.out().contains("usage: GeneratorCli"));
    assertTrue(result.out().contains("--scan-path PATH"));
    assertEquals("", result.err());
  }

  @Test
  void versionPrintsVersionWithoutRequiredArguments() {
    CliResult result = runCli("--version");

    assertEquals(0, result.exitCode());
    assertTrue(!result.out().isBlank());
    assertEquals("", result.err());
  }

  @Test
  void dryRunPrintsSourceWithoutWritingFile() throws Exception {
    CliResult result =
        withFixtureClasspath(
            () ->
                runCli(
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
    assertTrue(
        result.out().contains("public final class GeneratedAppModule implements AppPluginModule"));
    assertTrue(result.out().contains("binder.bindSingleton("));
    assertFalse(Files.exists(tempDir.resolve("com/example/generated/GeneratedAppModule.java")));
  }

  @Test
  void writesGeneratedSourceToPackagePath() throws Exception {
    CliResult result =
        runCli(
            "--output-dir",
            tempDir.toString(),
            "--module-package",
            "com.example.generated",
            "--module-class",
            "GeneratedAppModule",
            "--package-root",
            "io.github.mundanej.mjdi.generator.fixtures.valid",
            "--scan-path",
            fixtureClasspathRoot().toString());

    Path sourceFile = tempDir.resolve("com/example/generated/GeneratedAppModule.java");
    assertEquals(0, result.exitCode());
    assertEquals("Generated " + sourceFile + System.lineSeparator(), result.out());
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
    CliResult result =
        runCli("--output-dir", tempDir.toString(), "--module-class", "GeneratedAppModule");

    assertEquals(1, result.exitCode());
    assertTrue(result.err().contains("--module-package is required"));
  }

  @Test
  void missingModuleClassFails() {
    CliResult result =
        runCli("--output-dir", tempDir.toString(), "--module-package", "com.example.generated");

    assertEquals(1, result.exitCode());
    assertTrue(result.err().contains("--module-class is required"));
  }

  @Test
  void missingPackageRootFailsBeforeWritingFile() {
    CliResult result =
        runCli(
            "--output-dir",
            tempDir.toString(),
            "--module-package",
            "com.example.generated",
            "--module-class",
            "GeneratedAppModule");

    assertEquals(1, result.exitCode());
    assertTrue(result.err().contains("at least one package root is required"));
    assertFalse(Files.exists(tempDir.resolve("com/example/generated/GeneratedAppModule.java")));
  }

  @Test
  void blankOptionValuesFailBeforeWritingFile() {
    assertBlankValueFails("--module-package", "--module-package is required");
    assertBlankValueFails("--module-class", "--module-class is required");
    assertBlankValueFails("--package-root", "package roots must not be blank");
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
  void missingOptionValueFollowedByAnotherFlagFails() {
    CliResult result = runCli("--output-dir", "--module-package", "com.example.generated");

    assertEquals(1, result.exitCode());
    assertTrue(result.err().contains("--output-dir requires a value"));
  }

  @Test
  void multiplePackageRootsAreScanned() throws Exception {
    CliResult result =
        runCli(
            "--output-dir",
            tempDir.toString(),
            "--module-package",
            "com.example.generated",
            "--module-class",
            "GeneratedAppModule",
            "--package-root",
            "io.github.mundanej.mjdi.generator.fixtures.missing",
            "--package-root",
            "io.github.mundanej.mjdi.generator.fixtures.valid",
            "--scan-path",
            fixtureClasspathRoot().toString(),
            "--dry-run");

    assertEquals(0, result.exitCode());
    assertTrue(result.out().contains("Service.class"));
  }

  @Test
  void multipleScanPathsAreAccepted() throws Exception {
    Path emptyClasses = tempDir.resolve("empty-classes");
    Files.createDirectories(emptyClasses);

    CliResult result =
        runCli(
            "--output-dir",
            tempDir.toString(),
            "--module-package",
            "com.example.generated",
            "--module-class",
            "GeneratedAppModule",
            "--package-root",
            "io.github.mundanej.mjdi.generator.fixtures.valid",
            "--scan-path",
            emptyClasses.toString(),
            "--scan-path",
            fixtureClasspathRoot().toString(),
            "--dry-run");

    assertEquals(0, result.exitCode());
    assertTrue(result.out().contains("class GeneratedAppModule"));
    assertTrue(result.out().contains("Service.class"));
  }

  @Test
  void invalidGeneratedJavaNamesFailBeforeWritingFile() throws Exception {
    CliResult result =
        withFixtureClasspath(
            () ->
                runCli(
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
  void scannerFailurePropagatesWithoutPartialFile() throws Exception {
    CliResult result =
        withFixtureClasspath(
            () ->
                runCli(
                    "--output-dir",
                    tempDir.toString(),
                    "--module-package",
                    "com.example.generated",
                    "--module-class",
                    "GeneratedAppModule",
                    "--package-root",
                    "io.github.mundanej.mjdi.generator.fixtures.invalid.multiple"));

    assertEquals(1, result.exitCode());
    assertTrue(result.err().contains("has multiple @Inject constructors"));
    assertFalse(Files.exists(tempDir.resolve("com/example/generated/GeneratedAppModule.java")));
  }

  @Test
  void nullRunPreconditionsThrow() {
    PrintStream stream = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);
    GeneratorCli cli = new GeneratorCli();

    assertEquals(
        "args",
        assertThrows(NullPointerException.class, () -> cli.run(null, stream, stream)).getMessage());
    assertEquals(
        "out",
        assertThrows(NullPointerException.class, () -> cli.run(new String[0], null, stream))
            .getMessage());
    assertEquals(
        "err",
        assertThrows(NullPointerException.class, () -> cli.run(new String[0], stream, null))
            .getMessage());
  }

  @Test
  void outputDirAsFileFailsWithoutReplacingFile() throws Exception {
    Path outputFile = tempDir.resolve("not-a-directory");
    Files.writeString(outputFile, "existing", StandardCharsets.UTF_8);

    CliResult result =
        withFixtureClasspath(
            () ->
                runCli(
                    "--output-dir",
                    outputFile.toString(),
                    "--module-package",
                    "com.example.generated",
                    "--module-class",
                    "GeneratedAppModule",
                    "--package-root",
                    "io.github.mundanej.mjdi.generator.fixtures.valid"));

    assertEquals(1, result.exitCode());
    assertTrue(result.err().contains("usage: GeneratorCli"));
    assertEquals("existing", Files.readString(outputFile, StandardCharsets.UTF_8));
  }

  @Test
  void mainCanGenerateSourceInForkedJvm() throws Exception {
    Path stdout = tempDir.resolve("generator-cli-main.out");
    Path stderr = tempDir.resolve("generator-cli-main.err");
    Process process =
        new ProcessBuilder(
                javaCommand().toString(),
                "-cp",
                System.getProperty("java.class.path"),
                GeneratorCli.class.getName(),
                "--output-dir",
                tempDir.toString(),
                "--module-package",
                "com.example.generated",
                "--module-class",
                "GeneratedAppModule",
                "--package-root",
                "io.github.mundanej.mjdi.generator.fixtures.valid")
            .redirectOutput(stdout.toFile())
            .redirectError(stderr.toFile())
            .start();

    assertTrue(process.waitFor(30, TimeUnit.SECONDS), "forked GeneratorCli.main did not finish");
    assertEquals(0, process.exitValue());
    assertTrue(Files.readString(stdout, StandardCharsets.UTF_8).contains("Generated "));
    assertEquals("", Files.readString(stderr, StandardCharsets.UTF_8));
    assertTrue(
        Files.readString(tempDir.resolve("com/example/generated/GeneratedAppModule.java"))
            .contains("public final class GeneratedAppModule implements AppPluginModule"));
  }

  @Test
  void existingDifferentFileRequiresOverwrite() throws Exception {
    Path sourceFile = tempDir.resolve("com/example/generated/GeneratedAppModule.java");
    Files.createDirectories(Objects.requireNonNull(sourceFile.getParent(), "sourceFile parent"));
    Files.writeString(sourceFile, "different", StandardCharsets.UTF_8);

    CliResult blocked =
        withFixtureClasspath(
            () ->
                runCli(
                    "--output-dir",
                    tempDir.toString(),
                    "--module-package",
                    "com.example.generated",
                    "--module-class",
                    "GeneratedAppModule",
                    "--package-root",
                    "io.github.mundanej.mjdi.generator.fixtures.valid"));
    CliResult overwritten =
        withFixtureClasspath(
            () ->
                runCli(
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
    CliResult first =
        withFixtureClasspath(
            () ->
                runCli(
                    "--output-dir",
                    tempDir.toString(),
                    "--module-package",
                    "com.example.generated",
                    "--module-class",
                    "GeneratedAppModule",
                    "--package-root",
                    "io.github.mundanej.mjdi.generator.fixtures.valid"));
    CliResult second =
        withFixtureClasspath(
            () ->
                runCli(
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

  private void assertBlankValueFails(String option, String expectedError) {
    CliResult result =
        runCli(
            "--output-dir",
            tempDir.toString(),
            "--module-package",
            option.equals("--module-package") ? " " : "com.example.generated",
            "--module-class",
            option.equals("--module-class") ? " " : "GeneratedAppModule",
            "--package-root",
            option.equals("--package-root")
                ? " "
                : "io.github.mundanej.mjdi.generator.fixtures.valid");

    assertEquals(1, result.exitCode());
    assertTrue(result.err().contains(expectedError));
    assertFalse(Files.exists(tempDir.resolve("com/example/generated/GeneratedAppModule.java")));
  }

  private static Path javaCommand() {
    String executable =
        System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")
            ? "java.exe"
            : "java";
    return Path.of(System.getProperty("java.home"), "bin", executable);
  }

  private static CliResult runCli(String... args) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    int exitCode =
        new GeneratorCli()
            .run(
                args,
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
    return new CliResult(
        exitCode, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
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
