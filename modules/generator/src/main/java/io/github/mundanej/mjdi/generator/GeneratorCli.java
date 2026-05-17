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
    /**
     * Creates a CLI helper.
     */
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
            GeneratedModuleRequest request = new ClasspathInjectableScanner()
                    .scan(new ClasspathInjectableScanner.ScanRequest(
                            options.modulePackage(), options.moduleClass(), options.packageRoots()));
            GeneratedModuleSource source = new InjectionModuleSourceGenerator().generate(request);
            if (options.dryRun()) {
                out.print(source.sourceText());
            } else {
                writeSource(options, source);
            }
            return 0;
        } catch (RuntimeException | IOException exception) {
            err.println(exception.getMessage());
            err.println(usage());
            return 1;
        }
    }

    private static void writeSource(Options options, GeneratedModuleSource source) throws IOException {
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
    }

    private static String usage() {
        return "usage: GeneratorCli --output-dir DIR --module-package PACKAGE --module-class NAME "
                + "--package-root PACKAGE [--package-root PACKAGE ...] [--dry-run] [--overwrite]";
    }

    private record Options(
            Path outputDir,
            String modulePackage,
            String moduleClass,
            List<String> packageRoots,
            boolean dryRun,
            boolean overwrite) {
        private static Options parse(String[] args) {
            Path outputDir = null;
            String modulePackage = null;
            String moduleClass = null;
            List<String> packageRoots = new ArrayList<>();
            boolean dryRun = false;
            boolean overwrite = false;
            for (int index = 0; index < args.length; index++) {
                String arg = args[index];
                switch (arg) {
                    case "--output-dir" -> outputDir = Path.of(value(args, ++index, arg));
                    case "--module-package" -> modulePackage = value(args, ++index, arg);
                    case "--module-class" -> moduleClass = value(args, ++index, arg);
                    case "--package-root" -> packageRoots.add(value(args, ++index, arg));
                    case "--dry-run" -> dryRun = true;
                    case "--overwrite" -> overwrite = true;
                    default -> throw new IllegalArgumentException("unknown argument: " + arg);
                }
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
            return new Options(outputDir, modulePackage, moduleClass, packageRoots, dryRun, overwrite);
        }

        private static String value(String[] args, int index, String name) {
            if (index >= args.length || args[index].startsWith("--")) {
                throw new IllegalArgumentException(name + " requires a value");
            }
            return args[index];
        }
    }
}
