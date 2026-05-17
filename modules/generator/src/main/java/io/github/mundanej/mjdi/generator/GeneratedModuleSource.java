package io.github.mundanej.mjdi.generator;

/**
 * Source text produced for one generated application module.
 *
 * @param packageName the Java package for the generated module
 * @param className the simple class name for the generated module
 * @param sourceText the complete Java source text
 */
public record GeneratedModuleSource(String packageName, String className, String sourceText) {}
