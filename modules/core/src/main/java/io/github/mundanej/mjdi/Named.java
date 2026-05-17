package io.github.mundanej.mjdi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Gives an injectable class or constructor parameter a binding name.
 *
 * <p>Names are useful when more than one object has the same Java type. The runtime core does not
 * inspect this annotation; generated modules turn it into explicit {@link Key#named(Class, String)}
 * calls.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PARAMETER})
public @interface Named {
  /**
   * Returns the binding name.
   *
   * @return the name used by generated binding keys
   */
  String value();
}
