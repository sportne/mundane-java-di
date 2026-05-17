package io.github.mundanej.mjdi;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Immutable object lookup table created from a {@link Binder}.
 *
 * <p>Application code asks the context for a class or key. The context then calls the provider that
 * was registered for that key. A context is immutable and safe to share between threads after it is
 * built. Providers still need to be safe for the way the application uses them.
 */
public final class AppContext {
  private final Map<Key<?>, ContextProvider<?>> providers;
  private final Map<Class<?>, ContextProvider<?>> unnamedProviders;
  private final Map<Class<?>, Key<?>> unnamedKeys;
  private final ThreadLocal<Deque<Key<?>>> resolving = ThreadLocal.withInitial(ArrayDeque::new);

  AppContext(Map<Key<?>, ContextProvider<?>> providers) {
    Map<Key<?>, ContextProvider<?>> providerCopy = Map.copyOf(new LinkedHashMap<>(providers));
    Map<Class<?>, ContextProvider<?>> unnamedProviderCopy = new LinkedHashMap<>();
    Map<Class<?>, Key<?>> unnamedKeyCopy = new LinkedHashMap<>();
    for (Map.Entry<Key<?>, ContextProvider<?>> entry : providerCopy.entrySet()) {
      if (entry.getKey().name().isEmpty()) {
        unnamedProviderCopy.put(entry.getKey().type(), entry.getValue());
        unnamedKeyCopy.put(entry.getKey().type(), entry.getKey());
      }
    }
    this.providers = providerCopy;
    this.unnamedProviders = Map.copyOf(unnamedProviderCopy);
    this.unnamedKeys = Map.copyOf(unnamedKeyCopy);
  }

  /**
   * Returns the object bound to a class.
   *
   * @param type the class to look up
   * @param <T> the requested type
   * @return the object provided for {@code type}
   */
  public <T> T get(Class<T> type) {
    Objects.requireNonNull(type, "type");
    ContextProvider<?> provider = unnamedProviders.get(type);
    if (provider == null) {
      throw new NoSuchElementException("no binding for " + type.getName());
    }
    @SuppressWarnings("unchecked")
    Key<T> key = (Key<T>) unnamedKeys.get(type);
    return resolve(key, provider);
  }

  /**
   * Returns the object bound to a key.
   *
   * @param key the key to look up
   * @param <T> the requested type
   * @return the object provided for {@code key}
   */
  public <T> T get(Key<T> key) {
    Objects.requireNonNull(key, "key");
    ContextProvider<?> provider = providers.get(key);
    if (provider == null) {
      throw new NoSuchElementException("no binding for " + key);
    }
    return resolve(key, provider);
  }

  private <T> T resolve(Key<T> key, ContextProvider<?> provider) {
    Deque<Key<?>> stack = resolving.get();
    if (stack.contains(key)) {
      throw new IllegalStateException("dependency cycle detected: " + cyclePath(stack, key));
    }
    stack.addLast(key);
    try {
      Object value = provider.get(this);
      if (value == null) {
        throw new IllegalStateException(
            "provider returned null for "
                + key
                + "; bind Optional or a domain value for nullable data");
      }
      return key.type().cast(value);
    } finally {
      stack.removeLast();
      if (stack.isEmpty()) {
        resolving.remove();
      }
    }
  }

  private static String cyclePath(Deque<Key<?>> stack, Key<?> repeatedKey) {
    List<Key<?>> path = new ArrayList<>();
    boolean inCycle = false;
    for (Key<?> key : stack) {
      if (key.equals(repeatedKey)) {
        inCycle = true;
      }
      if (inCycle) {
        path.add(key);
      }
    }
    path.add(repeatedKey);
    return String.join(" -> ", path.stream().map(Key::toString).toList());
  }

  /**
   * Returns the named {@link String} value.
   *
   * <p>This is a shortcut for {@code get(Key.named(String.class, name))}. Use it for small named
   * configuration values such as file names, mode names, or labels.
   *
   * @param name the binding name to look up
   * @return the named string value
   */
  public String getNamedString(String name) {
    return get(Key.named(String.class, name));
  }

  /**
   * Returns the named boolean value.
   *
   * <p>This is a shortcut for {@code get(Key.named(Boolean.class, name))}. The stored value is a
   * {@link Boolean}, and Java unboxes it to {@code boolean} for the caller.
   *
   * @param name the binding name to look up
   * @return the named boolean value
   */
  public boolean getNamedBool(String name) {
    return get(Key.named(Boolean.class, name));
  }

  /**
   * Returns the named integer value.
   *
   * <p>This is a shortcut for {@code get(Key.named(Integer.class, name))}. The stored value is an
   * {@link Integer}, and Java unboxes it to {@code int} for the caller.
   *
   * @param name the binding name to look up
   * @return the named integer value
   */
  public int getNamedInt(String name) {
    return get(Key.named(Integer.class, name));
  }

  /**
   * Returns the named long integer value.
   *
   * <p>This is a shortcut for {@code get(Key.named(Long.class, name))}. The stored value is a
   * {@link Long}, and Java unboxes it to {@code long} for the caller.
   *
   * @param name the binding name to look up
   * @return the named long value
   */
  public long getNamedLong(String name) {
    return get(Key.named(Long.class, name));
  }

  /**
   * Returns the named double-precision decimal value.
   *
   * <p>This is a shortcut for {@code get(Key.named(Double.class, name))}. The stored value is a
   * {@link Double}, and Java unboxes it to {@code double} for the caller.
   *
   * @param name the binding name to look up
   * @return the named double value
   */
  public double getNamedDouble(String name) {
    return get(Key.named(Double.class, name));
  }

  /**
   * Returns the named single-precision decimal value.
   *
   * <p>This is a shortcut for {@code get(Key.named(Float.class, name))}. The stored value is a
   * {@link Float}, and Java unboxes it to {@code float} for the caller.
   *
   * @param name the binding name to look up
   * @return the named float value
   */
  public float getNamedFloat(String name) {
    return get(Key.named(Float.class, name));
  }

  /**
   * Returns the named short integer value.
   *
   * <p>This is a shortcut for {@code get(Key.named(Short.class, name))}. The stored value is a
   * {@link Short}, and Java unboxes it to {@code short} for the caller.
   *
   * @param name the binding name to look up
   * @return the named short value
   */
  public short getNamedShort(String name) {
    return get(Key.named(Short.class, name));
  }

  /**
   * Returns the named byte value.
   *
   * <p>This is a shortcut for {@code get(Key.named(Byte.class, name))}. The stored value is a
   * {@link Byte}, and Java unboxes it to {@code byte} for the caller.
   *
   * @param name the binding name to look up
   * @return the named byte value
   */
  public byte getNamedByte(String name) {
    return get(Key.named(Byte.class, name));
  }

  /**
   * Returns the named character value.
   *
   * <p>This is a shortcut for {@code get(Key.named(Character.class, name))}. The stored value is a
   * {@link Character}, and Java unboxes it to {@code char} for the caller.
   *
   * @param name the binding name to look up
   * @return the named character value
   */
  public char getNamedChar(String name) {
    return get(Key.named(Character.class, name));
  }
}
