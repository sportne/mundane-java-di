package io.github.mundanej.mjdi.examples;

import io.github.mundanej.mjdi.AppPluginModule;
import io.github.mundanej.mjdi.Binder;
import io.github.mundanej.mjdi.Inject;

/**
 * Small example module that shows how to register and request dependencies.
 *
 * <p>The example binds a {@link Repository} and a {@link Service}. The service asks the context for
 * its repository dependency.
 */
public final class ExampleAppModule implements AppPluginModule {
  /** Creates the example module. */
  public ExampleAppModule() {}

  /**
   * Registers the example repository and service bindings.
   *
   * @param binder the binder that receives the example bindings
   */
  @Override
  public void configure(Binder binder) {
    binder.bind(Repository.class, ignored -> new Repository());
    binder.bind(Service.class, context -> new Service(context.get(Repository.class)));
  }

  /** Example dependency that returns a fixed value. */
  public static final class Repository {
    /** Creates a repository. */
    @Inject
    public Repository() {}

    /**
     * Returns the example value.
     *
     * @return the fixed example text
     */
    public String value() {
      return "example";
    }
  }

  /** Example service that depends on {@link Repository}. */
  public static final class Service {
    private final Repository repository;

    /**
     * Creates a service with its repository dependency.
     *
     * @param repository the repository used by this service
     */
    @Inject
    public Service(Repository repository) {
      this.repository = repository;
    }

    /**
     * Returns the value read through the repository.
     *
     * @return the repository value
     */
    public String value() {
      return repository.value();
    }
  }
}
