package io.github.mundanej.mjdi.examples.showcase;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "io.github.mundanej.mjdi.examples.showcase",
    importOptions = DoNotIncludeTests.class)
final class ShowcaseArchitectureTest {
  @ArchTest
  static final ArchRule main_example_code_does_not_depend_on_generator =
      noClasses()
          .should()
          .dependOnClassesThat()
          .resideInAPackage("io.github.mundanej.mjdi.generator..");

  private ShowcaseArchitectureTest() {}
}
