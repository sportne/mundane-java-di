package io.github.mundanej.mjdi.examples.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.github.mundanej.mjdi.AppPluginModule;
import io.github.mundanej.mjdi.BootstrapAppContext;
import io.github.mundanej.mjdi.Key;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeneratedShowcaseModuleTest {
  @Test
  void generatedModuleProvidesConfiguredSingletonGraph() throws Exception {
    AppPluginModule generatedModule = generatedModule();
    var context = BootstrapAppContext.create(List.of(new ShowcaseConfigModule(), generatedModule));

    CheckoutService firstCheckout = context.get(CheckoutService.class);
    CheckoutService secondCheckout = context.get(CheckoutService.class);
    InventoryRepository firstRepository = context.get(InventoryRepository.class);
    InventoryRepository secondRepository = context.get(InventoryRepository.class);
    ShippingRates firstRates = context.get(Key.named(ShippingRates.class, "standard"));
    ShippingRates secondRates = context.get(Key.named(ShippingRates.class, "standard"));
    CheckoutQuote quote = firstCheckout.quote("coffee");

    assertSame(firstCheckout, secondCheckout);
    assertSame(firstRepository, secondRepository);
    assertSame(firstRates, secondRates);
    assertEquals("Generator Coffee", quote.storeName());
    assertEquals(12, quote.availableStock());
    assertEquals(50.0D, quote.totalBeforeTax());
    assertEquals(0.0825D, quote.taxRate());
  }

  private static AppPluginModule generatedModule() throws Exception {
    Class<?> moduleType =
        Class.forName(
            "io.github.mundanej.mjdi.examples.showcase.generated.GeneratedShowcaseModule");
    return (AppPluginModule) moduleType.getConstructor().newInstance();
  }
}
