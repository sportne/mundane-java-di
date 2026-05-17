package io.github.mundanej.mjdi;

/**
 * Adds a group of bindings to a {@link Binder}.
 *
 * <p>Applications can split their dependency setup into modules so each feature owns the bindings
 * it needs.
 */
@FunctionalInterface
public interface AppPluginModule {
  /**
   * Registers this module's bindings.
   *
   * @param binder the binder that collects providers and instances
   */
  void configure(Binder binder);
}
