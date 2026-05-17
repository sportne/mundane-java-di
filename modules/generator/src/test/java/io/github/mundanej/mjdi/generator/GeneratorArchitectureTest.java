package io.github.mundanej.mjdi.generator;

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

@AnalyzeClasses(packages = "io.github.mundanej.mjdi.generator", importOptions = DoNotIncludeTests.class)
final class GeneratorArchitectureTest {
    @ArchTest
    static final ArchRule project_specific_generator_does_not_depend_on_examples_or_tests =
            noClasses()
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "io.github.mundanej.mjdi.examples..",
                            "org.junit..",
                            "com.tngtech.archunit..");

    @ArchTest
    static final ArchRule project_specific_generator_has_no_external_runtime_dependencies =
            noClasses()
                    .should()
                    .dependOnClassesThat()
                    .resideOutsideOfPackages("java..", "io.github.mundanej.mjdi..");

    @ArchTest
    static final ArchRule baseline_generator_avoids_serialization_and_internal_jdk_apis =
            noClasses()
                    .should()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("java.io.ObjectInputStream")
                    .orShould()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("java.io.ObjectOutputStream")
                    .orShould()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("java.io.Externalizable")
                    .orShould()
                    .dependOnClassesThat()
                    .resideInAnyPackage("sun..", "jdk.internal..");

    @ArchTest
    static final ArchRule baseline_generator_avoids_serialization_hooks =
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
    static final ArchRule baseline_generator_does_not_terminate_spawn_processes_or_force_gc =
            noClasses()
                    .that()
                    .doNotHaveFullyQualifiedName("io.github.mundanej.mjdi.generator.GeneratorCli")
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
    static final ArchRule baseline_generator_has_no_finalizers =
            noMethods().should().haveName("finalize");

    @ArchTest
    static final ArchRule baseline_generator_has_no_public_static_mutable_fields =
            fields().that().arePublic().and().areStatic().should().beFinal().allowEmptyShould(true);

    @ArchTest
    static final ArchRule baseline_generator_has_no_native_methods =
            noMethods().should().haveModifier(JavaModifier.NATIVE);

    @ArchTest
    static final ArchRule baseline_generator_does_not_mutate_global_jvm_state =
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
    static final ArchRule baseline_generator_does_not_use_deprecated_thread_control =
            noClasses()
                    .should()
                    .callMethod(Thread.class, "stop")
                    .orShould()
                    .callMethod(Thread.class, "suspend")
                    .orShould()
                    .callMethod(Thread.class, "resume");

    private GeneratorArchitectureTest() {}
}
