package io.github.mundanej.mjdi.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.github.mundanej.mjdi.AppPluginModule;
import io.github.mundanej.mjdi.BootstrapAppContext;
import io.github.mundanej.mjdi.generator.GeneratorCli;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExampleGeneratedModuleJvmOnlyTest {
  @TempDir Path tempDir;

  @Test
  void generatedExampleModuleCompilesLoadsAndProvidesService() throws Exception {
    Path sourceFile = writeGeneratedExampleModule();
    Path classesDirectory = tempDir.resolve("classes");
    assertEquals(0, compile(sourceFile, classesDirectory));

    try (URLClassLoader loader =
        new URLClassLoader(
            new java.net.URL[] {classesDirectory.toUri().toURL()}, getClass().getClassLoader())) {
      Class<?> moduleType =
          Class.forName("io.github.mundanej.mjdi.examples.GeneratedExampleModule", true, loader);
      AppPluginModule module = (AppPluginModule) moduleType.getConstructor().newInstance();
      var context = BootstrapAppContext.create(List.of(module));

      ExampleAppModule.Service firstService = context.get(ExampleAppModule.Service.class);
      ExampleAppModule.Service secondService = context.get(ExampleAppModule.Service.class);
      ExampleAppModule.Repository firstRepository = context.get(ExampleAppModule.Repository.class);
      ExampleAppModule.Repository secondRepository = context.get(ExampleAppModule.Repository.class);

      assertEquals("example", firstService.value());
      assertSame(firstService, secondService);
      assertSame(firstRepository, secondRepository);
    }
  }

  private Path writeGeneratedExampleModule() throws Exception {
    Path outputDirectory = tempDir.resolve("generated");
    String originalClasspath = System.getProperty("java.class.path");
    System.setProperty("java.class.path", codeSource(ExampleAppModule.class));
    try {
      ByteArrayOutputStream err = new ByteArrayOutputStream();
      int exitCode =
          new GeneratorCli()
              .run(
                  new String[] {
                    "--output-dir",
                    outputDirectory.toString(),
                    "--module-package",
                    "io.github.mundanej.mjdi.examples",
                    "--module-class",
                    "GeneratedExampleModule",
                    "--package-root",
                    "io.github.mundanej.mjdi.examples"
                  },
                  new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8),
                  new PrintStream(err, true, StandardCharsets.UTF_8));
      assertEquals(0, exitCode, err.toString(StandardCharsets.UTF_8));
    } finally {
      System.setProperty("java.class.path", originalClasspath);
    }
    return outputDirectory.resolve("io/github/mundanej/mjdi/examples/GeneratedExampleModule.java");
  }

  private static int compile(Path sourceFile, Path classesDirectory) throws Exception {
    Files.createDirectories(classesDirectory);
    return ToolProvider.getSystemJavaCompiler()
        .run(
            null,
            null,
            null,
            "--release",
            "21",
            "-Xlint:all",
            "-Werror",
            "-classpath",
            compilerClasspath(),
            "-d",
            classesDirectory.toString(),
            sourceFile.toString());
  }

  private static String compilerClasspath() {
    return String.join(
        File.pathSeparator, codeSource(ExampleAppModule.class), codeSource(AppPluginModule.class));
  }

  private static String codeSource(Class<?> type) {
    URI uri = URI.create(type.getProtectionDomain().getCodeSource().getLocation().toString());
    return Path.of(uri).toString();
  }
}
