package io.github.mundanej.mjdi.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjdi.generator.fixtures.valid.NamedConsumer;
import io.github.mundanej.mjdi.generator.fixtures.valid.NamedRepository;
import io.github.mundanej.mjdi.generator.fixtures.valid.Repository;
import io.github.mundanej.mjdi.generator.fixtures.valid.ScalarConsumer;
import io.github.mundanej.mjdi.generator.fixtures.valid.Service;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClasspathInjectableScannerTest {
    @TempDir
    Path tempDir;

    @Test
    void scansInjectableClassesOnCurrentClasspath() throws Exception {
        GeneratedModuleRequest request = withFixtureClasspath(() -> new ClasspathInjectableScanner()
                .scan(new ClasspathInjectableScanner.ScanRequest(
                        "com.example.generated",
                        "GeneratedAppModule",
                        List.of("io.github.mundanej.mjdi.generator.fixtures.valid"))));

        List<String> typeNames = request.bindings().stream()
                .map(GeneratedModuleRequest.ConstructorBinding::typeName)
                .toList();

        assertEquals(
                List.of(
                        NamedConsumer.class.getCanonicalName(),
                        NamedRepository.class.getCanonicalName(),
                        Repository.class.getCanonicalName(),
                        ScalarConsumer.class.getCanonicalName(),
                        Service.class.getCanonicalName()),
                typeNames);
        assertFalse(typeNames.contains("io.github.mundanej.mjdi.generator.fixtures.valid.IgnoredNoInject"));
        assertTrue(request.bindings().stream()
                .anyMatch(binding -> binding.typeName().equals(NamedConsumer.class.getCanonicalName())
                        && binding.name().orElseThrow().equals("consumer")
                        && binding.dependencies().get(0).name().orElseThrow().equals("main")));
        assertTrue(request.bindings().stream()
                .anyMatch(binding -> binding.typeName().equals(ScalarConsumer.class.getCanonicalName())
                        && binding.dependencies().stream()
                                .anyMatch(dependency -> dependency.typeName().equals("int")
                                        && dependency.name().orElseThrow().equals("count"))));
    }

    @Test
    void scansInjectableClassesFromJarClasspathEntries() throws Exception {
        Path jarPath = tempDir.resolve("fixtures.jar");
        createJar(jarPath, Repository.class, Service.class);

        GeneratedModuleRequest request = withClasspath(jarPath.toString(), () -> new ClasspathInjectableScanner()
                .scan(new ClasspathInjectableScanner.ScanRequest(
                        "com.example.generated",
                        "GeneratedAppModule",
                        List.of("io.github.mundanej.mjdi.generator.fixtures.valid"))));

        assertEquals(
                List.of(Repository.class.getCanonicalName(), Service.class.getCanonicalName()),
                request.bindings().stream()
                        .map(GeneratedModuleRequest.ConstructorBinding::typeName)
                        .toList());
    }

    @Test
    void emptyClasspathProducesEmptyRequest() throws Exception {
        GeneratedModuleRequest request = withClasspath("", () -> new ClasspathInjectableScanner()
                .scan(new ClasspathInjectableScanner.ScanRequest(
                        "com.example.generated", "GeneratedAppModule", List.of("com.example"))));

        assertEquals(List.of(), request.bindings());
    }

    @Test
    void missingPackageDirectoryProducesEmptyRequest() throws Exception {
        GeneratedModuleRequest request = withFixtureClasspath(() -> new ClasspathInjectableScanner()
                .scan(new ClasspathInjectableScanner.ScanRequest(
                        "com.example.generated", "GeneratedAppModule", List.of("com.example.missing"))));

        assertEquals(List.of(), request.bindings());
    }

    @Test
    void blankPackageRootFails() {
        assertThrows(IllegalArgumentException.class, () -> new ClasspathInjectableScanner.ScanRequest(
                "com.example.generated", "GeneratedAppModule", List.of(" ")));
    }

    @Test
    void requiresPackageRoots() {
        assertThrows(IllegalArgumentException.class, () -> new ClasspathInjectableScanner.ScanRequest(
                "com.example.generated", "GeneratedAppModule", List.of()));
    }

    @Test
    void failsForMultipleInjectConstructors() throws Exception {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> scanInvalid("io.github.mundanej.mjdi.generator.fixtures.invalid.multiple"));

        assertTrue(exception.getMessage().contains("multiple @Inject constructors"));
    }

    @Test
    void failsForNonPublicInjectableClass() throws Exception {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> scanInvalid("io.github.mundanej.mjdi.generator.fixtures.invalid.hiddenclass"));

        assertTrue(exception.getMessage().contains("is not public"));
    }

    @Test
    void failsForNonPublicInjectConstructor() throws Exception {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> scanInvalid("io.github.mundanej.mjdi.generator.fixtures.invalid.hiddenctor"));

        assertTrue(exception.getMessage().contains("@Inject constructor that is not public"));
    }

    @Test
    void failsForNonStaticMemberInjectableClass() throws Exception {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> scanInvalid("io.github.mundanej.mjdi.generator.fixtures.invalid.member"));

        assertTrue(exception.getMessage().contains("non-static member class"));
    }

    @Test
    void failsForUnsupportedParameterType() throws Exception {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> scanInvalid("io.github.mundanej.mjdi.generator.fixtures.invalid.primitive"));

        assertTrue(exception.getMessage().contains("class literal"));
    }

    @Test
    void failsWhenClasspathCandidateCannotBeLoaded() throws Exception {
        Path jarPath = tempDir.resolve("broken.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jar.putNextEntry(new JarEntry("com/example/Broken.class"));
            jar.write(new byte[] {0, 1, 2, 3});
            jar.closeEntry();
        }

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> withClasspath(jarPath.toString(), () -> new ClasspathInjectableScanner()
                        .scan(new ClasspathInjectableScanner.ScanRequest(
                                "com.example.generated", "GeneratedAppModule", List.of("com.example")))));

        assertTrue(exception.getMessage().contains("failed to load classpath candidate"));
    }

    private static GeneratedModuleRequest scanInvalid(String packageRoot) throws Exception {
        return withFixtureClasspath(() -> new ClasspathInjectableScanner()
                .scan(new ClasspathInjectableScanner.ScanRequest(
                        "com.example.generated", "GeneratedAppModule", List.of(packageRoot))));
    }

    private static <T> T withFixtureClasspath(ThrowingSupplier<T> supplier) throws Exception {
        return withClasspath(fixtureClasspathRoot().toString(), supplier);
    }

    private static <T> T withClasspath(String classpath, ThrowingSupplier<T> supplier) throws Exception {
        String originalClasspath = System.getProperty("java.class.path");
        System.setProperty("java.class.path", classpath);
        try {
            return supplier.get();
        } finally {
            System.setProperty("java.class.path", originalClasspath);
        }
    }

    private static Path fixtureClasspathRoot() throws URISyntaxException {
        return Path.of(Repository.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    private static void createJar(Path jarPath, Class<?>... types) throws Exception {
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jar.putNextEntry(new JarEntry("other/package/Ignored.class"));
            jar.write(new byte[] {0});
            jar.closeEntry();
            for (Class<?> type : types) {
                String entryName = type.getName().replace('.', '/') + ".class";
                jar.putNextEntry(new JarEntry(entryName));
                try (var input = type.getClassLoader().getResourceAsStream(entryName)) {
                    input.transferTo(jar);
                }
                jar.closeEntry();
            }
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
