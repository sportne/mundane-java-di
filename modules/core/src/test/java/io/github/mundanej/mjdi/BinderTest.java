package io.github.mundanej.mjdi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
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
    void rejectsNullInputs() {
        Binder binder = new Binder();
        ContextProvider<Widget> provider = ignored -> new Widget("value");

        assertThrows(NullPointerException.class, () -> binder.install(null));
        assertThrows(NullPointerException.class, () -> binder.installOverride(null));
        assertThrows(NullPointerException.class, () -> binder.bind((Class<Widget>) null, provider));
        assertThrows(NullPointerException.class, () -> binder.bind((Key<Widget>) null, provider));
        assertThrows(NullPointerException.class, () -> binder.bind(Widget.class, null));
        assertThrows(NullPointerException.class, () -> binder.bindSingleton((Class<Widget>) null, provider));
        assertThrows(NullPointerException.class, () -> binder.bindSingleton((Key<Widget>) null, provider));
        assertThrows(NullPointerException.class, () -> binder.bindSingleton(Widget.class, null));
        assertThrows(NullPointerException.class, () -> binder.bindInstance((Class<Widget>) null, new Widget("value")));
        assertThrows(NullPointerException.class, () -> binder.bindInstance((Key<Widget>) null, new Widget("value")));
        assertThrows(NullPointerException.class, () -> binder.bindInstance(Widget.class, null));
        assertThrows(NullPointerException.class, () -> binder.bindInstance(Key.of(Widget.class), null));
    }

    @Test
    void bindsNamedTransientProvider() {
        Key<Widget> key = Key.named(Widget.class, "primary");
        AppContext context = new Binder()
                .bind(key, ignored -> new Widget("named"))
                .build();

        Widget first = context.get(key);
        Widget second = context.get(key);

        assertEquals("named", first.name());
        assertNotSame(first, second);
    }

    @Test
    void bindsNamedSingletonProvider() {
        Key<Widget> key = Key.named(Widget.class, "primary");
        AppContext context = new Binder()
                .bindSingleton(key, ignored -> new Widget("named-single"))
                .build();

        assertSame(context.get(key), context.get(key));
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
    void boundKeysReturnsImmutableSnapshot() {
        Key<Widget> firstKey = Key.named(Widget.class, "first");
        Key<Widget> secondKey = Key.named(Widget.class, "second");
        Binder binder = new Binder().bindInstance(firstKey, new Widget("first"));

        Set<Key<?>> snapshot = binder.boundKeys();

        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(secondKey));

        binder.bindInstance(secondKey, new Widget("second"));

        assertTrue(snapshot.contains(firstKey));
        assertFalse(snapshot.contains(secondKey));
        assertTrue(binder.boundKeys().contains(secondKey));
    }

    @Test
    void builtContextIsIndependentFromLaterBinderMutations() {
        Key<Widget> lateKey = Key.named(Widget.class, "late");
        Binder binder = new Binder().bindInstance(Widget.class, new Widget("first"));
        AppContext firstContext = binder.build();

        binder.installOverride(b -> b.bindInstance(Widget.class, new Widget("second")));
        binder.bindInstance(lateKey, new Widget("late"));

        assertEquals("first", firstContext.get(Widget.class).name());
        assertThrows(NoSuchElementException.class, () -> firstContext.get(lateKey));
        assertEquals("second", binder.build().get(Widget.class).name());
        assertEquals("late", binder.build().get(lateKey).name());
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
    void failedOverrideInstallRestoresPreviousBindings() {
        Binder binder = new Binder().install(b -> b.bindInstance(Widget.class, new Widget("first")));
        RuntimeException failure = new RuntimeException("failed");

        assertSame(failure, assertThrows(RuntimeException.class, () -> binder.installOverride(b -> {
            b.bindInstance(Widget.class, new Widget("second"));
            throw failure;
        })));
        assertThrows(IllegalStateException.class, () -> binder.install(
                b -> b.bindInstance(Widget.class, new Widget("third"))));
        assertEquals("first", binder.build().get(Widget.class).name());
    }

    @Test
    void failedNormalInstallRestoresPreviousBindings() {
        Binder binder = new Binder().install(b -> b.bindInstance(Widget.class, new Widget("first")));
        RuntimeException failure = new RuntimeException("failed");
        Key<Widget> secondKey = Key.named(Widget.class, "second");

        assertSame(failure, assertThrows(RuntimeException.class, () -> binder.install(b -> {
            b.bindInstance(secondKey, new Widget("second"));
            throw failure;
        })));

        AppContext context = binder.build();
        assertEquals("first", context.get(Widget.class).name());
        assertThrows(NoSuchElementException.class, () -> context.get(secondKey));
    }

    @Test
    void nestedOverrideInstallReplacesBindingWithinNormalInstall() {
        Binder binder = new Binder().install(b -> {
            b.bindInstance(Widget.class, new Widget("first"));
            b.installOverride(override -> override.bindInstance(Widget.class, new Widget("second")));
        });

        assertEquals("second", binder.build().get(Widget.class).name());
    }

    @Test
    void nestedNormalInstallIsStrictWithinOverrideInstall() {
        Binder binder = new Binder().install(b -> b.bindInstance(Widget.class, new Widget("first")));

        assertThrows(IllegalStateException.class, () -> binder.installOverride(override -> {
            override.bindInstance(Widget.class, new Widget("second"));
            override.install(normal -> normal.bindInstance(Widget.class, new Widget("third")));
        }));
        assertEquals("first", binder.build().get(Widget.class).name());
    }

    @Test
    void caughtNestedInstallFailureRollsBackOnlyNestedInstall() {
        Key<Widget> nestedKey = Key.named(Widget.class, "nested");
        Key<Widget> outerKey = Key.named(Widget.class, "outer");
        Binder binder = new Binder().install(b -> {
            b.bindInstance(Widget.class, new Widget("first"));
            try {
                b.install(nested -> {
                    nested.bindInstance(nestedKey, new Widget("nested"));
                    nested.bindInstance(Widget.class, new Widget("duplicate"));
                });
            } catch (IllegalStateException ignored) {
                b.bindInstance(outerKey, new Widget("outer"));
            }
        });

        AppContext context = binder.build();

        assertEquals("first", context.get(Widget.class).name());
        assertEquals("outer", context.get(outerKey).name());
        assertThrows(NoSuchElementException.class, () -> context.get(nestedKey));
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
