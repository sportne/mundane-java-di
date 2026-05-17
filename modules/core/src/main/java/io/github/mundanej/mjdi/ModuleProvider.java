package io.github.mundanej.mjdi;

import java.util.List;

/**
 * Supplies application modules through Java's service-loader pattern.
 *
 * <p>A library can provide this interface in {@code META-INF/services} so
 * {@link BootstrapAppContext} can discover its modules during startup.
 */
@FunctionalInterface
public interface ModuleProvider {
    /**
     * Returns the modules supplied by this provider.
     *
     * @return the modules to install into a binder
     */
    List<AppPluginModule> modules();
}
