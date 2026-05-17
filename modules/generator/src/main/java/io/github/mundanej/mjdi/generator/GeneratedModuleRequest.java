package io.github.mundanej.mjdi.generator;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Describes the module source that should be generated.
 *
 * @param modulePackage the Java package for the generated module
 * @param moduleClassName the simple class name for the generated module
 * @param bindings the bindings that should be emitted into the module
 */
public record GeneratedModuleRequest(
        String modulePackage, String moduleClassName, List<ConstructorBinding> bindings) {
    /**
     * Creates a generation request.
     *
     * @param modulePackage the Java package for the generated module
     * @param moduleClassName the simple class name for the generated module
     * @param bindings the bindings that should be emitted into the module
     */
    public GeneratedModuleRequest {
        requireJavaName(modulePackage, "modulePackage");
        requireJavaName(moduleClassName, "moduleClassName");
        bindings = List.copyOf(Objects.requireNonNull(bindings, "bindings"));
    }

    /**
     * Describes one class that should be bound by direct constructor call.
     *
     * @param typeName the fully qualified class name to construct
     * @param name the optional binding name
     * @param dependencies the constructor arguments to request from the context
     */
    public record ConstructorBinding(String typeName, Optional<String> name, List<Dependency> dependencies) {
        /**
         * Creates a constructor binding.
         *
         * @param typeName the fully qualified class name to construct
         * @param name the optional binding name
         * @param dependencies the constructor arguments to request from the context
         */
        public ConstructorBinding {
            requireJavaName(typeName, "typeName");
            name = Objects.requireNonNull(name, "name");
            name.ifPresent(value -> {
                if (value.isBlank()) {
                    throw new IllegalArgumentException("binding name must not be blank");
                }
            });
            dependencies = List.copyOf(Objects.requireNonNull(dependencies, "dependencies"));
        }

        /**
         * Creates an unnamed transient binding.
         *
         * @param typeName the fully qualified class name to construct
         * @param dependencies the constructor arguments to request from the context
         * @return a binding request
         */
        public static ConstructorBinding transientBinding(String typeName, Dependency... dependencies) {
            return new ConstructorBinding(typeName, Optional.empty(), List.of(dependencies));
        }

        /**
         * Creates a named transient binding.
         *
         * @param typeName the fully qualified class name to construct
         * @param name the binding name
         * @param dependencies the constructor arguments to request from the context
         * @return a binding request
         */
        public static ConstructorBinding namedBinding(String typeName, String name, Dependency... dependencies) {
            return new ConstructorBinding(typeName, Optional.of(name), List.of(dependencies));
        }
    }

    /**
     * Describes one constructor argument that generated code should fetch from the context.
     *
     * @param typeName the fully qualified dependency type name
     * @param name the optional dependency binding name
     */
    public record Dependency(String typeName, Optional<String> name) {
        /**
         * Creates a dependency request.
         *
         * @param typeName the fully qualified dependency type name
         * @param name the optional dependency binding name
         */
        public Dependency {
            requireJavaName(typeName, "typeName");
            name = Objects.requireNonNull(name, "name");
            name.ifPresent(value -> {
                if (value.isBlank()) {
                    throw new IllegalArgumentException("dependency name must not be blank");
                }
            });
        }

        /**
         * Creates an unnamed dependency request.
         *
         * @param typeName the fully qualified dependency type name
         * @return a dependency request
         */
        public static Dependency of(String typeName) {
            return new Dependency(typeName, Optional.empty());
        }

        /**
         * Creates a named dependency request.
         *
         * @param typeName the fully qualified dependency type name
         * @param name the binding name to request
         * @return a dependency request
         */
        public static Dependency named(String typeName, String name) {
            return new Dependency(typeName, Optional.of(name));
        }
    }

    private static void requireJavaName(String value, String label) {
        Objects.requireNonNull(value, label);
        if (!value.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*")) {
            throw new IllegalArgumentException(label + " must be a Java name: " + value);
        }
    }
}
