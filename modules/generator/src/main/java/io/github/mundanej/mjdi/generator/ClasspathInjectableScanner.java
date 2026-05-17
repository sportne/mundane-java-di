package io.github.mundanej.mjdi.generator;

import io.github.mundanej.mjdi.Inject;
import io.github.mundanej.mjdi.Named;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.jar.JarFile;

/**
 * Scans the current Java classpath for classes that can be generated into a module.
 *
 * <p>This class is build-time tooling. It may use class loading and annotation inspection because
 * it runs before the final application is built. The generated source does not use these dynamic
 * mechanisms.
 */
public final class ClasspathInjectableScanner {
    /**
     * Creates a scanner.
     */
    public ClasspathInjectableScanner() {}

    /**
     * Scans the current Java process classpath.
     *
     * @param request the scan and generated-module settings
     * @return a request that can be passed to {@link InjectionModuleSourceGenerator}
     */
    public GeneratedModuleRequest scan(ScanRequest request) {
        Objects.requireNonNull(request, "request");
        TreeSet<String> candidateClassNames = new TreeSet<>();
        for (String classpathEntry : currentClasspathEntries()) {
            Path path = Path.of(classpathEntry);
            if (Files.isDirectory(path)) {
                scanDirectory(path, request.packageRoots(), candidateClassNames);
            } else if (Files.isRegularFile(path) && classpathEntry.endsWith(".jar")) {
                scanJar(path, request.packageRoots(), candidateClassNames);
            }
        }

        List<GeneratedModuleRequest.ConstructorBinding> bindings = new ArrayList<>();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        for (String className : candidateClassNames) {
            inspectCandidate(loader, className).ifPresent(bindings::add);
        }
        return new GeneratedModuleRequest(request.modulePackage(), request.moduleClassName(), bindings);
    }

    /**
     * Settings for one classpath scan.
     *
     * @param modulePackage the Java package for the generated module
     * @param moduleClassName the simple class name for the generated module
     * @param packageRoots package prefixes to scan
     */
    public record ScanRequest(String modulePackage, String moduleClassName, List<String> packageRoots) {
        /**
         * Creates a scan request.
         *
         * @param modulePackage the Java package for the generated module
         * @param moduleClassName the simple class name for the generated module
         * @param packageRoots package prefixes to scan
         */
        public ScanRequest {
            Objects.requireNonNull(modulePackage, "modulePackage");
            Objects.requireNonNull(moduleClassName, "moduleClassName");
            packageRoots = List.copyOf(Objects.requireNonNull(packageRoots, "packageRoots"));
            if (packageRoots.isEmpty()) {
                throw new IllegalArgumentException("at least one package root is required");
            }
            for (String packageRoot : packageRoots) {
                if (packageRoot == null || packageRoot.isBlank()) {
                    throw new IllegalArgumentException("package roots must not be blank");
                }
            }
        }
    }

    private static List<String> currentClasspathEntries() {
        String classpath = System.getProperty("java.class.path", "");
        if (classpath.isBlank()) {
            return List.of();
        }
        return List.of(classpath.split(File.pathSeparator));
    }

    private static void scanDirectory(Path classpathRoot, List<String> packageRoots, TreeSet<String> classNames) {
        for (String packageRoot : packageRoots) {
            Path packagePath = classpathRoot.resolve(packageRoot.replace('.', File.separatorChar));
            if (!Files.isDirectory(packagePath)) {
                continue;
            }
            try (var paths = Files.walk(packagePath)) {
                paths.filter(path -> path.toString().endsWith(".class"))
                        .map(path -> classpathRoot.relativize(path).toString())
                        .map(ClasspathInjectableScanner::classNameFromPath)
                        .filter(ClasspathInjectableScanner::isLoadableCandidateName)
                        .forEach(classNames::add);
            } catch (IOException exception) {
                throw new IllegalStateException("failed to scan classpath directory " + classpathRoot, exception);
            }
        }
    }

