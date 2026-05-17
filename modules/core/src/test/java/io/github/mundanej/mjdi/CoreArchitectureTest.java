package io.github.mundanej.mjdi;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.io.PrintStream;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

@AnalyzeClasses(packages = "io.github.mundanej.mjdi", importOptions = DoNotIncludeTests.class)
final class CoreArchitectureTest {
    @ArchTest
    static final ArchRule project_specific_core_main_code_has_no_third_party_dependencies =
            noClasses()
                    .should()
                    .dependOnClassesThat()
                    .resideOutsideOfPackages("java..", "io.github.mundanej.mjdi..");

    @ArchTest
    static final ArchRule project_specific_core_does_not_depend_on_generator_or_examples =
            noClasses()
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "io.github.mundanej.mjdi.generator..",
                            "io.github.mundanej.mjdi.examples..",
                            "org.junit..",
                            "com.tngtech.archunit..",
                            "io.github.classgraph..",
                            "org.reflections..");

    @ArchTest
    static final ArchRule project_specific_runtime_does_not_inspect_injection_annotations =
            noClasses()
                    .that()
                    .doNotHaveFullyQualifiedName("io.github.mundanej.mjdi.Inject")
                    .and()
                    .doNotHaveFullyQualifiedName("io.github.mundanej.mjdi.Named")
                    .should()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("io.github.mundanej.mjdi.Inject")
                    .orShould()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("io.github.mundanej.mjdi.Named");

    @ArchTest
    static final ArchRule project_specific_service_loader_is_only_used_by_bootstrap =
            noClasses()
                    .that()
                    .doNotHaveFullyQualifiedName("io.github.mundanej.mjdi.BootstrapAppContext")
                    .should()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("java.util.ServiceLoader");

    @ArchTest
    static final ArchRule native_image_core_avoids_dynamic_runtime_mechanisms =
            noClasses()
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("java.lang.reflect..", "java.lang.invoke..", "org.reflections..")
                    .orShould()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("java.lang.ClassLoader")
                    .orShould()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("java.net.URLClassLoader")
                    .orShould()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("java.lang.reflect.Proxy")
                    .orShould()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("java.lang.reflect.InvocationHandler")
                    .orShould()
                    .callMethod(Class.class, "forName", String.class);

    @ArchTest
    static final ArchRule native_image_core_avoids_serialization =
            noClasses()
                    .should()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("java.io.Serializable")
                    .orShould()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("java.io.Externalizable")
                    .orShould()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("java.io.ObjectInputStream")
                    .orShould()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("java.io.ObjectOutputStream");

    @ArchTest
    static final ArchRule native_image_core_avoids_serialization_hooks =
            noMethods()
                    .should()
                    .haveName("readObject")
                    .orShould()
                    .haveName("writeObject")
                    .orShould()
                    .haveName("readResolve")
                    .orShould()
                    .haveName("writeReplace");

    @ArchTest
    static final ArchRule native_image_core_avoids_internal_jdk_unsafe_and_security_manager_apis =
            noClasses()
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("sun..", "jdk.internal..")
                    .orShould()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("java.security.AccessController")
                    .orShould()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("java.lang.SecurityManager");

    @ArchTest
    static final ArchRule native_image_core_avoids_resource_discovery =
            noClasses()
                    .should()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("java.util.ResourceBundle")
                    .orShould()
                    .callMethod(Class.class, "getResource", String.class)
                    .orShould()
                    .callMethod(Class.class, "getResourceAsStream", String.class);

    @ArchTest
    static final ArchRule native_image_core_avoids_dynamic_compilation_and_scripting =
            noClasses()
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("javax.script..", "javax.tools..");

    @ArchTest
    static final ArchRule native_image_core_has_no_native_methods =
            noMethods().should().haveModifier(JavaModifier.NATIVE);

    @ArchTest
    static final ArchRule baseline_core_does_not_terminate_spawn_processes_or_force_gc =
            noClasses()
                    .should()
                    .callMethod(System.class, "exit", int.class)
                    .orShould()
                    .callMethod(System.class, "gc")
                    .orShould()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("java.lang.Runtime")
                    .orShould()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("java.lang.ProcessBuilder");

    @ArchTest
    static final ArchRule baseline_core_has_no_finalizers =
            noMethods().should().haveName("finalize");

    @ArchTest
    static final ArchRule baseline_core_has_no_public_static_mutable_fields =
            fields().that().arePublic().and().areStatic().should().beFinal().allowEmptyShould(true);

    @ArchTest
    static final ArchRule baseline_core_does_not_mutate_global_jvm_state =
            noClasses()
                    .should()
                    .callMethod(System.class, "setProperties", Properties.class)
                    .orShould()
                    .callMethod(System.class, "setProperty", String.class, String.class)
                    .orShould()
                    .callMethod(System.class, "clearProperty", String.class)
                    .orShould()
                    .callMethod(System.class, "setOut", PrintStream.class)
                    .orShould()
                    .callMethod(System.class, "setErr", PrintStream.class)
                    .orShould()
                    .callMethod(Locale.class, "setDefault", Locale.class)
                    .orShould()
                    .callMethod(TimeZone.class, "setDefault", TimeZone.class);

    @ArchTest
    static final ArchRule baseline_core_does_not_use_deprecated_thread_control =
            noClasses()
                    .should()
                    .callMethod(Thread.class, "stop")
                    .orShould()
                    .callMethod(Thread.class, "suspend")
                    .orShould()
                    .callMethod(Thread.class, "resume");

    private CoreArchitectureTest() {}
}
