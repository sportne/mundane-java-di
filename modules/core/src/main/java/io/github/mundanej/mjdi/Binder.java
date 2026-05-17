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
    private int overrideDepth;

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
        configure(module, false);
        return this;
    }

    /**
     * Installs all bindings from a module, replacing existing bindings for matching keys.
     *
     * <p>Use this when an application intentionally wants a later module to override earlier module
     * bindings. If the override module binds the same key more than once, the last binding wins.
     *
     * @param module the module to configure in override mode
     * @return this binder for chaining
     */
    public Binder installOverride(AppPluginModule module) {
        configure(module, true);
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

    /**
     * Builds an immutable application context from the bindings registered so far.
     *
     * @return a context that can look up the current bindings
     */
    public AppContext build() {
        return new AppContext(providers);
    }

    private void configure(AppPluginModule module, boolean override) {
        Objects.requireNonNull(module, "module");
        int previousOverrideDepth = overrideDepth;
        overrideDepth = override ? overrideDepth + 1 : 0;
        try {
            module.configure(this);
        } finally {
            overrideDepth = previousOverrideDepth;
        }
    }

    private void put(Key<?> key, ContextProvider<?> provider) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(provider, "provider");
        if (overrideDepth == 0 && providers.containsKey(key)) {
            throw new IllegalStateException("duplicate binding for " + key);
        }
        providers.put(key, provider);
    }
}
