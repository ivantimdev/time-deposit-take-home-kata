package org.ikigaidigital;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMembers;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.CompositeArchRule;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

  private static final JavaClasses PRODUCTION_CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("org.ikigaidigital");

  private static final DescribedPredicate<JavaClass> ROOT_DOMAIN_CLASSES =
      belongToAnyOf(TimeDeposit.class, TimeDepositCalculator.class);

  private static final DescribedPredicate<JavaClass> DOMAIN_ZONE =
      resideInAPackage("org.ikigaidigital.domain..")
          .or(ROOT_DOMAIN_CLASSES)
          .as("the domain zone (org.ikigaidigital.domain.., TimeDeposit, TimeDepositCalculator)");

  private static final DescribedPredicate<Object> ANY_ANNOTATION =
      DescribedPredicate.alwaysTrue().as("any annotation");

  @Test
  void domainZoneDependsOnlyOnTheJdkAndItself() {
    classes()
        .that(DOMAIN_ZONE)
        .should()
        .onlyDependOnClassesThat(resideInAPackage("java..").or(DOMAIN_ZONE))
        .check(PRODUCTION_CLASSES);
  }

  @Test
  void domainPackagesUseOnlyTimeDepositFromTheRootPackage() {
    noClasses()
        .that()
        .resideInAPackage("org.ikigaidigital.domain..")
        .should()
        .dependOnClassesThat(
            resideInAPackage("org.ikigaidigital")
                .and(DescribedPredicate.not(belongToAnyOf(TimeDeposit.class))))
        .check(PRODUCTION_CLASSES);
  }

  @Test
  void applicationDependsOnlyOnTheJdkTheDomainZoneItselfAndTransactions() {
    classes()
        .that()
        .resideInAPackage("org.ikigaidigital.application..")
        .should()
        .onlyDependOnClassesThat(
            resideInAnyPackage(
                    "java..",
                    "org.ikigaidigital.application..",
                    "org.springframework.transaction..")
                .or(DOMAIN_ZONE))
        .check(PRODUCTION_CLASSES);
  }

  @Test
  void applicationIsFreeOfPersistenceWebAndAdapterDependencies() {
    noClasses()
        .that()
        .resideInAPackage("..application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "jakarta.persistence..",
            "org.springframework.data..",
            "org.springframework.web..",
            "com.fasterxml..",
            "..adapter..",
            "..config..")
        .check(PRODUCTION_CLASSES);
  }

  @Test
  void inboundAndOutboundAdaptersDoNotDependOnEachOther() {
    slices().matching("..adapter.(*)..").should().notDependOnEachOther().check(PRODUCTION_CLASSES);
  }

  @Test
  void onlyConfigurationDependsOnConfiguration() {
    noClasses()
        .that()
        .resideOutsideOfPackage("..config..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..config..")
        .check(PRODUCTION_CLASSES);
  }

  @Test
  void onlyAdaptersAndConfigurationDependOnAdapters() {
    noClasses()
        .that()
        .resideOutsideOfPackages("..adapter..", "..config..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..adapter..")
        .check(PRODUCTION_CLASSES);
  }

  @Test
  void packagesAreFreeOfCycles() {
    slices()
        .matching("org.ikigaidigital.(*)..")
        .should()
        .beFreeOfCycles()
        .check(PRODUCTION_CLASSES);
  }

  @Test
  void rootDomainClassesCarryNoAnnotations() {
    CompositeArchRule.of(
            noClasses().that(ROOT_DOMAIN_CLASSES).should().beAnnotatedWith(ANY_ANNOTATION))
        .and(
            noMembers()
                .that()
                .areDeclaredInClassesThat(ROOT_DOMAIN_CLASSES)
                .should()
                .beAnnotatedWith(ANY_ANNOTATION))
        .check(PRODUCTION_CLASSES);
  }

  @Test
  void domainZoneNeverDependsOnApplicationAdaptersConfigurationOrOtherRootClasses() {
    noClasses()
        .that(DOMAIN_ZONE)
        .should()
        .dependOnClassesThat(
            resideInAnyPackage("..application..", "..adapter..", "..config..")
                .or(
                    resideInAPackage("org.ikigaidigital")
                        .and(DescribedPredicate.not(ROOT_DOMAIN_CLASSES))))
        .check(PRODUCTION_CLASSES);
  }
}
