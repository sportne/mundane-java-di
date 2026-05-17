package io.github.mundanej.mjdi.examples.showcase;

import io.github.mundanej.mjdi.Inject;
import io.github.mundanej.mjdi.Named;

/** Example service assembled by a generated module. */
public final class CheckoutService {
  private final InventoryRepository inventoryRepository;
  private final ShippingRates shippingRates;
  private final String storeName;
  private final int retryLimit;
  private final double taxRate;

  /**
   * Creates the checkout service.
   *
   * @param inventoryRepository repository used to read stock levels
   * @param shippingRates named shipping-rate provider
   * @param storeName named display name supplied by a manual module
   * @param retryLimit named retry limit supplied by a manual module
   * @param taxRate named tax rate supplied by a manual module
   */
  @Inject
  public CheckoutService(
      InventoryRepository inventoryRepository,
      @Named("standard") ShippingRates shippingRates,
      @Named("storeName") String storeName,
      @Named("retryLimit") int retryLimit,
      @Named("taxRate") double taxRate) {
    this.inventoryRepository = inventoryRepository;
    this.shippingRates = shippingRates;
    this.storeName = storeName;
    this.retryLimit = retryLimit;
    this.taxRate = taxRate;
  }

  /**
   * Returns a deterministic checkout quote.
   *
   * @param sku inventory item to quote
   * @return checkout quote for the item
   */
  public CheckoutQuote quote(String sku) {
    int available = inventoryRepository.stockFor(sku);
    double subtotal = Math.min(available, retryLimit) * 10.0D;
    double shipping = shippingRates.amountFor(subtotal);
    return new CheckoutQuote(storeName, available, subtotal + shipping, taxRate);
  }
}
