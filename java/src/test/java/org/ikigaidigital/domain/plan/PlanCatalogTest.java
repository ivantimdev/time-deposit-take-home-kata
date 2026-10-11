package org.ikigaidigital.domain.plan;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PlanCatalogTest {

  private final PlanCatalog standard = PlanCatalog.standard();

  @Test
  void standardBasicPlanEarnsOnePercentAfterThirtyDays() {
    assertThat(standard.planFor("basic"))
        .contains(
            new Plan(
                PlanType.BASIC,
                new FixedRateInterest(new AnnualRate(0.01), EarningPeriod.after(30))));
  }

  @Test
  void standardStudentPlanEarnsThreePercentFromDayThirtyOneToDayThreeHundredSixtyFive() {
    assertThat(standard.planFor("student"))
        .contains(
            new Plan(
                PlanType.STUDENT,
                new FixedRateInterest(new AnnualRate(0.03), EarningPeriod.between(31, 365))));
  }

  @Test
  void standardPremiumPlanEarnsFivePercentAfterFortyFiveDays() {
    assertThat(standard.planFor("premium"))
        .contains(
            new Plan(
                PlanType.PREMIUM,
                new FixedRateInterest(new AnnualRate(0.05), EarningPeriod.after(45))));
  }

  @ParameterizedTest(name = "[{0}]")
  @ValueSource(strings = {"Basic", "gold"})
  void standardHasNoPlanForAnUnknownCode(String code) {
    assertThat(standard.planFor(code)).isEmpty();
  }

  @Test
  void customCatalogueHasNoPlanForATypeItDoesNotHold() {
    final PlanCatalog basicOnly =
        PlanCatalog.of(
            new Plan(
                PlanType.BASIC,
                new FixedRateInterest(new AnnualRate(0.12), EarningPeriod.from(1))));

    assertThat(basicOnly.planFor("student")).isEmpty();
  }
}
