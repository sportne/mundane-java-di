package io.github.mundanej.mjdi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class AppContextTest {
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
}
