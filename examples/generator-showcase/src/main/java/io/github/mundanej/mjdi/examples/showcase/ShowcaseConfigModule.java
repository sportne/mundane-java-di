package io.github.mundanej.mjdi.examples.showcase;

import io.github.mundanej.mjdi.AppPluginModule;
import io.github.mundanej.mjdi.Binder;
import io.github.mundanej.mjdi.Key;

/** Manual module that supplies configuration values consumed by generated bindings. */
public final class ShowcaseConfigModule implements AppPluginModule {
  /** Creates the config module. */
  public ShowcaseConfigModule() {}

  /**
   * Registers named scalar configuration values.
   *
   * @param binder binder that receives config bindings
   */
  @Override
  public void configure(Binder binder) {
    binder
        .bindInstance(Key.named(String.class, "storeName"), "Generator Coffee")
        .bindInstance(Key.named(Integer.class, "retryLimit"), 5)
        .bindInstance(Key.named(Double.class, "taxRate"), 0.0825D);
  }
}
