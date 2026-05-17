package io.github.mundanej.mjdi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class BinderTest {
    @Test
    void bindsTransientProvider() {
        AppContext context = BootstrapAppContext.create(List.of(
                binder -> binder.bind(Widget.class, ignored -> new Widget("one"))));

        Widget first = context.get(Widget.class);
        Widget second = context.get(Widget.class);

        assertEquals("one", first.name());
        assertNotSame(first, second);
    }

    @Test
    void bindsSingletonProvider() {
        AppContext context = BootstrapAppContext.create(List.of(
                binder -> binder.bindSingleton(Widget.class, ignored -> new Widget("single"))));

        assertSame(context.get(Widget.class), context.get(Widget.class));
    }

    @Test
    void bindsInstancesAndNamedKeys() {
        Widget widget = new Widget("named");
        Key<Widget> key = Key.named(Widget.class, "primary");

        Binder binder = BootstrapAppContext.binder(List.of(b -> b.bindInstance(key, widget)));
        AppContext context = binder.build();

        assertSame(widget, context.get(key));
        assertTrue(binder.boundKeys().contains(key));
        assertEquals("io.github.mundanej.mjdi.BinderTest$Widget[primary]", key.toString());
        assertEquals("primary", key.name().orElseThrow());
    }

    @Test
    void failsForDuplicateBindings() {
        AppPluginModule duplicate = binder -> {
            binder.bind(Widget.class, ignored -> new Widget("first"));
            binder.bind(Widget.class, ignored -> new Widget("second"));
        };

        assertThrows(IllegalStateException.class, () -> BootstrapAppContext.create(List.of(duplicate)));
    }

    @Test
    void normalInstallFailsWhenLaterModuleBindsSameKey() {
        AppPluginModule first = binder -> binder.bindInstance(Widget.class, new Widget("first"));
        AppPluginModule second = binder -> binder.bindInstance(Widget.class, new Widget("second"));

        assertThrows(IllegalStateException.class, () -> BootstrapAppContext.create(List.of(first, second)));
    }

    @Test
    void overrideInstallReplacesExistingBinding() {
        Binder binder = new Binder()
                .install(b -> b.bindInstance(Widget.class, new Widget("first")))
                .installOverride(b -> b.bindInstance(Widget.class, new Widget("second")));

        AppContext context = binder.build();

        assertEquals("second", context.get(Widget.class).name());
    }

    @Test
    void overrideInstallUsesLastBindingForDuplicateKey() {
        Binder binder = new Binder().installOverride(b -> {
            b.bindInstance(Widget.class, new Widget("first"));
            b.bindInstance(Widget.class, new Widget("second"));
        });

        AppContext context = binder.build();

        assertEquals("second", context.get(Widget.class).name());
    }

    @Test
    void normalStrictBindingReturnsAfterOverrideInstall() {
        Binder binder = new Binder()
                .install(b -> b.bindInstance(Widget.class, new Widget("first")))
                .installOverride(b -> b.bindInstance(Widget.class, new Widget("second")));

        assertThrows(IllegalStateException.class, () -> binder.install(
                b -> b.bindInstance(Widget.class, new Widget("third"))));
        assertEquals("second", binder.build().get(Widget.class).name());
    }

    @Test
    void normalStrictBindingReturnsWhenOverrideInstallThrows() {
        Binder binder = new Binder().install(b -> b.bindInstance(Widget.class, new Widget("first")));
        RuntimeException failure = new RuntimeException("failed");

        assertSame(failure, assertThrows(RuntimeException.class, () -> binder.installOverride(b -> {
            b.bindInstance(Widget.class, new Widget("second"));
            throw failure;
        })));
        assertThrows(IllegalStateException.class, () -> binder.install(
                b -> b.bindInstance(Widget.class, new Widget("third"))));
        assertEquals("second", binder.build().get(Widget.class).name());
    }

    @Test
    void failsForMissingBinding() {
        AppContext context = BootstrapAppContext.create(List.of());

        assertThrows(NoSuchElementException.class, () -> context.get(Widget.class));
    }

    @Test
    void rejectsBlankNamedKey() {
        assertThrows(IllegalArgumentException.class, () -> Key.named(Widget.class, " "));
    }

    @Test
    void serviceLoadedModulesAreEmptyWhenNoProviderIsRegistered() {
        assertTrue(BootstrapAppContext.serviceLoadedModules().isEmpty());
    }

    record Widget(String name) {}
}
