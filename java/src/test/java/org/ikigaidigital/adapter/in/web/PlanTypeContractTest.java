package org.ikigaidigital.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.ikigaidigital.adapter.in.web.model.TimeDepositResponse;
import org.ikigaidigital.domain.plan.PlanType;
import org.junit.jupiter.api.Test;

// A plan added to the catalogue must also be added to the contract's planType enum.
class PlanTypeContractTest {

  @Test
  void contractListsExactlyThePlanTypeCodes() {
    assertThat(Arrays.stream(TimeDepositResponse.PlanTypeEnum.values()))
        .extracting(TimeDepositResponse.PlanTypeEnum::getValue)
        .containsExactlyElementsOf(Arrays.stream(PlanType.values()).map(PlanType::code).toList());
  }
}
