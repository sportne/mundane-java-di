package io.github.mundanej.mjdi.generator;

import io.github.mundanej.mjdi.AppPluginModule;
import io.github.mundanej.mjdi.Binder;
import io.github.mundanej.mjdi.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Emits Java source for an {@code AppPluginModule}.
 *
 * <p>The generated source uses direct constructor calls and singleton bindings. It does not use
 * reflection or classpath scanning, which keeps the output friendly to GraalVM Native Image.
 */
public final class InjectionModuleSourceGenerator {
  /** Creates a generator. */
  public InjectionModuleSourceGenerator() {}

  /**
   * Generates Java source from a simple binding request.
   *
   * @param request the module package, class name, and bindings to emit
   * @return the generated source description
   */
  public GeneratedModuleSource generate(GeneratedModuleRequest request) {
    ImportPlan imports = ImportPlan.create(request);
    List<String> lines = new ArrayList<>();
    lines.add("package " + request.modulePackage() + ";");
    lines.add("");
    lines.add("import " + AppPluginModule.class.getName() + ";");
    lines.add("import " + Binder.class.getName() + ";");
    if (usesKey(request)) {
      lines.add("import " + Key.class.getName() + ";");
    }
    for (String importedType : imports.importedTypes()) {
      lines.add("import " + importedType + ";");
    }
    lines.add("");
    lines.add("/** Generated module that registers application bindings. */");
    lines.add("public final class " + request.moduleClassName() + " implements AppPluginModule {");
    lines.add("  /** Creates the generated module. */");
    lines.add("  public " + request.moduleClassName() + "() {}");
    lines.add("");
    lines.add("  /**");
    lines.add("   * Registers generated dependency bindings.");
    lines.add("   *");
    lines.add("   * @param binder the binder that receives generated bindings");
    lines.add("   */");
    lines.add("  @Override");
    lines.add("  public void configure(Binder binder) {");
    for (GeneratedModuleRequest.ConstructorBinding binding : request.bindings()) {
      lines.addAll(bindingCall(binding, imports));
    }
    lines.add("  }");
    lines.add("}");
    lines.add("");
    return new GeneratedModuleSource(
        request.modulePackage(),
        request.moduleClassName(),
        String.join(System.lineSeparator(), lines));
  }

  private static List<String> bindingCall(
      GeneratedModuleRequest.ConstructorBinding binding, ImportPlan imports) {
    String target = classLiteral(binding.typeName(), imports);
    if (binding.name().isPresent()) {
      target = "Key.named(" + target + ", " + quoted(binding.name().orElseThrow()) + ")";
    }
    List<String> lines = new ArrayList<>();
    if (binding.dependencies().isEmpty()) {
      String line =
          "    binder.bindSingleton("
              + target
              + ", context -> new "
              + imports.reference(binding.typeName())
              + "());";
      if (line.length() <= 80) {
        lines.add(line);
        return lines;
      }
      lines.add("    binder.bindSingleton(");
      lines.add(
          "        "
              + target
              + ", context -> new "
              + imports.reference(binding.typeName())
              + "());");
      return lines;
    }
    List<String> dependencies = dependencies(binding.dependencies(), imports);
    if (dependencies.size() == 1) {
      String line =
          "    binder.bindSingleton("
              + target
              + ", context -> new "
              + imports.reference(binding.typeName())
              + "("
              + dependencies.get(0)
              + "));";
      if (line.length() <= 100) {
        lines.add(line);
        return lines;
      }
    }
    lines.add("    binder.bindSingleton(");
    lines.add("        " + target + ",");
    if (dependencies.size() == 1) {
      lines.add(
          "        context -> new "
              + imports.reference(binding.typeName())
              + "("
              + dependencies.get(0)
              + "));");
      return lines;
    }
    lines.add("        context ->");
    lines.add("            new " + imports.reference(binding.typeName()) + "(");
    for (int index = 0; index < dependencies.size(); index++) {
      String suffix = index == dependencies.size() - 1 ? "));" : ",";
      lines.add("                " + dependencies.get(index) + suffix);
    }
    return lines;
  }

