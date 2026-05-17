package io.github.mundanej.mjdi.examples.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mundanej.mjdi.generator.ClasspathInjectableScanner;
import io.github.mundanej.mjdi.generator.GeneratedModuleRequest;
import io.github.mundanej.mjdi.generator.GeneratedModuleSource;
import io.github.mundanej.mjdi.generator.InjectionModuleSourceGenerator;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProgrammaticGeneratorApiTest {
  @Test
  void scannerAndSourceGeneratorCanBeCalledFromBuildTooling() {
    GeneratedModuleRequest request =
        new ClasspathInjectableScanner()
            .scan(
                new ClasspathInjectableScanner.ScanRequest(
                    "io.github.mundanej.mjdi.examples.showcase.generated",
                    "ProgrammaticShowcaseModule",
                    List.of("io.github.mundanej.mjdi.examples.showcase"),
                    List.of(codeSource(CheckoutService.class))));

    GeneratedModuleSource source = new InjectionModuleSourceGenerator().generate(request);

    assertEquals(3, request.bindings().size());
    assertEquals("ProgrammaticShowcaseModule", source.className());
    assertTrue(source.sourceText().contains("new CheckoutService("));
    assertTrue(source.sourceText().contains("Key.named(ShippingRates.class, \"standard\")"));
    assertTrue(source.sourceText().contains("context.getNamedString(\"storeName\")"));
    assertTrue(source.sourceText().contains("context.getNamedInt(\"retryLimit\")"));
    assertTrue(source.sourceText().contains("context.getNamedDouble(\"taxRate\")"));
  }

  private static Path codeSource(Class<?> type) {
    URI uri = URI.create(type.getProtectionDomain().getCodeSource().getLocation().toString());
    return Path.of(uri);
  }
}
