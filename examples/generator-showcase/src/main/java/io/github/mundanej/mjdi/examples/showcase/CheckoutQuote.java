package io.github.mundanej.mjdi.examples.showcase;

/**
 * Immutable quote returned by the showcase checkout service.
 *
 * @param storeName store name used for the quote
 * @param availableStock number of units available
 * @param totalBeforeTax total before tax is applied
 * @param taxRate tax rate used by the quote
 */
public record CheckoutQuote(
    String storeName, int availableStock, double totalBeforeTax, double taxRate) {}
