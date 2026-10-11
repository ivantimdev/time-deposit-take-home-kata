package org.ikigaidigital.domain.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EarningPeriodTest {

  @ParameterizedTest(name = "day {0} -> {1}")
  @CsvSource({"30, false", "31, true", "2147483647, true"})
  void periodAfterThirtyDaysStartsOnDayThirtyOne(int days, boolean included) {
    assertThat(EarningPeriod.after(30).includes(days)).isEqualTo(included);
  }

  @ParameterizedTest(name = "day {0} -> {1}")
  @CsvSource({"30, false", "31, true", "365, true", "366, false"})
  void periodBetweenDaysIncludesBothEnds(int days, boolean included) {
    assertThat(EarningPeriod.between(31, 365).includes(days)).isEqualTo(included);
  }

  @ParameterizedTest(name = "day {0} -> {1}")
  @CsvSource({"-1, false", "0, false", "1, true", "2147483647, true"})
  void periodFromDayOneIncludesDayOne(int days, boolean included) {
    assertThat(EarningPeriod.from(1).includes(days)).isEqualTo(included);
  }

  @Test
  void periodAfterTheLargestDayIsRejected() {
    assertThatThrownBy(() -> EarningPeriod.after(Integer.MAX_VALUE))
        .isInstanceOf(ArithmeticException.class);
  }

  @ParameterizedTest(name = "day {0} -> {1}")
  @CsvSource({"30, false", "31, true", "32, false"})
  void singleDayPeriodIncludesOnlyThatDay(int days, boolean included) {
    assertThat(EarningPeriod.between(31, 31).includes(days)).isEqualTo(included);
  }

  @Test
  void periodEndingBeforeItStartsIsRejected() {
    assertThatThrownBy(() -> EarningPeriod.between(32, 31))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContainingAll("32", "31");
  }
}
