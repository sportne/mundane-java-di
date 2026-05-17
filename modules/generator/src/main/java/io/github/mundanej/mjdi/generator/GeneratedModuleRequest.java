package io.github.mundanej.mjdi.generator;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Describes the module source that should be generated.
 *
 * @param modulePackage the Java package for the generated module
 * @param moduleClassName the simple class name for the generated module
 * @param bindings the bindings that should be emitted into the module
 */
public record GeneratedModuleRequest(
        String modulePackage, String moduleClassName, List<ConstructorBinding> bindings) {
    private static final Set<String> JAVA_KEYWORDS_AND_LITERALS = Set.of(
            "_",
            "abstract",
            "assert",
            "boolean",
            "break",
            "byte",
            "case",
            "catch",
            "char",
            "class",
            "const",
            "continue",
            "default",
            "do",
            "double",
            "else",
            "enum",
            "extends",
            "false",
            "final",
            "finally",
            "float",
            "for",
            "goto",
            "if",
            "implements",
            "import",
            "instanceof",
            "int",
            "interface",
            "long",
            "native",
            "new",
            "null",
            "package",
            "private",
            "protected",
            "public",
            "return",
            "short",
            "static",
            "strictfp",
            "super",
            "switch",
            "synchronized",
            "this",
            "throw",
            "throws",
            "transient",
            "true",
            "try",
            "void",
            "volatile",
            "while");
    private static final Set<String> RESTRICTED_TYPE_IDENTIFIERS = Set.of("record", "var", "yield");

    /**
     * Creates a generation request.
     *
     * @param modulePackage the Java package for the generated module
     * @param moduleClassName the simple class name for the generated module
     * @param bindings the bindings that should be emitted into the module
     */
    public GeneratedModuleRequest {
        requireQualifiedJavaName(modulePackage, "modulePackage");
        requireSimpleTypeName(moduleClassName, "moduleClassName");
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
            requireQualifiedTypeName(typeName, "typeName");
            name = Objects.requireNonNull(name, "name");
            name.ifPresent(value -> {
                if (value.isBlank()) {
                    throw new IllegalArgumentException("binding name must not be blank");
                }
            });
            dependencies = List.copyOf(Objects.requireNonNull(dependencies, "dependencies"));
        }

        /**
         * Creates an unnamed generated binding.
         *
         * @param typeName the fully qualified class name to construct
         * @param dependencies the constructor arguments to request from the context
         * @return a binding request
         */
        public static ConstructorBinding binding(String typeName, Dependency... dependencies) {
            return new ConstructorBinding(typeName, Optional.empty(), List.of(dependencies));
        }

        /**
         * Creates a named generated binding.
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
            requireQualifiedTypeName(typeName, "typeName");
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

    private static void requireQualifiedJavaName(String value, String label) {
        Objects.requireNonNull(value, label);
        String[] parts = value.split("\\.", -1);
        for (String part : parts) {
            if (!isValidSimpleJavaName(part)) {
                throw new IllegalArgumentException(label + " must be a Java name: " + value);
            }
        }
    }

    private static void requireQualifiedTypeName(String value, String label) {
        requireQualifiedJavaName(value, label);
        String simpleName = value.substring(value.lastIndexOf('.') + 1);
        if (RESTRICTED_TYPE_IDENTIFIERS.contains(simpleName)) {
            throw new IllegalArgumentException(label + " must be a Java type name: " + value);
        }
    }

    private static void requireSimpleTypeName(String value, String label) {
        Objects.requireNonNull(value, label);
        if (!isValidSimpleJavaName(value) || RESTRICTED_TYPE_IDENTIFIERS.contains(value)) {
            throw new IllegalArgumentException(label + " must be a simple Java name: " + value);
        }
    }

    private static boolean isValidSimpleJavaName(String value) {
        if (value.isEmpty() || JAVA_KEYWORDS_AND_LITERALS.contains(value)) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(value.charAt(0))) {
            return false;
        }
        for (int index = 1; index < value.length(); index++) {
            if (!Character.isJavaIdentifierPart(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }
}
