package io.github.mundanej.mjdi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BootstrapAppContextTest {
  @TempDir Path tempDir;

  @Test
  void binderInstallsModulesInOrder() {
    List<String> order = new ArrayList<>();

    Binder binder =
        BootstrapAppContext.binder(
            List.of(
                b -> {
                  order.add("first");
                  b.bindInstance(Key.named(String.class, "first"), "one");
                },
                b -> {
                  order.add("second");
                  b.bindInstance(Key.named(String.class, "second"), "two");
                }));

    AppContext context = binder.build();

    assertEquals(List.of("first", "second"), order);
    assertEquals("one", context.get(Key.named(String.class, "first")));
    assertEquals("two", context.get(Key.named(String.class, "second")));
  }

  @Test
  void createBuildsReadyToUseContext() {
    AppContext context =
        BootstrapAppContext.create(List.of(binder -> binder.bindInstance(String.class, "ready")));

    assertEquals("ready", context.get(String.class));
  }

  @Test
  void explicitBootstrapRejectsNullInputs() {
    List<AppPluginModule> modulesWithNull = new ArrayList<>();
    modulesWithNull.add(null);

    assertThrows(NullPointerException.class, () -> BootstrapAppContext.binder(null));
    assertThrows(NullPointerException.class, () -> BootstrapAppContext.binder(modulesWithNull));
    assertThrows(NullPointerException.class, () -> BootstrapAppContext.create(null));
  }

  @Test
  void serviceLoadedModulesAreEmptyWhenContextLoaderHasNoProviders() throws Exception {
    try (URLClassLoader loader =
        new URLClassLoader(new java.net.URL[] {tempDir.toUri().toURL()}, null)) {
      ClassLoader original = Thread.currentThread().getContextClassLoader();
      Thread.currentThread().setContextClassLoader(loader);
      try {
        assertTrue(BootstrapAppContext.serviceLoadedModules().isEmpty());
      } finally {
        Thread.currentThread().setContextClassLoader(original);
      }
    }
  }

  @Test
  void serviceLoadedModulesComeFromContextClassLoader() throws Exception {
    writeProviderConfiguration(TestModuleProvider.class);

    withContextClassLoader(
        tempDir,
        () -> {
          List<AppPluginModule> modules = BootstrapAppContext.serviceLoadedModules();
          AppContext context = BootstrapAppContext.createFromServiceLoader();

          assertEquals(1, modules.size());
          assertEquals("service-loader", context.get(String.class));
        });
  }

  private void writeProviderConfiguration(Class<? extends ModuleProvider> providerType)
      throws Exception {
    Path servicesDirectory = tempDir.resolve("META-INF/services");
    Files.createDirectories(servicesDirectory);
    Files.writeString(
        servicesDirectory.resolve(ModuleProvider.class.getName()),
        providerType.getName() + System.lineSeparator(),
        StandardCharsets.UTF_8);
  }

  private void withContextClassLoader(Path classpathRoot, ThrowingRunnable action)
      throws Exception {
    ClassLoader original = Thread.currentThread().getContextClassLoader();
    try (URLClassLoader loader =
        new URLClassLoader(new java.net.URL[] {classpathRoot.toUri().toURL()}, original)) {
      Thread.currentThread().setContextClassLoader(loader);
      action.run();
    } finally {
      Thread.currentThread().setContextClassLoader(original);
    }
  }

  public static final class TestModuleProvider implements ModuleProvider {
    public TestModuleProvider() {}

    @Override
    public List<AppPluginModule> modules() {
      return List.of(binder -> binder.bindInstance(String.class, "service-loader"));
    }
  }

  @FunctionalInterface
  private interface ThrowingRunnable {
    void run() throws Exception;
  }
}
