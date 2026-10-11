package org.ikigaidigital.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

// ASSUMPTION A1: the build's parser gives the exact operation count the line-based guard rail
// cannot; every interface of the package is scanned so that no tag can hide an operation.
class GeneratedApiInterfacesTest {

  @Test
  void declareExactlyTheTwoOperations() {
    assertThat(new ClassFileImporter().importPackages("org.ikigaidigital.adapter.in.web.api"))
        .filteredOn(JavaClass::isInterface)
        .flatExtracting(JavaClass::getMethods)
        .extracting(JavaMethod::getName)
        .containsExactlyInAnyOrder("listTimeDeposits", "updateBalances");
  }
}
