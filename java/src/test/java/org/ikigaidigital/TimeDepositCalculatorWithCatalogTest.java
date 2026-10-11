package org.ikigaidigital;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.ikigaidigital.domain.plan.AnnualRate;
import org.ikigaidigital.domain.plan.EarningPeriod;
import org.ikigaidigital.domain.plan.FixedRateInterest;
import org.ikigaidigital.domain.plan.Plan;
import org.ikigaidigital.domain.plan.PlanCatalog;
import org.ikigaidigital.domain.plan.PlanType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TimeDepositCalculatorWithCatalogTest {

  private final TimeDepositCalculator calculator =
      new TimeDepositCalculator(
          PlanCatalog.of(
              new Plan(
                  PlanType.BASIC,
                  new FixedRateInterest(new AnnualRate(0.12), EarningPeriod.from(1)))));

  @ParameterizedTest(name = "{0} {1} at {2} days -> {3}")
  @CsvSource(
      textBlock =
          """
          basic,   1000.0,    0,   1000.0
          basic,   1000.0,    1,   1010.0
          basic,   1000.0,    30,  1010.0
          basic,   1234567.0, 31,  1246912.67
          basic,   18.0,      1,   18.18
          basic,   0.04,      1,   0.04
          student, 1000.0,    100, 1000.0
          premium, 1000.0,    100, 1000.0
          """)
  void appliesOnlyThePlansOfTheGivenCatalogue(
      String planType, double balance, int days, double expectedBalance) {
    final TimeDeposit deposit = new TimeDeposit(1, planType, balance, days);

    calculator.updateBalance(List.of(deposit));

    assertThat(deposit.getBalance()).isEqualTo(expectedBalance);
  }
}