  private static List<String> dependencies(
      List<GeneratedModuleRequest.Dependency> dependencies, ImportPlan imports) {
    List<String> expressions = new ArrayList<>();
    for (GeneratedModuleRequest.Dependency dependency : dependencies) {
      String name = dependency.name().orElse(null);
      String namedScalarMethod = namedScalarMethod(dependency.typeName());
      if (name != null && namedScalarMethod != null) {
        expressions.add("context." + namedScalarMethod + "(" + quoted(name) + ")");
        continue;
      }
      String key = classLiteral(dependency.typeName(), imports);
      if (name != null) {
        key = "Key.named(" + key + ", " + quoted(name) + ")";
      }
      expressions.add("context.get(" + key + ")");
    }
    return expressions;
  }

  private static String namedScalarMethod(String typeName) {
    return switch (typeName) {
      case "java.lang.String" -> "getNamedString";
      case "boolean", "java.lang.Boolean" -> "getNamedBool";
      case "int", "java.lang.Integer" -> "getNamedInt";
      case "long", "java.lang.Long" -> "getNamedLong";
      case "double", "java.lang.Double" -> "getNamedDouble";
      case "float", "java.lang.Float" -> "getNamedFloat";
      case "short", "java.lang.Short" -> "getNamedShort";
      case "byte", "java.lang.Byte" -> "getNamedByte";
      case "char", "java.lang.Character" -> "getNamedChar";
      default -> null;
    };
  }

  private static String classLiteral(String typeName, ImportPlan imports) {
    return imports.reference(typeName) + ".class";
  }

  private static String quoted(String value) {
    StringBuilder result = new StringBuilder("\"");
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      switch (character) {
        case '\\' -> result.append("\\\\");
        case '"' -> result.append("\\\"");
        case '\b' -> result.append("\\b");
        case '\t' -> result.append("\\t");
        case '\n' -> result.append("\\n");
        case '\f' -> result.append("\\f");
        case '\r' -> result.append("\\r");
        default -> {
          if (Character.isISOControl(character)) {
            result.append(String.format("\\u%04x", (int) character));
          } else {
            result.append(character);
          }
        }
      }
    }
    return result.append('"').toString();
  }

  private static boolean usesKey(GeneratedModuleRequest request) {
    for (GeneratedModuleRequest.ConstructorBinding binding : request.bindings()) {
      if (binding.name().isPresent()) {
        return true;
      }
      for (GeneratedModuleRequest.Dependency dependency : binding.dependencies()) {
        if (dependency.name().isPresent() && namedScalarMethod(dependency.typeName()) == null) {
          return true;
        }
      }
    }
    return false;
  }

  private record ImportPlan(Map<String, String> references, Set<String> importedTypes) {
    private static ImportPlan create(GeneratedModuleRequest request) {
      Map<String, Set<String>> bySimpleName = new HashMap<>();
      for (String typeName : typeNames(request)) {
        if (isPrimitive(typeName) || typeName.startsWith("java.lang.")) {
          continue;
        }
        bySimpleName
            .computeIfAbsent(simpleName(typeName), ignored -> new HashSet<>())
            .add(typeName);
      }

      Map<String, String> references = new LinkedHashMap<>();
      Set<String> importedTypes = new TreeSet<>();
      for (String typeName : typeNames(request)) {
        if (isPrimitive(typeName)) {
          references.put(typeName, typeName);
        } else if (typeName.startsWith("java.lang.")) {
          references.put(typeName, simpleName(typeName));
        } else if (bySimpleName.get(simpleName(typeName)).size() == 1) {
          references.put(typeName, simpleName(typeName));
          importedTypes.add(typeName);
        } else {
          references.put(typeName, typeName);
        }
      }
      return new ImportPlan(Map.copyOf(references), new TreeSet<>(importedTypes));
    }

    private String reference(String typeName) {
      return references.getOrDefault(typeName, typeName);
    }

    private static Set<String> typeNames(GeneratedModuleRequest request) {
      Set<String> typeNames = new TreeSet<>();
      for (GeneratedModuleRequest.ConstructorBinding binding : request.bindings()) {
        typeNames.add(binding.typeName());
        for (GeneratedModuleRequest.Dependency dependency : binding.dependencies()) {
          typeNames.add(dependency.typeName());
        }
      }
      return typeNames;
    }

    private static boolean isPrimitive(String typeName) {
      return switch (typeName) {
        case "boolean", "byte", "char", "double", "float", "int", "long", "short" -> true;
        default -> false;
      };
    }

    private static String simpleName(String typeName) {
      return typeName.substring(typeName.lastIndexOf('.') + 1);
    }
  }
}
