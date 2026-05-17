package io.github.mundanej.mjdi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class AppContextTest {
    @Test
    void contextUsesBindingSnapshotFromBuildTime() {
        Binder binder = new Binder();
        binder.bindInstance(String.class, "before");

        AppContext context = binder.build();
        binder.bindInstance(Integer.class, 42);

        assertEquals("before", context.get(String.class));
        assertThrows(NoSuchElementException.class, () -> context.get(Integer.class));
    }

    @Test
    void providerReceivesActualContextAndCanResolveDependencies() {
        Binder binder = new Binder();
        binder.bindInstance(Integer.class, 42);

        AppContext[] receivedContext = new AppContext[1];
        binder.bind(String.class, context -> {
            receivedContext[0] = context;
            return "value-" + context.get(Integer.class);
        });

        AppContext context = binder.build();

        assertEquals("value-42", context.get(String.class));
        assertSame(context, receivedContext[0]);
    }

    @Test
    void getsNamedScalarValues() {
        AppContext context = BootstrapAppContext.create(List.of(binder -> binder
                .bindInstance(Key.named(String.class, "text"), "alpha")
                .bindInstance(Key.named(Boolean.class, "enabled"), true)
                .bindInstance(Key.named(Integer.class, "count"), 7)
                .bindInstance(Key.named(Long.class, "distance"), 8L)
                .bindInstance(Key.named(Double.class, "ratio"), 1.5D)
                .bindInstance(Key.named(Float.class, "scale"), 2.5F)
                .bindInstance(Key.named(Short.class, "small"), (short) 3)
                .bindInstance(Key.named(Byte.class, "tiny"), (byte) 4)
                .bindInstance(Key.named(Character.class, "letter"), 'x')));

        assertEquals("alpha", context.getNamedString("text"));
        assertEquals(true, context.getNamedBool("enabled"));
        assertEquals(7, context.getNamedInt("count"));
        assertEquals(8L, context.getNamedLong("distance"));
        assertEquals(1.5D, context.getNamedDouble("ratio"));
        assertEquals(2.5F, context.getNamedFloat("scale"));
        assertEquals((short) 3, context.getNamedShort("small"));
        assertEquals((byte) 4, context.getNamedByte("tiny"));
        assertEquals('x', context.getNamedChar("letter"));
    }

    @Test
    void namedScalarLookupRejectsBlankName() {
        AppContext context = BootstrapAppContext.create(List.of());

        assertThrows(IllegalArgumentException.class, () -> context.getNamedString(" "));
    }

    @Test
    void namedScalarLookupFailsForMissingBinding() {
        AppContext context = BootstrapAppContext.create(List.of());

        assertThrows(NoSuchElementException.class, () -> context.getNamedInt("missing"));
    }

    @Test
    void missingBindingMessageIncludesKey() {
        AppContext context = BootstrapAppContext.create(List.of());
        Key<String> key = Key.named(String.class, "missing");

        NoSuchElementException exception =
                assertThrows(NoSuchElementException.class, () -> context.get(key));

        assertTrue(exception.getMessage().contains(key.toString()));
    }

    @Test
    void lookupRejectsNullArguments() {
        AppContext context = BootstrapAppContext.create(List.of());

        assertThrows(NullPointerException.class, () -> context.get((Class<String>) null));
        assertThrows(NullPointerException.class, () -> context.get((Key<String>) null));
    }

    @Test
    void providerReturningWrongRuntimeTypeThrowsClassCastException() {
        Binder binder = new Binder();
        binder.bind(String.class, wrongStringProvider());
        AppContext context = binder.build();

        assertThrows(ClassCastException.class, () -> context.get(String.class));
    }

    @SuppressWarnings("unchecked")
    private static ContextProvider<? extends String> wrongStringProvider() {
        return (ContextProvider<? extends String>) (ContextProvider<?>) ignored -> 42;
    }
}
