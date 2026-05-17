package io.github.mundanej.mjdi.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import io.github.mundanej.mjdi.AppPluginModule;
import io.github.mundanej.mjdi.BootstrapAppContext;
import io.github.mundanej.mjdi.Key;
import io.github.mundanej.mjdi.generator.fixtures.valid.NamedConsumer;
import io.github.mundanej.mjdi.generator.fixtures.valid.NamedRepository;
import io.github.mundanej.mjdi.generator.fixtures.valid.Repository;
import io.github.mundanej.mjdi.generator.fixtures.valid.ScalarConsumer;
import io.github.mundanej.mjdi.generator.fixtures.valid.Service;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GeneratedSourceQualityTest {
  @TempDir Path tempDir;

  @Test
  void generatedSourceCompilesWithWarningsAsErrors() throws Exception {
    Path sourceFile = writeGeneratedSource();
    Path classesDirectory = tempDir.resolve("classes");

    assertEquals(0, compile(sourceFile, classesDirectory));
  }

  @Test
  void generatedSourcePassesJavadocsWithDoclint() throws Exception {
    Path sourceFile = writeGeneratedSource();
    Path docsDirectory = tempDir.resolve("docs");

    int exitCode =
        ToolProvider.getSystemDocumentationTool()
            .run(
                null,
                null,
                null,
                "-quiet",
                "-Xdoclint:all",
                "-classpath",
                compilerClasspath(),
                "-d",
                docsDirectory.toString(),
                sourceFile.toString());

    assertEquals(0, exitCode);
  }

  @Test
  void generatedSourcePassesProjectCheckstyle() throws Exception {
    Path sourceFile = writeGeneratedSource();

    assertEquals(0, checkstyle(sourceFile));
  }

  @Test
  void generatedSourceIsAlreadyGoogleJavaFormatted() throws Exception {
    Path sourceFile = writeGeneratedSource();

    Process process =
        new ProcessBuilder(
                javaCommand().toString(),
                "--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
                "-cp",
                System.getProperty("java.class.path"),
                "com.google.googlejavaformat.java.Main",
                "--dry-run",
                "--set-exit-if-changed",
                sourceFile.toString())
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start();

    assertTrue(process.waitFor(30, TimeUnit.SECONDS), "google-java-format did not finish");
    assertEquals(0, process.exitValue());
  }

  @Test
  void generatedSingletonBindingsReturnSameInstances() throws Exception {
    Path sourceFile = writeGeneratedSource();
    Path classesDirectory = tempDir.resolve("classes");
    assertEquals(0, compile(sourceFile, classesDirectory));

    try (URLClassLoader loader =
        new URLClassLoader(
            new java.net.URL[] {classesDirectory.toUri().toURL()}, getClass().getClassLoader())) {
      Class<?> moduleType = Class.forName("com.example.generated.GeneratedAppModule", true, loader);
      AppPluginModule module = (AppPluginModule) moduleType.getConstructor().newInstance();
      var context =
          BootstrapAppContext.create(List.of(GeneratedSourceQualityTest::bindScalars, module));

      Service firstService = context.get(Service.class);
      Service secondService = context.get(Service.class);
      NamedConsumer firstConsumer = context.get(Key.named(NamedConsumer.class, "consumer"));
      NamedConsumer secondConsumer = context.get(Key.named(NamedConsumer.class, "consumer"));
      NamedRepository namedRepository = context.get(Key.named(NamedRepository.class, "main"));
      ScalarConsumer scalarConsumer = context.get(ScalarConsumer.class);

      assertSame(firstService, secondService);
      assertSame(firstService.repository(), secondService.repository());
      assertSame(firstConsumer, secondConsumer);
      assertSame(firstConsumer.repository(), namedRepository);
      assertEquals("alpha", scalarConsumer.text());
      assertEquals(true, scalarConsumer.enabled());
      assertEquals(7, scalarConsumer.count());
      assertEquals(8L, scalarConsumer.distance());
      assertEquals(1.5D, scalarConsumer.ratio());
      assertEquals(2.5F, scalarConsumer.scale());
      assertEquals((short) 3, scalarConsumer.small());
      assertEquals((byte) 4, scalarConsumer.tiny());
      assertEquals('x', scalarConsumer.letter());
    }
  }

  @Test
  void generatedSourceKeepsCoverageShapeSimple() throws Exception {
    String source = generatedSource().sourceText();

    assertTrue(source.contains("binder.bindSingleton("));
    assertTrue(source.contains("context -> new "));
    assertTrue(!source.contains("if ("));
    assertTrue(!source.contains(" ? "));
  }

  private Path writeGeneratedSource() throws IOException {
    GeneratedModuleSource source = generatedSource();
    Path sourceFile =
        tempDir
            .resolve(source.packageName().replace('.', '/'))
            .resolve(source.className() + ".java");
    Files.createDirectories(Objects.requireNonNull(sourceFile.getParent(), "sourceFile parent"));
    Files.writeString(sourceFile, source.sourceText(), StandardCharsets.UTF_8);
    return sourceFile;
  }

  private static GeneratedModuleSource generatedSource() {
    GeneratedModuleRequest request =
        new GeneratedModuleRequest(
            "com.example.generated",
            "GeneratedAppModule",
            List.of(
                GeneratedModuleRequest.ConstructorBinding.binding(
                    Repository.class.getCanonicalName()),
                GeneratedModuleRequest.ConstructorBinding.binding(
                    Service.class.getCanonicalName(),
                    GeneratedModuleRequest.Dependency.of(Repository.class.getCanonicalName())),
                GeneratedModuleRequest.ConstructorBinding.namedBinding(
                    NamedRepository.class.getCanonicalName(), "main"),
                GeneratedModuleRequest.ConstructorBinding.namedBinding(
                    NamedConsumer.class.getCanonicalName(),
                    "consumer",
                    GeneratedModuleRequest.Dependency.named(
                        NamedRepository.class.getCanonicalName(), "main")),
                GeneratedModuleRequest.ConstructorBinding.binding(
                    ScalarConsumer.class.getCanonicalName(),
                    GeneratedModuleRequest.Dependency.named(
                        String.class.getCanonicalName(), "text"),
                    GeneratedModuleRequest.Dependency.named("boolean", "enabled"),
                    GeneratedModuleRequest.Dependency.named("int", "count"),
                    GeneratedModuleRequest.Dependency.named("long", "distance"),
                    GeneratedModuleRequest.Dependency.named("double", "ratio"),
                    GeneratedModuleRequest.Dependency.named("float", "scale"),
                    GeneratedModuleRequest.Dependency.named("short", "small"),
                    GeneratedModuleRequest.Dependency.named("byte", "tiny"),
                    GeneratedModuleRequest.Dependency.named("char", "letter"))));
    return new InjectionModuleSourceGenerator().generate(request);
  }

  private static void bindScalars(io.github.mundanej.mjdi.Binder binder) {
    binder
        .bindInstance(Key.named(String.class, "text"), "alpha")
        .bindInstance(Key.named(Boolean.class, "enabled"), true)
        .bindInstance(Key.named(Integer.class, "count"), 7)
        .bindInstance(Key.named(Long.class, "distance"), 8L)
        .bindInstance(Key.named(Double.class, "ratio"), 1.5D)
        .bindInstance(Key.named(Float.class, "scale"), 2.5F)
        .bindInstance(Key.named(Short.class, "small"), (short) 3)
        .bindInstance(Key.named(Byte.class, "tiny"), (byte) 4)
        .bindInstance(Key.named(Character.class, "letter"), 'x');
  }

  private static int compile(Path sourceFile, Path classesDirectory) throws IOException {
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
        File.pathSeparator, codeSource(Repository.class), codeSource(AppPluginModule.class));
  }

  private static String codeSource(Class<?> type) {
    URI uri = URI.create(type.getProtectionDomain().getCodeSource().getLocation().toString());
    return Path.of(uri).toString();
  }

  private static Path javaCommand() {
    String executable =
        System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("win")
            ? "java.exe"
            : "java";
    return Path.of(System.getProperty("java.home"), "bin", executable);
  }

  private static int checkstyle(Path sourceFile) throws CheckstyleException {
    Configuration configuration =
        ConfigurationLoader.loadConfiguration(
            checkstyleConfigurationPath().toString(), name -> null);
    Checker checker = new Checker();
    checker.setModuleClassLoader(Checker.class.getClassLoader());
    checker.configure(configuration);
    try {
      return checker.process(List.of(sourceFile.toFile()));
    } finally {
      checker.destroy();
    }
  }

  private static Path checkstyleConfigurationPath() {
    Path currentDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
    Path directPath = currentDirectory.resolve("config/checkstyle/checkstyle.xml");
    if (Files.exists(directPath)) {
      return directPath;
    }
    return currentDirectory.resolve("../../config/checkstyle/checkstyle.xml").normalize();
  }
}
