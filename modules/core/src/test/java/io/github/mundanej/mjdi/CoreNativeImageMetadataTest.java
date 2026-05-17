package io.github.mundanej.mjdi;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CoreNativeImageMetadataTest {
    @Test
    void coreDoesNotShipReachabilityMetadataWorkarounds() {
        assertFalse(Files.exists(Path.of("src/main/resources/META-INF/native-image")));
    }
}
