package io.github.mundanej.mjdi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ApiAnnotationTest {
  @Test
  void injectHasRuntimeRetentionAndConstructorTarget() throws NoSuchMethodException {
    assertEquals(RetentionPolicy.RUNTIME, Inject.class.getAnnotation(Retention.class).value());
    assertEquals(Set.of(ElementType.CONSTRUCTOR), targetsOf(Inject.class));

    Constructor<Fixture> constructor = Fixture.class.getDeclaredConstructor(String.class);

    assertNotNull(constructor.getAnnotation(Inject.class));
    assertEquals("dependency", new Fixture("dependency").dependency());
  }

  @Test
  void namedHasRuntimeRetentionAndTypeAndParameterTargets() throws NoSuchMethodException {
    assertEquals(RetentionPolicy.RUNTIME, Named.class.getAnnotation(Retention.class).value());
    assertEquals(Set.of(ElementType.TYPE, ElementType.PARAMETER), targetsOf(Named.class));

    Named typeName = Fixture.class.getAnnotation(Named.class);
    Named parameterName =
        Fixture.class
            .getDeclaredConstructor(String.class)
            .getParameters()[0]
            .getAnnotation(Named.class);

    assertEquals("fixture", typeName.value());
    assertEquals("dependency", parameterName.value());
  }

  @Test
  void namedValueIsARequiredStringMember() throws NoSuchMethodException {
    Method value = Named.class.getDeclaredMethod("value");

    assertEquals(String.class, value.getReturnType());
    assertNull(value.getDefaultValue());
  }

  private static Set<ElementType> targetsOf(Class<?> annotationType) {
    return Set.copyOf(Arrays.asList(annotationType.getAnnotation(Target.class).value()));
  }

  @Named("fixture")
  private static final class Fixture {
    private final String dependency;

    @Inject
    Fixture(@Named("dependency") String dependency) {
      this.dependency = dependency;
    }

    private String dependency() {
      return dependency;
    }
  }
}
