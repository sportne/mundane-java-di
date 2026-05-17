package io.github.mundanej.mjdi;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Convenience methods for building an {@link AppContext}.
 *
 * <p>Use this class when an application wants a simple one-line bootstrap path instead of manually
 * creating a {@link Binder}.
 */
public final class BootstrapAppContext {
    private BootstrapAppContext() {}

    /**
     * Creates and populates a binder from explicit modules.
     *
     * @param modules the modules to install, in order
     * @return a populated binder
     */
    public static Binder binder(List<AppPluginModule> modules) {
        Binder binder = new Binder();
        for (AppPluginModule module : modules) {
            binder.install(module);
        }
        return binder;
    }

    /**
     * Creates an application context from explicit modules.
     *
     * @param modules the modules to install, in order
     * @return a ready-to-use application context
     */
    public static AppContext create(List<AppPluginModule> modules) {
        return binder(modules).build();
    }

    /**
     * Loads modules from {@link ModuleProvider} service entries.
     *
     * <p>This method executes providers visible through the application classpath's service-loader
     * configuration. Use it only with dependencies that the application trusts.
     *
     * @return all modules supplied by providers visible to the service loader
     */
    public static List<AppPluginModule> serviceLoadedModules() {
        List<AppPluginModule> modules = new ArrayList<>();
        for (ModuleProvider provider : ServiceLoader.load(ModuleProvider.class)) {
            modules.addAll(provider.modules());
        }
        return List.copyOf(modules);
    }

    /**
     * Creates an application context from service-loaded modules.
     *
     * <p>This method executes service-loaded module providers. Use it only with dependencies that
     * the application trusts.
     *
     * @return a ready-to-use application context
     */
    public static AppContext createFromServiceLoader() {
        return create(serviceLoadedModules());
    }
}
