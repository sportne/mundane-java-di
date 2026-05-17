package io.github.mundanej.mjdi.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GeneratedModuleSourceTest {
    @Test
    void exposesSourceComponents() {
        GeneratedModuleSource source =
                new GeneratedModuleSource("com.example.generated", "GeneratedAppModule", "final class GeneratedAppModule {}");

        assertEquals("com.example.generated", source.packageName());
        assertEquals("GeneratedAppModule", source.className());
        assertEquals("final class GeneratedAppModule {}", source.sourceText());
    }

    @Test
    void usesRecordEqualityHashCodeAndToString() {
        GeneratedModuleSource first =
                new GeneratedModuleSource("com.example.generated", "GeneratedAppModule", "final class GeneratedAppModule {}");
        GeneratedModuleSource second =
                new GeneratedModuleSource("com.example.generated", "GeneratedAppModule", "final class GeneratedAppModule {}");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertTrue(first.toString().contains("packageName=com.example.generated"));
        assertTrue(first.toString().contains("className=GeneratedAppModule"));
        assertTrue(first.toString().contains("sourceText=final class GeneratedAppModule {}"));
    }
}
