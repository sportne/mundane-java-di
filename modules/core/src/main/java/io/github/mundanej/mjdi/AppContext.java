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
}
