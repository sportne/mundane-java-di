package io.github.mundanej.mjdi.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mundanej.mjdi.BootstrapAppContext;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExampleAppModuleTest {
    @Test
    void moduleProvidesService() {
        ExampleAppModule.Service service = BootstrapAppContext.create(List.of(new ExampleAppModule()))
                .get(ExampleAppModule.Service.class);

        assertEquals("example", service.value());
    }
}
