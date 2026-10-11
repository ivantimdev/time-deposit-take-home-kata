package org.ikigaidigital.domain.plan;

import static org.assertj.core.api.Assertions.assertThat;

import org.ikigaidigital.TimeDeposit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FixedRateInterestTest {

  private final FixedRateInterest interest =
      new FixedRateInterest(new AnnualRate(0.03), EarningPeriod.between(31, 365));

  @ParameterizedTest(name = "day {0} -> {1}")
  @CsvSource({"30, 0.0", "31, 2.5", "365, 2.5", "366, 0.0"})
  void appliesTheRateOnlyWithinTheEarningPeriod(int days, double expectedInterest) {
    final TimeDeposit deposit = new TimeDeposit(1, "student", 1000.0, days);

    assertThat(interest.monthlyInterest(deposit)).isEqualTo(expectedInterest);
  }
}
