package io.github.mundanej.mjdi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the constructor that the build-time generator should call.
 *
 * <p>The runtime core does not inspect this annotation. It exists so generator and architecture
 * tests can identify which constructor is intended for dependency injection.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
public @interface Inject {}
