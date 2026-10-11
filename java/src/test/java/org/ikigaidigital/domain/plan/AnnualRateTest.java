package org.ikigaidigital.domain.plan;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AnnualRateTest {

  // 18.00 at 1% pins the operand order: balance * (rate / 12) gives 0.015000000000000001.
  @ParameterizedTest(name = "{1} at {0} -> {2}")
  @CsvSource({
    "0.01, 18.0,      0.015",
    "0.03, 123456.78, 308.64195",
    "0.05, 25.2,      0.105",
    "0.12, 1000.0,    10.0"
  })
  void monthlyInterestIsATwelfthOfTheAnnualRateOnTheBalance(
      double rate, double balance, double expectedInterest) {
    assertThat(new AnnualRate(rate).monthlyInterestOn(balance)).isEqualTo(expectedInterest);
  }
}
