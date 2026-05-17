package io.github.mundanej.mjdi.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GeneratedSourceQualityTest {
    @TempDir
    Path tempDir;

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

        int exitCode = ToolProvider.getSystemDocumentationTool()
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
    void generatedSingletonBindingsReturnSameInstances() throws Exception {
        Path sourceFile = writeGeneratedSource();
        Path classesDirectory = tempDir.resolve("classes");
        assertEquals(0, compile(sourceFile, classesDirectory));

        try (URLClassLoader loader =
                new URLClassLoader(new java.net.URL[] {classesDirectory.toUri().toURL()}, getClass().getClassLoader())) {
            Class<?> moduleType = Class.forName("com.example.generated.GeneratedAppModule", true, loader);
            AppPluginModule module = (AppPluginModule) moduleType.getConstructor().newInstance();
            var context = BootstrapAppContext.create(List.of(GeneratedSourceQualityTest::bindScalars, module));

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
        Path sourceFile = tempDir.resolve(source.packageName().replace('.', '/')).resolve(source.className() + ".java");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, source.sourceText(), StandardCharsets.UTF_8);
        return sourceFile;
    }

    private static GeneratedModuleSource generatedSource() {
        GeneratedModuleRequest request = new GeneratedModuleRequest(
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
                                GeneratedModuleRequest.Dependency.named(String.class.getCanonicalName(), "text"),
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

    private static int compile(Path sourceFile, Path classesDirectory) throws IOException {
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
                        compilerClasspath(),
                        "-d",
                        classesDirectory.toString(),
                        sourceFile.toString());
    }

    private static String compilerClasspath() {
        return String.join(
                File.pathSeparator,
                codeSource(Repository.class),
                codeSource(AppPluginModule.class));
    }

    private static String codeSource(Class<?> type) {
        URI uri = URI.create(type.getProtectionDomain().getCodeSource().getLocation().toString());
        return Path.of(uri).toString();
    }
}