    private static void scanJar(Path jarPath, List<String> packageRoots, TreeSet<String> classNames) {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            jarFile.stream()
                    .filter(entry -> !entry.isDirectory())
                    .map(entry -> entry.getName())
                    .filter(name -> name.endsWith(".class"))
                    .filter(name -> startsWithPackageRoot(name, packageRoots))
                    .map(ClasspathInjectableScanner::classNameFromPath)
                    .filter(ClasspathInjectableScanner::isLoadableCandidateName)
                    .forEach(classNames::add);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to scan classpath jar " + jarPath, exception);
        }
    }

    private static boolean startsWithPackageRoot(String entryName, List<String> packageRoots) {
        for (String packageRoot : packageRoots) {
            if (entryName.startsWith(packageRoot.replace('.', '/') + "/")) {
                return true;
            }
        }
        return false;
    }

    private static String classNameFromPath(String path) {
        String withoutSuffix = path.substring(0, path.length() - ".class".length());
        return withoutSuffix.replace(File.separatorChar, '.').replace('/', '.');
    }

    private static boolean isLoadableCandidateName(String className) {
        return !className.endsWith(".module-info") && !className.endsWith(".package-info");
    }

    private static Optional<GeneratedModuleRequest.ConstructorBinding> inspectCandidate(
            ClassLoader loader, String className) {
        Class<?> type = loadClass(loader, className);
        List<Constructor<?>> injectConstructors = new ArrayList<>();
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (constructor.isAnnotationPresent(Inject.class)) {
                injectConstructors.add(constructor);
            }
        }
        if (injectConstructors.isEmpty()) {
            return Optional.empty();
        }
        if (injectConstructors.size() > 1) {
            throw new IllegalStateException(type.getName() + " has multiple @Inject constructors");
        }
        Constructor<?> constructor = injectConstructors.get(0);
        if (!Modifier.isPublic(type.getModifiers())) {
            throw new IllegalStateException(type.getName() + " is injectable but is not public");
        }
        if (type.isMemberClass() && !Modifier.isStatic(type.getModifiers())) {
            throw new IllegalStateException(type.getName() + " is injectable but is a non-static member class");
        }
        if (!Modifier.isPublic(constructor.getModifiers())) {
            throw new IllegalStateException(type.getName() + " has an @Inject constructor that is not public");
        }
        String typeName = canonicalName(type);
        List<GeneratedModuleRequest.Dependency> dependencies = new ArrayList<>();
        for (Parameter parameter : constructor.getParameters()) {
            Class<?> parameterType = parameter.getType();
            String dependencyTypeName = canonicalName(parameterType);
            Named named = parameter.getAnnotation(Named.class);
            dependencies.add(named == null
                    ? GeneratedModuleRequest.Dependency.of(dependencyTypeName)
                    : GeneratedModuleRequest.Dependency.named(dependencyTypeName, named.value()));
        }
        Named named = type.getAnnotation(Named.class);
        return Optional.of(named == null
                ? GeneratedModuleRequest.ConstructorBinding.binding(
                        typeName, dependencies.toArray(GeneratedModuleRequest.Dependency[]::new))
                : GeneratedModuleRequest.ConstructorBinding.namedBinding(
                        typeName, named.value(), dependencies.toArray(GeneratedModuleRequest.Dependency[]::new)));
    }

    private static Class<?> loadClass(ClassLoader loader, String className) {
        try {
            return Class.forName(className, false, loader);
        } catch (ClassNotFoundException | LinkageError exception) {
            throw new IllegalStateException("failed to load classpath candidate " + className, exception);
        }
    }

    private static String canonicalName(Class<?> type) {
        if (type.isPrimitive() || type.isArray() || type.getCanonicalName() == null) {
            throw new IllegalStateException(type.getName() + " cannot be represented as a supported class literal");
        }
        return type.getCanonicalName();
    }
}
