package io.github.mundanej.mjdi;

import java.util.Objects;
import java.util.Optional;

/**
 * Names one thing that can be looked up from an {@link AppContext}.
 *
 * <p>A key is normally just a Java class, such as {@code Service.class}. When two bindings use the
 * same class for different purposes, the key can also include a short name.
 *
 * @param <T> the Java type returned for this key
 */
public final class Key<T> {
  private final Class<T> type;
  private final String name;

  private Key(Class<T> type, String name) {
    this.type = Objects.requireNonNull(type, "type");
    this.name = normalize(name);
  }

  /**
   * Creates an unnamed key for the given Java type.
   *
   * @param type the class to bind or look up
   * @param <T> the Java type represented by the key
   * @return a key for {@code type}
   */
  public static <T> Key<T> of(Class<T> type) {
    return new Key<>(type, null);
  }

  /**
   * Creates a named key for the given Java type.
   *
   * @param type the class to bind or look up
   * @param name the name that separates this binding from other bindings of the same type
   * @param <T> the Java type represented by the key
   * @return a named key for {@code type}
   */
  public static <T> Key<T> named(Class<T> type, String name) {
    return new Key<>(type, Objects.requireNonNull(name, "name"));
  }

  /**
   * Returns the Java class this key represents.
   *
   * @return the bound or requested class
   */
  public Class<T> type() {
    return type;
  }

  /**
   * Returns the optional name for this key.
   *
   * @return the name, or an empty optional for an unnamed key
   */
  public Optional<String> name() {
    return Optional.ofNullable(name);
  }

  /**
   * Compares this key with another object.
   *
   * @param other the object to compare
   * @return {@code true} when both keys use the same type and name
   */
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Key<?> key)) {
      return false;
    }
    return type.equals(key.type) && Objects.equals(name, key.name);
  }

  /**
   * Returns the hash code for this key.
   *
   * @return a hash code based on the type and optional name
   */
  @Override
  public int hashCode() {
    int result = type.hashCode();
    result = 31 * result + (name == null ? 0 : name.hashCode());
    return result;
  }

  /**
   * Returns a readable form of this key for error messages.
   *
   * @return the type name, with the binding name when present
   */
  @Override
  public String toString() {
    return name == null ? type.getName() : type.getName() + "[" + name + "]";
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    if (value.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    return value;
  }
}
