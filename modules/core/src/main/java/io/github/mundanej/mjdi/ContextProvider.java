package io.github.mundanej.mjdi;

/**
 * Creates or returns one object for an {@link AppContext}.
 *
 * <p>This is the small building block behind bindings. A provider can ask the context for other
 * dependencies and then return the object it is responsible for.
 *
 * @param <T> the type this provider returns
 */
@FunctionalInterface
public interface ContextProvider<T> {
  /**
   * Returns an object using the supplied application context.
   *
   * @param context the context that can provide other dependencies
   * @return the provided object
   */
  T get(AppContext context);
}
