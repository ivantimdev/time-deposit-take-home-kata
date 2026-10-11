package org.ikigaidigital;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.ikigaidigital.domain.plan.AnnualRate;
import org.ikigaidigital.domain.plan.EarningPeriod;
import org.ikigaidigital.domain.plan.FixedRateInterest;
import org.ikigaidigital.domain.plan.Plan;
import org.ikigaidigital.domain.plan.PlanCatalog;
import org.ikigaidigital.domain.plan.PlanType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TimeDepositCalculatorWithCatalogTest {

  private final TimeDepositCalculator calculator =
      new TimeDepositCalculator(
          PlanCatalog.of(
              new Plan(
                  PlanType.BASIC,
                  new FixedRateInterest(new AnnualRate(0.12), EarningPeriod.after(0)))));

  @ParameterizedTest(name = "basic {0} at {1} days -> {2}")
  @CsvSource(
      textBlock =
          """
          1000.0,    0,  1000.0
          1000.0,    1,  1010.0
          1000.0,    30, 1010.0
          1234567.0, 31, 1246912.67
          18.0,      1,  18.18
          0.04,      1,  0.04
          """)
  void appliesThePlanOfTheGivenCatalogue(double balance, int days, double expectedBalance) {
    final TimeDeposit deposit = new TimeDeposit(1, "basic", balance, days);

    calculator.updateBalance(List.of(deposit));

    assertThat(deposit.getBalance()).isEqualTo(expectedBalance);
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"student", "premium"})
  void earnsNothingForAPlanTypeTheCatalogueDoesNotHold(String planType) {
    final TimeDeposit deposit = new TimeDeposit(1, planType, 1000.0, 100);

    calculator.updateBalance(List.of(deposit));

    assertThat(deposit.getBalance()).isEqualTo(1000.0);
  }

  // ASSUMPTION A6: a null plan type earns no interest; the original threw NullPointerException.
  @Test
  void leavesTheBalanceUnchangedWhenThePlanTypeIsNull() {
    final TimeDeposit deposit = new TimeDeposit(1, null, 1000.0, 100);

    calculator.updateBalance(List.of(deposit));

    assertThat(deposit.getBalance()).isEqualTo(1000.0);
  }
}
