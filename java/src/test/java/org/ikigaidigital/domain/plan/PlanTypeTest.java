package org.ikigaidigital.domain.plan;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PlanTypeTest {

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource({"basic, BASIC", "student, STUDENT", "premium, PREMIUM"})
  void findsThePlanTypeOfAnExactCode(String code, PlanType expected) {
    assertThat(PlanType.fromCode(code)).contains(expected);
  }

  @ParameterizedTest(name = "[{0}]")
  @NullAndEmptySource
  @ValueSource(strings = {"Basic", "BASIC", "basic ", "gold"})
  void findsNoPlanTypeForAnyOtherCode(String code) {
    assertThat(PlanType.fromCode(code)).isEmpty();
  }

  @Test
  void exposesTheStoredCodeOfEachPlanType() {
    assertThat(PlanType.values())
        .extracting(PlanType::code)
        .containsExactly("basic", "student", "premium");
  }
}
