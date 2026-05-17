package io.github.mundanej.mjdi.generator.fixtures.valid;

import io.github.mundanej.mjdi.Inject;
import io.github.mundanej.mjdi.Named;

@Named("consumer")
public final class NamedConsumer {
  private final NamedRepository repository;

  @Inject
  public NamedConsumer(@Named("main") NamedRepository repository) {
    this.repository = repository;
  }

  public NamedRepository repository() {
    return repository;
  }
}
