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
    void rejectsNullRequest() {
        assertThrows(NullPointerException.class, () -> new InjectionModuleSourceGenerator().generate(null));
    }

    @Test
    void emitsEmptyConfigureForEmptyBindingList() {
        GeneratedModuleRequest request =
                new GeneratedModuleRequest("com.example.generated", "EmptyModule", List.of());

        String source = new InjectionModuleSourceGenerator().generate(request).sourceText();

        assertTrue(source.contains(sourceLines(
                "    @Override",
                "    public void configure(Binder binder) {",
                "    }",
                "}")));
        assertTrue(!source.contains("binder.bindSingleton("));
    }

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
    void preservesBindingOrder() {
        GeneratedModuleRequest request = new GeneratedModuleRequest(
                "com.example.generated",
                "OrderedModule",
                List.of(
                        binding("com.example.First"),
                        binding("com.example.Second"),
                        binding("com.example.Third")));

        String source = new InjectionModuleSourceGenerator().generate(request).sourceText();

        int firstIndex = source.indexOf("binder.bindSingleton(com.example.First.class");
        int secondIndex = source.indexOf("binder.bindSingleton(com.example.Second.class");
        int thirdIndex = source.indexOf("binder.bindSingleton(com.example.Third.class");
        assertTrue(firstIndex >= 0);
        assertTrue(secondIndex >= 0);
        assertTrue(thirdIndex >= 0);
        assertTrue(firstIndex < secondIndex);
        assertTrue(secondIndex < thirdIndex);
    }

    @Test
    void emitsNoArgumentConstructorBindingExactly() {
        GeneratedModuleRequest request = new GeneratedModuleRequest(
                "com.example.generated", "NoArgModule", List.of(binding("com.example.NoArgService")));

        String source = new InjectionModuleSourceGenerator().generate(request).sourceText();

        String expectedLine = "        binder.bindSingleton(com.example.NoArgService.class, "
                + "context -> new com.example.NoArgService());";
        assertTrue(source.lines().anyMatch(expectedLine::equals));
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
    void emitsNamedWrapperScalarDependenciesWithAppContextHelpers() {
        GeneratedModuleRequest request = new GeneratedModuleRequest(
                "com.example.generated",
                "WrapperScalarModule",
                List.of(binding(
                        "com.example.ScalarConsumer",
                        named("java.lang.Boolean", "enabled"),
                        named("java.lang.Integer", "count"),
                        named("java.lang.Long", "distance"),
                        named("java.lang.Double", "ratio"),
                        named("java.lang.Float", "scale"),
                        named("java.lang.Short", "small"),
                        named("java.lang.Byte", "tiny"),
                        named("java.lang.Character", "letter"))));

        String source = new InjectionModuleSourceGenerator().generate(request).sourceText();

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
    void escapesBindingAndDependencyNamesContainingQuoteAndBackslash() {
        GeneratedModuleRequest request = new GeneratedModuleRequest(
                "com.example.generated",
                "EscapedNamesModule",
                List.of(namedBinding(
                        "com.example.OrderService",
                        "primary\"\\binding",
                        named("com.example.Repository", "orders\"\\dependency"))));

        String source = new InjectionModuleSourceGenerator().generate(request).sourceText();

        assertTrue(source.contains("Key.named(com.example.OrderService.class, \"primary\\\"\\\\binding\")"));
        assertTrue(source.contains("Key.named(com.example.Repository.class, \"orders\\\"\\\\dependency\")"));
    }

    @Test
    void emitsPublicStaticNestedTypeOutput() {
        GeneratedModuleRequest request = new GeneratedModuleRequest(
                "com.example.generated",
                "NestedModule",
                List.of(binding("java.util.AbstractMap.SimpleEntry", of("java.util.Map.Entry"))));

        String source = new InjectionModuleSourceGenerator().generate(request).sourceText();

        assertTrue(source.contains("binder.bindSingleton(java.util.AbstractMap.SimpleEntry.class, "
                + "context -> new java.util.AbstractMap.SimpleEntry("
                + "context.get(java.util.Map.Entry.class)));"));
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

    private static String sourceLines(String... lines) {
        return String.join(System.lineSeparator(), lines);
    }
}
