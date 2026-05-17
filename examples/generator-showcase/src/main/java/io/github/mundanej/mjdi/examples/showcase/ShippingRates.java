package io.github.mundanej.mjdi.examples.showcase;

import io.github.mundanej.mjdi.Inject;
import io.github.mundanej.mjdi.Named;

/** Named concrete dependency used by the generated checkout service binding. */
@Named("standard")
public final class ShippingRates {
  /** Creates the named shipping-rate provider. */
  @Inject
  public ShippingRates() {}

  /**
   * Calculates shipping for a subtotal.
   *
   * @param subtotal total before shipping
   * @return shipping amount
   */
  public double amountFor(double subtotal) {
    return subtotal >= 50.0D ? 0.0D : 4.5D;
  }
}
