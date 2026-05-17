package io.github.mundanej.mjdi.generator;

import static io.github.mundanej.mjdi.generator.GeneratedModuleRequest.ConstructorBinding.namedBinding;
import static io.github.mundanej.mjdi.generator.GeneratedModuleRequest.ConstructorBinding.binding;
import static io.github.mundanej.mjdi.generator.GeneratedModuleRequest.Dependency.named;
import static io.github.mundanej.mjdi.generator.GeneratedModuleRequest.Dependency.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class InjectionModuleSourceGeneratorTest {
    @Test
    void emitsDirectConstructorBindings() {
        GeneratedModuleRequest request = new GeneratedModuleRequest(
                "com.example.generated",
                "GeneratedAppModule",
                List.of(binding("com.example.OrderService", of("com.example.Repository"))));

        GeneratedModuleSource source = new InjectionModuleSourceGenerator().generate(request);

        assertEquals("com.example.generated", source.packageName());
        assertEquals("GeneratedAppModule", source.className());
        assertTrue(source.sourceText().contains("public final class GeneratedAppModule implements AppPluginModule"));
        assertTrue(source.sourceText()
                .contains("binder.bindSingleton(com.example.OrderService.class, context -> new com.example.OrderService("
                        + "context.get(com.example.Repository.class)));"));
    }

    @Test
    void emitsNamedBindingsAndDependencies() {
        GeneratedModuleRequest request = new GeneratedModuleRequest(
                "com.example.generated",
                "NamedModule",
                List.of(namedBinding(
                        "com.example.OrderService",
                        "primary",
                        named("com.example.Repository", "orders"))));

        String source = new InjectionModuleSourceGenerator().generate(request).sourceText();

        assertTrue(source.contains("binder.bindSingleton(Key.named(com.example.OrderService.class, \"primary\"), "
                + "context -> new com.example.OrderService("
                + "context.get(Key.named(com.example.Repository.class, \"orders\"))));"));
    }

    @Test
    void emitsNamedScalarDependenciesWithAppContextHelpers() {
        GeneratedModuleRequest request = new GeneratedModuleRequest(
                "com.example.generated",
                "ScalarModule",
                List.of(binding(
                        "com.example.ScalarConsumer",
                        named("java.lang.String", "text"),
                        named("boolean", "enabled"),
                        named("int", "count"),
                        named("long", "distance"),
                        named("double", "ratio"),
                        named("float", "scale"),
                        named("short", "small"),
                        named("byte", "tiny"),
                        named("char", "letter"))));

        String source = new InjectionModuleSourceGenerator().generate(request).sourceText();

        assertTrue(source.contains("context.getNamedString(\"text\")"));
        assertTrue(source.contains("context.getNamedBool(\"enabled\")"));
        assertTrue(source.contains("context.getNamedInt(\"count\")"));
        assertTrue(source.contains("context.getNamedLong(\"distance\")"));
        assertTrue(source.contains("context.getNamedDouble(\"ratio\")"));
        assertTrue(source.contains("context.getNamedFloat(\"scale\")"));
        assertTrue(source.contains("context.getNamedShort(\"small\")"));
        assertTrue(source.contains("context.getNamedByte(\"tiny\")"));
        assertTrue(source.contains("context.getNamedChar(\"letter\")"));
    }

    @Test
    void rejectsInvalidJavaNames() {
        assertThrows(IllegalArgumentException.class, () -> new GeneratedModuleRequest(
                "not a package", "GeneratedAppModule", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new GeneratedModuleRequest(
                "com.example.generated", "com.example.GeneratedAppModule", List.of()));
        assertThrows(IllegalArgumentException.class, () -> of("not a type"));
        assertThrows(IllegalArgumentException.class, () -> of("int"));
    }

    @Test
    void rejectsJavaKeywords() {
        assertThrows(IllegalArgumentException.class, () -> new GeneratedModuleRequest(
                "com.example.class", "GeneratedAppModule", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new GeneratedModuleRequest(
                "com.example.generated", "class", List.of()));
        assertThrows(IllegalArgumentException.class, () -> of("com.example.switch"));
    }

    @Test
    void rejectsRestrictedTypeIdentifiers() {
        assertThrows(IllegalArgumentException.class, () -> new GeneratedModuleRequest(
                "com.example.record", "record", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new GeneratedModuleRequest(
                "com.example.var", "var", List.of()));
        assertThrows(IllegalArgumentException.class, () -> of("com.example.yield"));
    }

    @Test
    void rejectsBlankNames() {
        assertThrows(IllegalArgumentException.class, () -> namedBinding("com.example.OrderService", " "));
        assertThrows(IllegalArgumentException.class, () -> named("com.example.Repository", " "));
    }
}
