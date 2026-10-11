package org.ikigaidigital.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

// ASSUMPTION A1: the build generates one interface per contract tag, each with one method per
// operation; together they give the exact count that the guard rail's line-based one cannot.
class TimeDepositsApiTest {

  @Test
  void generatedInterfacesDeclareExactlyTheTwoOperations() {
    assertThat(new ClassFileImporter().importPackages("org.ikigaidigital.adapter.in.web.api"))
        .filteredOn(JavaClass::isInterface)
        .flatExtracting(JavaClass::getMethods)
        .extracting(JavaMethod::getName)
        .containsExactlyInAnyOrder("listTimeDeposits", "updateBalances");
  }
}
