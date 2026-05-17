package io.github.mundanej.mjdi.generator;

import static io.github.mundanej.mjdi.generator.GeneratedModuleRequest.ConstructorBinding.binding;
import static io.github.mundanej.mjdi.generator.GeneratedModuleRequest.ConstructorBinding.namedBinding;
import static io.github.mundanej.mjdi.generator.GeneratedModuleRequest.Dependency.named;
import static io.github.mundanej.mjdi.generator.GeneratedModuleRequest.Dependency.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@SuppressWarnings("BadImport")
class GeneratedModuleRequestTest {
  @Test
  void rejectsNullRecordInputs() {
    assertThrows(
        NullPointerException.class,
        () -> new GeneratedModuleRequest(null, "GeneratedAppModule", List.of()));
    assertThrows(
        NullPointerException.class,
        () -> new GeneratedModuleRequest("com.example.generated", null, List.of()));
    assertThrows(
        NullPointerException.class,
        () -> new GeneratedModuleRequest("com.example.generated", "GeneratedAppModule", null));
    assertThrows(
        NullPointerException.class,
        () ->
            new GeneratedModuleRequest(
                "com.example.generated", "GeneratedAppModule", listWithNull()));

    assertThrows(
        NullPointerException.class,
        () -> new GeneratedModuleRequest.ConstructorBinding(null, Optional.empty(), List.of()));
    assertThrows(
        NullPointerException.class,
        () ->
            new GeneratedModuleRequest.ConstructorBinding("com.example.Service", null, List.of()));
    assertThrows(
        NullPointerException.class,
        () ->
            new GeneratedModuleRequest.ConstructorBinding(
                "com.example.Service", Optional.empty(), null));
    assertThrows(
        NullPointerException.class,
        () ->
            new GeneratedModuleRequest.ConstructorBinding(
                "com.example.Service", Optional.empty(), listWithNull()));

    assertThrows(
        NullPointerException.class,
        () -> new GeneratedModuleRequest.Dependency(null, Optional.empty()));
    assertThrows(
        NullPointerException.class,
        () -> new GeneratedModuleRequest.Dependency("com.example.Repository", null));
  }

  @Test
  void rejectsNullFactoryInputs() {
    assertThrows(NullPointerException.class, () -> binding(null));
    assertThrows(
        NullPointerException.class,
        () -> binding("com.example.Service", (GeneratedModuleRequest.Dependency[]) null));
    assertThrows(
        NullPointerException.class,
        () ->
            binding(
                "com.example.Service",
                listWithNull().toArray(GeneratedModuleRequest.Dependency[]::new)));

    assertThrows(NullPointerException.class, () -> namedBinding(null, "primary"));
    assertThrows(NullPointerException.class, () -> namedBinding("com.example.Service", null));
    assertThrows(
        NullPointerException.class,
        () ->
            namedBinding(
                "com.example.Service", "primary", (GeneratedModuleRequest.Dependency[]) null));
    assertThrows(
        NullPointerException.class,
        () ->
            namedBinding(
                "com.example.Service",
                "primary",
                listWithNull().toArray(GeneratedModuleRequest.Dependency[]::new)));

    assertThrows(NullPointerException.class, () -> of(null));
    assertThrows(NullPointerException.class, () -> named(null, "primary"));
    assertThrows(NullPointerException.class, () -> named("com.example.Repository", null));
  }

  @Test
  void defensivelyCopiesBindings() {
    List<GeneratedModuleRequest.ConstructorBinding> bindings = new ArrayList<>();
    GeneratedModuleRequest.ConstructorBinding serviceBinding = binding("com.example.Service");
    bindings.add(serviceBinding);

    GeneratedModuleRequest request =
        new GeneratedModuleRequest("com.example.generated", "GeneratedAppModule", bindings);
    bindings.clear();

    assertEquals(List.of(serviceBinding), request.bindings());
    assertThrows(
        UnsupportedOperationException.class,
        () -> request.bindings().add(binding("com.example.OtherService")));
  }

