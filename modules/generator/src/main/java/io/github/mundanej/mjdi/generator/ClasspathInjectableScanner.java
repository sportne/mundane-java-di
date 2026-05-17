package io.github.mundanej.mjdi.generator;

import io.github.mundanej.mjdi.Inject;
import io.github.mundanej.mjdi.Named;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
  /** Creates a scanner. */
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
    for (Path path : request.scanPaths()) {
      if (Files.isDirectory(path)) {
        scanDirectory(path, request.packageRoots(), candidateClassNames);
      } else if (Files.isRegularFile(path) && path.toString().endsWith(".jar")) {
        scanJar(path, request.packageRoots(), candidateClassNames);
      }
    }

    List<GeneratedModuleRequest.ConstructorBinding> bindings = new ArrayList<>();
    try (URLClassLoader loader =
        new URLClassLoader(
            toUrls(request.scanPaths()), Thread.currentThread().getContextClassLoader())) {
      for (String className : candidateClassNames) {
        inspectCandidate(loader, className).ifPresent(bindings::add);
      }
    } catch (IOException exception) {
      throw new IllegalStateException("failed to close generator scan class loader", exception);
    }
    return new GeneratedModuleRequest(request.modulePackage(), request.moduleClassName(), bindings);
  }

  /**
   * Settings for one classpath scan.
   *
   * @param modulePackage the Java package for the generated module
   * @param moduleClassName the simple class name for the generated module
   * @param packageRoots package prefixes to scan
   * @param scanPaths classpath directory or jar entries to scan
   */
  public record ScanRequest(
      String modulePackage,
      String moduleClassName,
      List<String> packageRoots,
      List<Path> scanPaths) {
    /**
     * Creates a scan request that uses the current Java process classpath.
     *
     * @param modulePackage the Java package for the generated module
     * @param moduleClassName the simple class name for the generated module
     * @param packageRoots package prefixes to scan
     */
    public ScanRequest(String modulePackage, String moduleClassName, List<String> packageRoots) {
      this(modulePackage, moduleClassName, packageRoots, currentClasspathEntries());
    }

    /**
     * Creates a scan request.
     *
     * @param modulePackage the Java package for the generated module
     * @param moduleClassName the simple class name for the generated module
     * @param packageRoots package prefixes to scan
     * @param scanPaths classpath directory or jar entries to scan
     */
    public ScanRequest {
      Objects.requireNonNull(modulePackage, "modulePackage");
      Objects.requireNonNull(moduleClassName, "moduleClassName");
      packageRoots = List.copyOf(Objects.requireNonNull(packageRoots, "packageRoots"));
      scanPaths = List.copyOf(Objects.requireNonNull(scanPaths, "scanPaths"));
      if (packageRoots.isEmpty()) {
        throw new IllegalArgumentException("at least one package root is required");
      }
      for (String packageRoot : packageRoots) {
        if (packageRoot == null || packageRoot.isBlank()) {
          throw new IllegalArgumentException("package roots must not be blank");
        }
      }
      for (Path scanPath : scanPaths) {
        Objects.requireNonNull(scanPath, "scanPath");
      }
    }
  }

  private static List<Path> currentClasspathEntries() {
    String classpath = System.getProperty("java.class.path", "");
    if (classpath.isBlank()) {
      return List.of();
    }
    return List.of(classpath.split(File.pathSeparator)).stream().map(Path::of).toList();
  }

  private static URL[] toUrls(List<Path> scanPaths) {
    List<URL> urls = new ArrayList<>();
    for (Path scanPath : scanPaths) {
      try {
        urls.add(scanPath.toUri().toURL());
      } catch (MalformedURLException exception) {
        throw new IllegalStateException("invalid scan path URL " + scanPath, exception);
      }
    }
    return urls.toArray(URL[]::new);
  }

  private static void scanDirectory(
      Path classpathRoot, List<String> packageRoots, Set<String> classNames) {
    for (String packageRoot : packageRoots) {
      Path packagePath = classpathRoot.resolve(packageRoot.replace('.', File.separatorChar));
      if (!Files.isDirectory(packagePath)) {
        continue;
      }
      try (var paths = Files.walk(packagePath)) {
        paths
            .filter(path -> path.toString().endsWith(".class"))
            .map(path -> classpathRoot.relativize(path).toString())
            .map(ClasspathInjectableScanner::classNameFromPath)
            .filter(ClasspathInjectableScanner::isLoadableCandidateName)
            .forEach(classNames::add);
      } catch (IOException exception) {
        throw new IllegalStateException(
            "failed to scan classpath directory " + classpathRoot, exception);
      }
    }
  }

  private static void scanJar(Path jarPath, List<String> packageRoots, Set<String> classNames) {
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
      throw new IllegalStateException(
          type.getName() + " is injectable but is a non-static member class");
    }
    if (!Modifier.isPublic(constructor.getModifiers())) {
      throw new IllegalStateException(
          type.getName() + " has an @Inject constructor that is not public");
    }
    String typeName = canonicalName(type);
    List<GeneratedModuleRequest.Dependency> dependencies = new ArrayList<>();
    for (Parameter parameter : constructor.getParameters()) {
      Named named = parameter.getAnnotation(Named.class);
      String dependencyTypeName = dependencyTypeName(parameter.getType(), named);
      dependencies.add(
          named == null
              ? GeneratedModuleRequest.Dependency.of(dependencyTypeName)
              : GeneratedModuleRequest.Dependency.named(dependencyTypeName, named.value()));
    }
    Named named = type.getAnnotation(Named.class);
    return Optional.of(
        named == null
            ? GeneratedModuleRequest.ConstructorBinding.binding(
                typeName, dependencies.toArray(GeneratedModuleRequest.Dependency[]::new))
            : GeneratedModuleRequest.ConstructorBinding.namedBinding(
                typeName,
                named.value(),
                dependencies.toArray(GeneratedModuleRequest.Dependency[]::new)));
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
      throw new IllegalStateException(
          type.getName() + " cannot be represented as a supported class literal");
    }
    return type.getCanonicalName();
  }

  private static String dependencyTypeName(Class<?> type, Named named) {
    if (type.isPrimitive() && named != null && isScalarPrimitive(type)) {
      return type.getName();
    }
    return canonicalName(type);
  }

  private static boolean isScalarPrimitive(Class<?> type) {
    return type == boolean.class
        || type == byte.class
        || type == char.class
        || type == double.class
        || type == float.class
        || type == int.class
        || type == long.class
        || type == short.class;
  }
}
