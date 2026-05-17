package io.github.mundanej.mjdi;

import java.util.Objects;

/**
 * Helper methods for working with {@link ContextProvider} instances.
 */
public final class Providers {
    private Providers() {}

    /**
     * Wraps a provider so it creates its object only once.
     *
     * <p>The first call asks the wrapped provider for a value. Later calls return that same value.
     *
     * @param provider the provider to wrap
     * @param <T> the provided type
     * @return a provider that memoizes its first result
     */
    public static <T> ContextProvider<T> singleton(ContextProvider<T> provider) {
        Objects.requireNonNull(provider, "provider");
        return new ContextProvider<>() {
            private volatile boolean initialized;
            private T value;

            @Override
            public T get(AppContext context) {
                if (!initialized) {
                    synchronized (this) {
                        if (!initialized) {
                            value = provider.get(context);
                            initialized = true;
                        }
                    }
                }
                return value;
            }
        };
    }
}
