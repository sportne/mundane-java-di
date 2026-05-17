package io.github.mundanej.mjdi.generator.fixtures.invalid.member;

import io.github.mundanej.mjdi.Inject;

/**
 * Fixture containing an unsupported non-static member injectable.
 */
public class OuterInjectable {
    /**
     * Creates the outer fixture.
     */
    public OuterInjectable() {}

    /**
     * Injectable member class that cannot be constructed by generated source.
     */
    public class InnerInjectable {
        /**
         * Creates the unsupported inner injectable.
         */
        @Inject
        public InnerInjectable() {}
    }
}
