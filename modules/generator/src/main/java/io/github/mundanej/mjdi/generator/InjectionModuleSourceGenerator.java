package io.github.mundanej.mjdi.generator;

import io.github.mundanej.mjdi.AppPluginModule;
import io.github.mundanej.mjdi.Binder;
import io.github.mundanej.mjdi.Key;
import java.util.ArrayList;
import java.util.List;

/**
 * Emits Java source for an {@code AppPluginModule}.
 *
 * <p>The generated source uses direct constructor calls. It does not use reflection or classpath
 * scanning, which keeps the output friendly to GraalVM Native Image.
 */
public final class InjectionModuleSourceGenerator {
    /**
     * Creates a generator.
     */
    public InjectionModuleSourceGenerator() {}

    /**
     * Generates Java source from a simple binding request.
     *
     * @param request the module package, class name, and bindings to emit
     * @return the generated source description
     */
    public GeneratedModuleSource generate(GeneratedModuleRequest request) {
        List<String> lines = new ArrayList<>();
        lines.add("package " + request.modulePackage() + ";");
        lines.add("");
        lines.add("import " + AppPluginModule.class.getName() + ";");
        lines.add("import " + Binder.class.getName() + ";");
        lines.add("import " + Key.class.getName() + ";");
        lines.add("");
        lines.add("/**");
        lines.add(" * Generated module that registers application bindings.");
        lines.add(" */");
        lines.add("public final class " + request.moduleClassName() + " implements AppPluginModule {");
        lines.add("    /**");
        lines.add("     * Creates the generated module.");
        lines.add("     */");
        lines.add("    public " + request.moduleClassName() + "() {}");
        lines.add("");
        lines.add("    /**");
        lines.add("     * Registers generated dependency bindings.");
        lines.add("     *");
        lines.add("     * @param binder the binder that receives generated bindings");
        lines.add("     */");
        lines.add("    @Override");
        lines.add("    public void configure(Binder binder) {");
        for (GeneratedModuleRequest.ConstructorBinding binding : request.bindings()) {
            lines.add("        " + bindingCall(binding));
        }
        lines.add("    }");
        lines.add("}");
        lines.add("");
        return new GeneratedModuleSource(
                request.modulePackage(), request.moduleClassName(), String.join(System.lineSeparator(), lines));
    }

    private static String bindingCall(GeneratedModuleRequest.ConstructorBinding binding) {
        String target = classLiteral(binding.typeName());
        if (binding.name().isPresent()) {
            target = "Key.named(" + target + ", " + quoted(binding.name().orElseThrow()) + ")";
        }
        return "binder.bind(" + target + ", context -> new " + binding.typeName() + "("
                + dependencies(binding.dependencies()) + "));";
    }

    private static String dependencies(List<GeneratedModuleRequest.Dependency> dependencies) {
        List<String> expressions = new ArrayList<>();
        for (GeneratedModuleRequest.Dependency dependency : dependencies) {
            String key = classLiteral(dependency.typeName());
            if (dependency.name().isPresent()) {
                key = "Key.named(" + key + ", " + quoted(dependency.name().orElseThrow()) + ")";
            }
            expressions.add("context.get(" + key + ")");
        }
        return String.join(", ", expressions);
    }

    private static String classLiteral(String typeName) {
        return typeName + ".class";
    }

    private static String quoted(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
