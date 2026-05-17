package io.github.mundanej.mjdi.examples.showcase;

import io.github.mundanej.mjdi.Inject;

/** Example repository discovered through an {@link Inject} constructor. */
public final class InventoryRepository {
  /** Creates the repository. */
  @Inject
  public InventoryRepository() {}

  /**
   * Returns stock for a sample item.
   *
   * @param sku item identifier
   * @return available stock
   */
  public int stockFor(String sku) {
    return "coffee".equals(sku) ? 12 : 0;
  }
}
