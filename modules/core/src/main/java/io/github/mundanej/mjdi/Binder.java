package io.github.mundanej.mjdi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Collects dependency bindings before an {@link AppContext} is created.
 *
 * <p>A binding connects a {@link Key} to a provider or to an already-created instance. Once all
 * modules are installed, the binder can build an immutable context.
 */
public final class Binder {
    private final Map<Key<?>, ContextProvider<?>> providers = new LinkedHashMap<>();

    /**
     * Creates an empty binder.
     */
    public Binder() {}

    /**
     * Installs all bindings from a module.
     *
     * @param module the module to configure
     * @return this binder for chaining
     */
    public Binder install(AppPluginModule module) {
        Objects.requireNonNull(module, "module").configure(this);
        return this;
    }

    /**
     * Binds a class to a provider that runs every time the class is requested.
     *
     * @param type the class to bind
     * @param provider the provider that creates or returns the object
     * @param <T> the bound type
     * @return this binder for chaining
     */
    public <T> Binder bind(Class<T> type, ContextProvider<? extends T> provider) {
        return bind(Key.of(type), provider);
    }

    /**
     * Binds a key to a provider that runs every time the key is requested.
     *
     * @param key the key to bind
     * @param provider the provider that creates or returns the object
     * @param <T> the bound type
     * @return this binder for chaining
     */
    public <T> Binder bind(Key<T> key, ContextProvider<? extends T> provider) {
        put(key, provider);
        return this;
    }

    /**
     * Binds a class to a provider that runs once and then reuses the same value.
     *
     * @param type the class to bind
     * @param provider the provider that creates the first value
     * @param <T> the bound type
     * @return this binder for chaining
     */
    public <T> Binder bindSingleton(Class<T> type, ContextProvider<? extends T> provider) {
        return bindSingleton(Key.of(type), provider);
    }

    /**
     * Binds a key to a provider that runs once and then reuses the same value.
     *
     * @param key the key to bind
     * @param provider the provider that creates the first value
     * @param <T> the bound type
     * @return this binder for chaining
     */
    public <T> Binder bindSingleton(Key<T> key, ContextProvider<? extends T> provider) {
        put(key, Providers.singleton(provider));
        return this;
    }

    /**
     * Binds a class to an object that already exists.
     *
     * @param type the class to bind
     * @param instance the object to return for this binding
     * @param <T> the bound type
     * @return this binder for chaining
     */
    public <T> Binder bindInstance(Class<T> type, T instance) {
        return bindInstance(Key.of(type), instance);
    }

    /**
     * Binds a key to an object that already exists.
     *
     * @param key the key to bind
     * @param instance the object to return for this binding
     * @param <T> the bound type
     * @return this binder for chaining
     */
    public <T> Binder bindInstance(Key<T> key, T instance) {
        put(key, ignored -> instance);
        return this;
    }

    /**
     * Returns the keys that have already been bound.
     *
     * @return a snapshot of the bound keys
     */
    public Set<Key<?>> boundKeys() {
        return Set.copyOf(providers.keySet());
    }

    AppContext build() {
        return new AppContext(providers);
    }

    private void put(Key<?> key, ContextProvider<?> provider) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(provider, "provider");
        if (providers.containsKey(key)) {
            throw new IllegalStateException("duplicate binding for " + key);
        }
        providers.put(key, provider);
    }
}
