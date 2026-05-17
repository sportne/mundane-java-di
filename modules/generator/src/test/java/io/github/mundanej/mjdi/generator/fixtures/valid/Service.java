package io.github.mundanej.mjdi.generator.fixtures.valid;

import io.github.mundanej.mjdi.Inject;

public final class Service {
    private final Repository repository;

    @Inject
    public Service(Repository repository) {
        this.repository = repository;
    }

    public Repository repository() {
        return repository;
    }
}
