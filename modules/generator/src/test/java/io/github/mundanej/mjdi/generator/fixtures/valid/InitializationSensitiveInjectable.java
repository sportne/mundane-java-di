package io.github.mundanej.mjdi.generator.fixtures.valid;

import io.github.mundanej.mjdi.Inject;

/** Test fixture that records whether scanning initializes candidate classes. */
public final class InitializationSensitiveInjectable {
  static {
    System.setProperty("mjdi.initialization-sensitive-fixture.initialized", "true");
  }

  /** Creates the fixture. */
  @Inject
  public InitializationSensitiveInjectable() {}
}
