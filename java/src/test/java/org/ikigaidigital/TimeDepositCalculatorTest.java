package org.ikigaidigital;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TimeDepositCalculatorTest {

  private static final List<String> PLAN_TYPES = List.of("basic", "student", "premium");

  private final TimeDepositCalculator calculator = new TimeDepositCalculator();

  private double balanceAfterOneUpdate(String planType, double balance, int days) {
    final TimeDeposit deposit = new TimeDeposit(1, planType, balance, days);
    calculator.updateBalance(new ArrayList<>(List.of(deposit)));
    return deposit.getBalance();
  }

  private static List<TimeDeposit> tenThousandDeposits() {
    return IntStream.rangeClosed(1, 10_000)
        .mapToObj(id -> new TimeDeposit(id, PLAN_TYPES.get(id % 3), 1000.0 + id, 31 + id % 400))
        .collect(Collectors.toCollection(ArrayList::new));
  }

  @Nested
  class InterestByPlanAndAge {

    // -1 and Integer.MAX_VALUE are not requirements: they pin that a negative age earns nothing
    // and that the largest age does not overflow.
    @ParameterizedTest(name = "{0} at {1} days -> {2}")
    @CsvSource(
        textBlock =
            """
            basic,   -1,         1234567.0
            basic,   0,          1234567.0
            basic,   1,          1234567.0
            basic,   2,          1234567.0
            basic,   15,         1234567.0
            basic,   29,         1234567.0
            basic,   30,         1234567.0
            basic,   31,         1235595.81
            basic,   32,         1235595.81
            basic,   44,         1235595.81
            basic,   45,         1235595.81
            basic,   46,         1235595.81
            basic,   47,         1235595.81
            basic,   60,         1235595.81
            basic,   90,         1235595.81
            basic,   180,        1235595.81
            basic,   300,        1235595.81
            basic,   364,        1235595.81
            basic,   365,        1235595.81
            basic,   366,        1235595.81
            basic,   367,        1235595.81
            basic,   1000,       1235595.81
            basic,   10000,      1235595.81
            basic,   2147483647, 1235595.81
            student, -1,         1234567.0
            student, 0,          1234567.0
            student, 1,          1234567.0
            student, 2,          1234567.0
            student, 15,         1234567.0
            student, 29,         1234567.0
            student, 30,         1234567.0
            student, 31,         1237653.42
            student, 32,         1237653.42
            student, 44,         1237653.42
            student, 45,         1237653.42
            student, 46,         1237653.42
            student, 47,         1237653.42
            student, 60,         1237653.42
            student, 90,         1237653.42
            student, 180,        1237653.42
            student, 300,        1237653.42
            student, 364,        1237653.42
            student, 365,        1237653.42
            student, 366,        1234567.0
            student, 367,        1234567.0
            student, 1000,       1234567.0
            student, 10000,      1234567.0
            student, 2147483647, 1234567.0
            premium, -1,         1234567.0
            premium, 0,          1234567.0
            premium, 1,          1234567.0
            premium, 2,          1234567.0
            premium, 15,         1234567.0
            premium, 29,         1234567.0
            premium, 30,         1234567.0
            premium, 31,         1234567.0
            premium, 32,         1234567.0
            premium, 44,         1234567.0
            premium, 45,         1234567.0
            premium, 46,         1239711.03
            premium, 47,         1239711.03
            premium, 60,         1239711.03
            premium, 90,         1239711.03
            premium, 180,        1239711.03
            premium, 300,        1239711.03
            premium, 364,        1239711.03
            premium, 365,        1239711.03
            premium, 366,        1239711.03
            premium, 367,        1239711.03
            premium, 1000,       1239711.03
            premium, 10000,      1239711.03
            premium, 2147483647, 1239711.03
            """)
    void creditsOneMonthOfInterestOnlyWithinThePlanAgeWindow(
        String planType, int days, double expectedBalance) {
      assertThat(balanceAfterOneUpdate(planType, 1234567.00, days)).isEqualTo(expectedBalance);
    }

    @ParameterizedTest(name = "{0} with balance {1} -> {2}")
    @CsvSource(
        textBlock =
            """
            basic,   0.00,             0.0
            basic,   0.01,             0.01
            basic,   0.10,             0.1
            basic,   1.00,             1.0
            basic,   99.99,            100.07
            basic,   100.00,           100.08
            basic,   999.99,           1000.82
            basic,   1000.00,          1000.83
            basic,   123456.78,        123559.66
            basic,   999999999.99,     1.00083333332E9
            basic,   1000000000000.00, 1.00083333333333E12
            student, 0.00,             0.0
            student, 0.01,             0.01
            student, 0.10,             0.1
            student, 1.00,             1.0
            student, 99.99,            100.24
            student, 100.00,           100.25
            student, 999.99,           1002.49
            student, 1000.00,          1002.5
            student, 123456.78,        123765.42
            student, 999999999.99,     1.00249999999E9
            student, 1000000000000.00, 1.0025E12
            premium, 0.00,             0.0
            premium, 0.01,             0.01
            premium, 0.10,             0.1
            premium, 1.00,             1.0
            premium, 99.99,            100.41
            premium, 100.00,           100.42
            premium, 999.99,           1004.16
            premium, 1000.00,          1004.17
            premium, 123456.78,        123971.18
            premium, 999999999.99,     1.00416666666E9
            premium, 1000000000000.00, 1.00416666666667E12
            """)
    void creditsInterestRoundedToCentsForEveryBalance(
        String planType, double balance, double expectedBalance) {
      assertThat(balanceAfterOneUpdate(planType, balance, 100)).isEqualTo(expectedBalance);
    }
  }

  @Nested
  class PlanTypeMatching {

    @ParameterizedTest(name = "[{0}] earns nothing")
    @ValueSource(
        strings = {
          "BASIC", "Basic", "bAsIc", "STUDENT", "Premium", "basic ", " basic", "gold", "", "basic\n"
        })
    void creditsNoInterestToAPlanTypeThatIsNotAnExactLowercaseMatch(String planType) {
      assertThat(balanceAfterOneUpdate(planType, 1000.00, 100)).isEqualTo(1000.0);
    }
  }

  @Nested
  class Rounding {

    @Test
    void roundsAnExactHalfCentUp() {
      assertThat(balanceAfterOneUpdate("basic", 150.00, 45)).isEqualTo(150.13);
    }

    @ParameterizedTest(name = "{0} with balance {1} -> {2}")
    @CsvSource(
        textBlock =
            """
            basic,   18.00, 18.01
            basic,   90.00, 90.07
            student, 6.00,  6.01
            student, 30.00, 30.07
            premium, 18.00, 18.07
            premium, 25.20, 25.3
            """)
    void roundsTheExactBinaryValueOfTheInterestNotItsShortestDecimal(
        String planType, double balance, double expectedBalance) {
      assertThat(balanceAfterOneUpdate(planType, balance, 100)).isEqualTo(expectedBalance);
    }

    @ParameterizedTest(name = "{0} with balance {1} -> {2}")
    @CsvSource(
        textBlock =
            """
            basic,   6.02, 6.029999999999999
            student, 2.01, 2.0199999999999996
            premium, 1.37, 1.3800000000000001
            """)
    void keepsTheDoubleAdditionTailInTheBalance(
        String planType, double balance, double expectedBalance) {
      assertThat(balanceAfterOneUpdate(planType, balance, 100)).isEqualTo(expectedBalance);
    }

    @Test
    void roundsPositiveInterestBelowHalfACentToZero() {
      assertThat(balanceAfterOneUpdate("basic", 5.99, 100)).isEqualTo(5.99);
    }

    @Test
    void roundsInterestAboveHalfACentToOneCent() {
      assertThat(balanceAfterOneUpdate("basic", 10.00, 100)).isEqualTo(10.01);
    }

    // Not a requirement: pins the sign handling of the original arithmetic on a negative balance.
    @Test
    void debitsInterestFromANegativeBalance() {
      assertThat(balanceAfterOneUpdate("basic", -1000.00, 45)).isEqualTo(-1000.83);
    }
  }

  @Nested
  class ListHandling {

    private final TimeDeposit basic = new TimeDeposit(1, "basic", 1000.00, 100);
    private final TimeDeposit student = new TimeDeposit(2, "student", 2500.50, 200);
    private final TimeDeposit premium = new TimeDeposit(3, "premium", 10000.00, 400);
    private final List<TimeDeposit> deposits = new ArrayList<>(List.of(basic, student, premium));

    @Test
    void keepsTheSameDepositInstancesInTheSameOrder() {
      calculator.updateBalance(deposits);

      assertThat(deposits).hasSize(3);
      assertThat(deposits.get(0)).isSameAs(basic);
      assertThat(deposits.get(1)).isSameAs(student);
      assertThat(deposits.get(2)).isSameAs(premium);
    }

    @Test
    void leavesIdPlanTypeAndDaysUnchanged() {
      calculator.updateBalance(deposits);

      assertThat(deposits)
          .extracting(TimeDeposit::getId, TimeDeposit::getPlanType, TimeDeposit::getDays)
          .containsExactly(
              tuple(1, "basic", 100), tuple(2, "student", 200), tuple(3, "premium", 400));
    }

    @Test
    void updatesTheBalanceOfEveryDepositInPlace() {
      calculator.updateBalance(deposits);

      assertThat(deposits)
          .extracting(TimeDeposit::getBalance)
          .containsExactly(1000.83, 2506.75, 10041.67);
    }

    @Test
    void creditsTheSameInstanceTwiceWhenItAppearsTwice() {
      final TimeDeposit deposit = new TimeDeposit(1, "premium", 1000.00, 60);

      calculator.updateBalance(Arrays.asList(deposit, deposit));

      assertThat(deposit.getBalance()).isEqualTo(1008.3499999999999);
    }

    @Test
    void updatesDepositsSharingAnIdIndependently() {
      final TimeDeposit smaller = new TimeDeposit(7, "basic", 1000.00, 100);
      final TimeDeposit larger = new TimeDeposit(7, "basic", 2000.00, 100);

      calculator.updateBalance(Arrays.asList(smaller, larger));

      assertThat(List.of(smaller.getBalance(), larger.getBalance()))
          .containsExactly(1000.83, 2001.67);
    }

    @Test
    void acceptsAnEmptyList() {
      final List<TimeDeposit> empty = new ArrayList<>();

      assertThatCode(() -> calculator.updateBalance(empty)).doesNotThrowAnyException();
      assertThat(empty).isEmpty();
    }

    @Test
    void acceptsAnUnmodifiableListBecauseOnlyTheDepositsChange() {
      final TimeDeposit first = new TimeDeposit(1, "basic", 1000.00, 100);
      final TimeDeposit second = new TimeDeposit(2, "premium", 2000.00, 100);

      calculator.updateBalance(List.of(first, second));

      assertThat(List.of(first.getBalance(), second.getBalance()))
          .containsExactly(1000.83, 2008.33);
    }

    @Test
    void acceptsASingletonList() {
      final TimeDeposit deposit = new TimeDeposit(1, "student", 1000.00, 100);

      calculator.updateBalance(Collections.singletonList(deposit));

      assertThat(deposit.getBalance()).isEqualTo(1002.5);
    }

    @Test
    void updatesEveryDepositOfALargeList() {
      final List<TimeDeposit> large = tenThousandDeposits();

      calculator.updateBalance(large);

      assertThat(List.of(large.get(0), large.get(4999), large.get(9999)))
          .extracting(TimeDeposit::getBalance)
          .containsExactly(1003.5, 6025.0, 11027.5);
    }
  }

  @Nested
  class RepeatedCalls {

    @Test
    void compoundsInterestOnEveryCall() {
      final TimeDeposit deposit = new TimeDeposit(1, "basic", 1000.00, 45);
      final List<TimeDeposit> deposits = List.of(deposit);

      calculator.updateBalance(deposits);
      final double afterFirstCall = deposit.getBalance();
      calculator.updateBalance(deposits);
      final double afterSecondCall = deposit.getBalance();
      calculator.updateBalance(deposits);
      final double afterThirdCall = deposit.getBalance();

      assertThat(List.of(afterFirstCall, afterSecondCall, afterThirdCall))
          .containsExactly(1000.83, 1001.6600000000001, 1002.4900000000001);
    }

    @Test
    void compoundsStudentInterestWithinTheFirstYear() {
      final TimeDeposit deposit = new TimeDeposit(1, "student", 1000.00, 365);
      final List<TimeDeposit> deposits = List.of(deposit);

      calculator.updateBalance(deposits);
      final double afterFirstCall = deposit.getBalance();
      calculator.updateBalance(deposits);
      final double afterSecondCall = deposit.getBalance();

      assertThat(List.of(afterFirstCall, afterSecondCall)).containsExactly(1002.5, 1005.01);
    }

    @Test
    void stopsStudentInterestAfterTheFirstYear() {
      final TimeDeposit deposit = new TimeDeposit(2, "student", 1000.00, 366);
      final List<TimeDeposit> deposits = List.of(deposit);

      calculator.updateBalance(deposits);
      final double afterFirstCall = deposit.getBalance();
      calculator.updateBalance(deposits);
      final double afterSecondCall = deposit.getBalance();

      assertThat(List.of(afterFirstCall, afterSecondCall)).containsExactly(1000.0, 1000.0);
    }
  }
}
