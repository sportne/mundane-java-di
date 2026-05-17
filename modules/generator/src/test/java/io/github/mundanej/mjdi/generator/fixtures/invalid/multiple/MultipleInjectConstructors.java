package io.github.mundanej.mjdi.generator.fixtures.invalid.multiple;

import io.github.mundanej.mjdi.Inject;

public final class MultipleInjectConstructors {
    @Inject
    public MultipleInjectConstructors() {}

    @Inject
    public MultipleInjectConstructors(String value) {}
}
