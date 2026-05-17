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
            var context = BootstrapAppContext.create(List.of(module));

            Service firstService = context.get(Service.class);
            Service secondService = context.get(Service.class);
            NamedConsumer firstConsumer = context.get(Key.named(NamedConsumer.class, "consumer"));
            NamedConsumer secondConsumer = context.get(Key.named(NamedConsumer.class, "consumer"));
            NamedRepository namedRepository = context.get(Key.named(NamedRepository.class, "main"));

            assertSame(firstService, secondService);
            assertSame(firstService.repository(), secondService.repository());
            assertSame(firstConsumer, secondConsumer);
            assertSame(firstConsumer.repository(), namedRepository);
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
                                        NamedRepository.class.getCanonicalName(), "main"))));
        return new InjectionModuleSourceGenerator().generate(request);
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