  @Test
  void defensivelyCopiesDependencies() {
    GeneratedModuleRequest.Dependency repository = of("com.example.Repository");
    List<GeneratedModuleRequest.Dependency> dependencies = new ArrayList<>();
    dependencies.add(repository);

    GeneratedModuleRequest.ConstructorBinding serviceBinding =
        new GeneratedModuleRequest.ConstructorBinding(
            "com.example.Service", Optional.empty(), dependencies);
    dependencies.clear();

    assertEquals(List.of(repository), serviceBinding.dependencies());
    assertThrows(
        UnsupportedOperationException.class,
        () -> serviceBinding.dependencies().add(of("com.example.OtherRepository")));
  }

  @Test
  void acceptsNamedScalarPrimitiveDependencies() {
    assertEquals(
        named("boolean", "flag"),
        new GeneratedModuleRequest.Dependency("boolean", Optional.of("flag")));
    assertEquals(
        named("byte", "tiny"), new GeneratedModuleRequest.Dependency("byte", Optional.of("tiny")));
    assertEquals(
        named("char", "letter"),
        new GeneratedModuleRequest.Dependency("char", Optional.of("letter")));
    assertEquals(
        named("double", "ratio"),
        new GeneratedModuleRequest.Dependency("double", Optional.of("ratio")));
    assertEquals(
        named("float", "scale"),
        new GeneratedModuleRequest.Dependency("float", Optional.of("scale")));
    assertEquals(
        named("int", "count"), new GeneratedModuleRequest.Dependency("int", Optional.of("count")));
    assertEquals(
        named("long", "distance"),
        new GeneratedModuleRequest.Dependency("long", Optional.of("distance")));
    assertEquals(
        named("short", "small"),
        new GeneratedModuleRequest.Dependency("short", Optional.of("small")));
  }

  @Test
  void rejectsInvalidModulePackageNames() {
    for (String packageName : invalidQualifiedJavaNames()) {
      assertThrows(
          IllegalArgumentException.class,
          () -> new GeneratedModuleRequest(packageName, "GeneratedAppModule", List.of()),
          packageName);
    }
  }

  @Test
  void rejectsInvalidModuleClassNames() {
    for (String className : invalidSimpleTypeNames()) {
      assertThrows(
          IllegalArgumentException.class,
          () -> new GeneratedModuleRequest("com.example.generated", className, List.of()),
          className);
    }
  }

  @Test
  void rejectsInvalidBindingTypeNames() {
    for (String typeName : invalidQualifiedTypeNames()) {
      assertThrows(IllegalArgumentException.class, () -> binding(typeName), typeName);
    }
  }

  @Test
  void rejectsInvalidDependencyTypeNames() {
    for (String typeName : invalidQualifiedTypeNames()) {
      assertThrows(IllegalArgumentException.class, () -> of(typeName), typeName);
    }
    assertThrows(IllegalArgumentException.class, () -> of("int"));
  }

  @Test
  void rejectsBlankBindingAndDependencyNames() {
    for (String name : List.of("", " ", "\t\n")) {
      assertThrows(
          IllegalArgumentException.class, () -> namedBinding("com.example.Service", name), name);
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new GeneratedModuleRequest.ConstructorBinding(
                  "com.example.Service", Optional.of(name), List.of()),
          name);

      assertThrows(
          IllegalArgumentException.class, () -> named("com.example.Repository", name), name);
      assertThrows(
          IllegalArgumentException.class,
          () -> new GeneratedModuleRequest.Dependency("com.example.Repository", Optional.of(name)),
          name);
    }
  }

  private static List<String> invalidQualifiedJavaNames() {
    return List.of(
        "",
        ".com.example",
        "com.example.",
        "com..example",
        "com.1example",
        "com.example.bad-name",
        "com.example._");
  }

  private static List<String> invalidSimpleTypeNames() {
    return List.of(
        "",
        ".GeneratedAppModule",
        "GeneratedAppModule.",
        "Generated..AppModule",
        "1GeneratedAppModule",
        "Generated-AppModule",
        "_",
        "record",
        "var",
        "yield");
  }

  private static List<String> invalidQualifiedTypeNames() {
    return List.of(
        "",
        ".com.example.Service",
        "com.example.Service.",
        "com..example.Service",
        "com.1example.Service",
        "com.example.Bad-Service",
        "com.example._",
        "record",
        "var",
        "yield",
        "com.example.record",
        "com.example.var",
        "com.example.yield");
  }

  private static <T> List<T> listWithNull() {
    List<T> values = new ArrayList<>();
    values.add(null);
    return values;
  }
}
