package io.github.mundanej.mjdi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Immutable object lookup table created from a {@link Binder}.
 *
 * <p>Application code asks the context for a class or key. The context then calls the provider that
 * was registered for that key.
 */
public final class AppContext {
    private final Map<Key<?>, ContextProvider<?>> providers;

    AppContext(Map<Key<?>, ContextProvider<?>> providers) {
        this.providers = Map.copyOf(new LinkedHashMap<>(providers));
    }

    /**
     * Returns the object bound to a class.
     *
     * @param type the class to look up
     * @param <T> the requested type
     * @return the object provided for {@code type}
     */
    public <T> T get(Class<T> type) {
        return get(Key.of(type));
    }

    /**
     * Returns the object bound to a key.
     *
     * @param key the key to look up
     * @param <T> the requested type
     * @return the object provided for {@code key}
     */
    public <T> T get(Key<T> key) {
        ContextProvider<?> provider = providers.get(key);
        if (provider == null) {
            throw new NoSuchElementException("no binding for " + key);
        }
        Object value = provider.get(this);
        return key.type().cast(value);
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
