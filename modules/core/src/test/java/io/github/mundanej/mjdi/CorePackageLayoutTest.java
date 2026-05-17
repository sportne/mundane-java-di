package io.github.mundanej.mjdi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class CorePackageLayoutTest {
    @Test
    void productionSourcesStayInRootPackage() throws IOException {
        Path sourceRoot = Path.of("src/main/java");
        Path packageRoot = sourceRoot.resolve("io/github/mundanej/mjdi");

        List<Path> misplacedSources;
        try (var paths = Files.walk(sourceRoot)) {
            misplacedSources = paths.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.startsWith(packageRoot))
                    .toList();
        }

        assertEquals(List.of(), misplacedSources);
    }
}
