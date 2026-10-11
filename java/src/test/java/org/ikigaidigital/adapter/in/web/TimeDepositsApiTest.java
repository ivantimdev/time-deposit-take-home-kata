package org.ikigaidigital.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.ikigaidigital.adapter.in.web.api.TimeDepositsApi;
import org.junit.jupiter.api.Test;

// ASSUMPTION A1: the generated interface, parsed from the contract by the build, declares one
// method per operation; this is the exact count behind the guard rail's line-based one.
class TimeDepositsApiTest {

  @Test
  void contractDeclaresExactlyTheTwoOperations() {
    assertThat(TimeDepositsApi.class.getMethods())
        .extracting(Method::getName)
        .containsExactlyInAnyOrder("listTimeDeposits", "updateBalances");
  }
}
