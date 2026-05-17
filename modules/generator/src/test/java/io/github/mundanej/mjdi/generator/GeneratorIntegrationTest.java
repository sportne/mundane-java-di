package io.github.mundanej.mjdi.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mundanej.mjdi.AppPluginModule;
import io.github.mundanej.mjdi.Binder;
import io.github.mundanej.mjdi.BootstrapAppContext;
import io.github.mundanej.mjdi.Key;
import io.github.mundanej.mjdi.generator.fixtures.valid.NamedConsumer;
import io.github.mundanej.mjdi.generator.fixtures.valid.NamedRepository;
import io.github.mundanej.mjdi.generator.fixtures.valid.Repository;
import io.github.mundanej.mjdi.generator.fixtures.valid.ScalarConsumer;
import io.github.mundanej.mjdi.generator.fixtures.valid.Service;
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

class GeneratorIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void cliGeneratedModuleCompilesLoadsAndProvidesFixtureGraph() throws Exception {
        Path sourceFile = writeGeneratedFixtureModule();
        Path classesDirectory = tempDir.resolve("classes");
        assertEquals(0, compile(sourceFile, classesDirectory, fixtureCompilerClasspath()));

        try (URLClassLoader loader =
                new URLClassLoader(new java.net.URL[] {classesDirectory.toUri().toURL()}, getClass().getClassLoader())) {
            AppPluginModule module = loadModule(loader, "com.example.generated.GeneratedAppModule");
            var context = BootstrapAppContext.create(List.of(GeneratorIntegrationTest::bindScalars, module));

            Service firstService = context.get(Service.class);
            Service secondService = context.get(Service.class);
            NamedConsumer namedConsumer = context.get(Key.named(NamedConsumer.class, "consumer"));
            NamedRepository namedRepository = context.get(Key.named(NamedRepository.class, "main"));
            ScalarConsumer scalarConsumer = context.get(ScalarConsumer.class);

            assertSame(firstService, secondService);
            assertSame(namedRepository, namedConsumer.repository());
            assertEquals(7, scalarConsumer.count());
        }
    }

    @Test
    void generatedModuleConflictsCanBeOverriddenExplicitly() throws Exception {
        Path sourceFile = writeGeneratedFixtureModule();
        Path classesDirectory = tempDir.resolve("classes");
        assertEquals(0, compile(sourceFile, classesDirectory, fixtureCompilerClasspath()));

        try (URLClassLoader loader =
                new URLClassLoader(new java.net.URL[] {classesDirectory.toUri().toURL()}, getClass().getClassLoader())) {
            AppPluginModule module = loadModule(loader, "com.example.generated.GeneratedAppModule");
            Repository replacementRepository = new Repository();
            AppPluginModule overrideRepository =
                    binder -> binder.bindInstance(Repository.class, replacementRepository);

            assertThrows(IllegalStateException.class, () -> BootstrapAppContext.create(List.of(module, overrideRepository)));

            Binder binder = new Binder().install(module).installOverride(overrideRepository);
            assertSame(replacementRepository, binder.build().get(Repository.class));
        }
    }

    private Path writeGeneratedFixtureModule() throws Exception {
        Path outputDirectory = tempDir.resolve("generated");
        String originalClasspath = System.getProperty("java.class.path");
        System.setProperty("java.class.path", codeSource(Repository.class));
        try {
            ByteArrayOutputStream err = new ByteArrayOutputStream();
            int exitCode = new GeneratorCli()
                    .run(
                            new String[] {
                                "--output-dir",
                                outputDirectory.toString(),
                                "--module-package",
                                "com.example.generated",
                                "--module-class",
                                "GeneratedAppModule",
                                "--package-root",
                                "io.github.mundanej.mjdi.generator.fixtures.valid"
                            },
                            new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8),
                            new PrintStream(err, true, StandardCharsets.UTF_8));
            assertEquals(0, exitCode, err.toString(StandardCharsets.UTF_8));
        } finally {
            System.setProperty("java.class.path", originalClasspath);
        }
        return outputDirectory.resolve("com/example/generated/GeneratedAppModule.java");
    }

    private static AppPluginModule loadModule(URLClassLoader loader, String className) throws Exception {
        Class<?> moduleType = Class.forName(className, true, loader);
        return (AppPluginModule) moduleType.getConstructor().newInstance();
    }

    private static void bindScalars(Binder binder) {
        binder.bindInstance(Key.named(String.class, "text"), "alpha")
                .bindInstance(Key.named(Boolean.class, "enabled"), true)
                .bindInstance(Key.named(Integer.class, "count"), 7)
                .bindInstance(Key.named(Long.class, "distance"), 8L)
                .bindInstance(Key.named(Double.class, "ratio"), 1.5D)
                .bindInstance(Key.named(Float.class, "scale"), 2.5F)
                .bindInstance(Key.named(Short.class, "small"), (short) 3)
                .bindInstance(Key.named(Byte.class, "tiny"), (byte) 4)
                .bindInstance(Key.named(Character.class, "letter"), 'x');
    }

    private static int compile(Path sourceFile, Path classesDirectory, String classpath) throws Exception {
        Files.createDirectories(classesDirectory);
        return ToolProvider.getSystemJavaCompiler()
                .run(
                        null,
                        null,
                        null,
                        "--release",
                        "17",
                        "-Xlint:all",
                        "-Werror",
                        "-classpath",
                        classpath,
                        "-d",
                        classesDirectory.toString(),
                        sourceFile.toString());
    }

    private static String fixtureCompilerClasspath() {
        return String.join(File.pathSeparator, codeSource(Repository.class), codeSource(AppPluginModule.class));
    }

    private static String codeSource(Class<?> type) {
        URI uri = URI.create(type.getProtectionDomain().getCodeSource().getLocation().toString());
        return Path.of(uri).toString();
    }
}
