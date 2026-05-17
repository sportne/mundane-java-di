package io.github.mundanej.mjdi.generator;

import static io.github.mundanej.mjdi.generator.GeneratedModuleRequest.ConstructorBinding.namedBinding;
import static io.github.mundanej.mjdi.generator.GeneratedModuleRequest.ConstructorBinding.transientBinding;
import static io.github.mundanej.mjdi.generator.GeneratedModuleRequest.Dependency.named;
import static io.github.mundanej.mjdi.generator.GeneratedModuleRequest.Dependency.of;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class GeneratedModuleSourceArchitectureTest {
    @Test
    void generatedModuleSourceAvoidsForbiddenRuntimeMechanisms() {
        String source = new InjectionModuleSourceGenerator()
                .generate(new GeneratedModuleRequest(
                        "com.example.generated",
                        "GeneratedAppModule",
                        List.of(
                                transientBinding("com.example.OrderService", of("com.example.Repository")),
                                namedBinding(
                                        "com.example.Port",
                                        "primary",
                                        named("com.example.Settings", "prod")))))
                .sourceText();

        assertFalse(source.contains("io.github.mundanej.mjdi.generator"));
        assertFalse(source.contains("ClassGraph"));
        assertFalse(source.contains("org.reflections"));
        assertFalse(source.contains("java.lang.reflect"));
        assertFalse(source.contains("java.lang.invoke"));
        assertFalse(source.contains("Class.forName"));
        assertFalse(source.contains("ClassLoader"));
        assertFalse(source.contains("URLClassLoader"));
        assertFalse(source.contains("ServiceLoader"));
        assertFalse(source.contains("ObjectInputStream"));
        assertFalse(source.contains("ObjectOutputStream"));
        assertFalse(source.contains("Serializable"));
        assertFalse(source.contains("Externalizable"));
        assertFalse(source.contains("readObject"));
        assertFalse(source.contains("writeObject"));
        assertFalse(source.contains("readResolve"));
        assertFalse(source.contains("writeReplace"));
        assertFalse(source.contains("System.exit"));
        assertFalse(source.contains("System.gc"));
        assertFalse(source.contains("ProcessBuilder"));
        assertFalse(source.contains("Runtime.getRuntime"));
        assertFalse(source.contains("javax.script"));
        assertFalse(source.contains("javax.tools"));
        assertFalse(source.contains("sun."));
        assertFalse(source.contains("jdk.internal"));
        assertFalse(source.contains("Unsafe"));
        assertFalse(source.contains("getResource"));
        assertFalse(source.contains("ResourceBundle"));
        assertFalse(source.contains("org.junit"));
        assertFalse(source.contains("com.tngtech.archunit"));

        assertTrue(source.contains("new com.example.OrderService("));
        assertTrue(source.contains("context.get(com.example.Repository.class)"));
        assertTrue(source.contains("context.get(Key.named(com.example.Settings.class, \"prod\"))"));
    }
}
