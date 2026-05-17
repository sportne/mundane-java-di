package io.github.mundanej.mjdi.generator;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Command-line entrypoint for generating an {@code AppPluginModule}.
 *
 * <p>The CLI scans the current Java process classpath. Add application classes to the Java command
 * classpath before running this entrypoint.
 */
public final class GeneratorCli {
  /** Creates a CLI helper. */
  public GeneratorCli() {}

  /**
   * Runs the generator command line.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    int exitCode = new GeneratorCli().run(args, System.out, System.err);
    if (exitCode != 0) {
      System.exit(exitCode);
    }
  }

  /**
   * Runs the generator command using explicit output streams.
   *
   * @param args command-line arguments
   * @param out standard output stream
   * @param err standard error stream
   * @return process-style exit code
   */
  public int run(String[] args, PrintStream out, PrintStream err) {
    Objects.requireNonNull(args, "args");
    Objects.requireNonNull(out, "out");
    Objects.requireNonNull(err, "err");
    try {
      Options options = Options.parse(args);
      if (options.help()) {
        out.print(usage());
        return 0;
      }
      if (options.version()) {
        out.println(version());
        return 0;
      }
      GeneratedModuleRequest request =
          new ClasspathInjectableScanner()
              .scan(
                  new ClasspathInjectableScanner.ScanRequest(
                      options.modulePackage(),
                      options.moduleClass(),
                      options.packageRoots(),
                      options.effectiveScanPaths()));
      GeneratedModuleSource source = new InjectionModuleSourceGenerator().generate(request);
      if (options.dryRun()) {
        out.print(source.sourceText());
      } else {
        out.println("Generated " + writeSource(options, source));
      }
      return 0;
    } catch (RuntimeException | IOException exception) {
      err.println(exception.getMessage());
      err.println(usage());
      return 1;
    }
  }

  private static Path writeSource(Options options, GeneratedModuleSource source)
      throws IOException {
    Path packageDirectory = options.outputDir().resolve(source.packageName().replace('.', '/'));
    Path sourceFile = packageDirectory.resolve(source.className() + ".java");
    Files.createDirectories(packageDirectory);
    if (Files.exists(sourceFile)) {
      String existing = Files.readString(sourceFile, StandardCharsets.UTF_8);
      if (!existing.equals(source.sourceText()) && !options.overwrite()) {
        throw new IllegalStateException(sourceFile + " already exists with different content");
      }
    }
    Files.writeString(sourceFile, source.sourceText(), StandardCharsets.UTF_8);
    return sourceFile;
  }

  private static String usage() {
    return """
                usage: GeneratorCli --output-dir DIR --module-package PACKAGE --module-class NAME \
                --package-root PACKAGE [--package-root PACKAGE ...] [--scan-path PATH ...] [--dry-run] [--overwrite]

                options:
                  --output-dir DIR          directory where generated source is written
                  --module-package PACKAGE  Java package for the generated module
                  --module-class NAME       simple class name for the generated module
                  --package-root PACKAGE    package prefix to scan; may be repeated
                  --scan-path PATH          classpath directory or jar to scan; may be repeated
                  --dry-run                 print generated source without writing a file
                  --overwrite               replace an existing generated file with different content
                  --help                    print this help text
                  --version                 print the generator version
                """;
  }

  private static String version() {
    String implementationVersion = GeneratorCli.class.getPackage().getImplementationVersion();
    return implementationVersion == null ? "development" : implementationVersion;
  }

  private record Options(
      Path outputDir,
      String modulePackage,
      String moduleClass,
      List<String> packageRoots,
      List<Path> scanPaths,
      boolean dryRun,
      boolean overwrite,
      boolean help,
      boolean version) {
    private static Options parse(String[] args) {
      Path outputDir = null;
      String modulePackage = null;
      String moduleClass = null;
      List<String> packageRoots = new ArrayList<>();
      List<Path> scanPaths = new ArrayList<>();
      boolean dryRun = false;
      boolean overwrite = false;
      boolean help = false;
      boolean version = false;
      for (int index = 0; index < args.length; index++) {
        String arg = args[index];
        switch (arg) {
          case "--output-dir" -> outputDir = Path.of(value(args, ++index, arg));
          case "--module-package" -> modulePackage = value(args, ++index, arg);
          case "--module-class" -> moduleClass = value(args, ++index, arg);
          case "--package-root" -> packageRoots.add(value(args, ++index, arg));
          case "--scan-path" -> scanPaths.add(Path.of(value(args, ++index, arg)));
          case "--dry-run" -> dryRun = true;
          case "--overwrite" -> overwrite = true;
          case "--help" -> help = true;
          case "--version" -> version = true;
          default -> throw new IllegalArgumentException("unknown argument: " + arg);
        }
      }
      if (help || version) {
        return new Options(null, null, null, List.of(), List.of(), false, false, help, version);
      }
      if (outputDir == null) {
        throw new IllegalArgumentException("--output-dir is required");
      }
      if (modulePackage == null || modulePackage.isBlank()) {
        throw new IllegalArgumentException("--module-package is required");
      }
      if (moduleClass == null || moduleClass.isBlank()) {
        throw new IllegalArgumentException("--module-class is required");
      }
      return new Options(
          outputDir,
          modulePackage,
          moduleClass,
          List.copyOf(packageRoots),
          List.copyOf(scanPaths),
          dryRun,
          overwrite,
          false,
          false);
    }

    private static String value(String[] args, int index, String name) {
      if (index >= args.length || args[index].startsWith("--")) {
        throw new IllegalArgumentException(name + " requires a value");
      }
      return args[index];
    }

    private List<Path> effectiveScanPaths() {
      if (!scanPaths.isEmpty()) {
        return scanPaths;
      }
      String classpath = System.getProperty("java.class.path", "");
      if (classpath.isBlank()) {
        return List.of();
      }
      return List.of(classpath.split(java.io.File.pathSeparator)).stream().map(Path::of).toList();
    }
  }
}
