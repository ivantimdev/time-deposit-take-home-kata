package org.ikigaidigital.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.ikigaidigital.TimeDeposit;
import org.junit.jupiter.api.Test;

class TimeDepositDetailsTest {

  private static final TimeDeposit DEPOSIT = new TimeDeposit(1, "basic", 1000.0, 45);
  private static final Withdrawal FIRST =
      new Withdrawal(1, 1, new BigDecimal("100.00"), LocalDate.of(2024, 1, 15));
  private static final Withdrawal SECOND =
      new Withdrawal(2, 1, new BigDecimal("50.00"), LocalDate.of(2024, 1, 10));
  private static final Withdrawal THIRD =
      new Withdrawal(3, 1, new BigDecimal("25.50"), LocalDate.of(2024, 2, 1));

  @Test
  void ordersWithdrawalsById() {
    TimeDepositDetails details = new TimeDepositDetails(DEPOSIT, List.of(THIRD, FIRST, SECOND));

    assertThat(details.withdrawals()).containsExactly(FIRST, SECOND, THIRD);
  }

  @Test
  void keepsTheGivenDeposit() {
    TimeDepositDetails details = new TimeDepositDetails(DEPOSIT, List.of());

    assertThat(details.deposit()).isSameAs(DEPOSIT);
  }

  @Test
  void withdrawalsCannotBeModified() {
    TimeDepositDetails details = new TimeDepositDetails(DEPOSIT, List.of(FIRST));

    assertThatThrownBy(() -> details.withdrawals().add(SECOND))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void withdrawalsDoNotFollowLaterChangesToTheGivenList() {
    List<Withdrawal> given = new ArrayList<>(List.of(FIRST));
    TimeDepositDetails details = new TimeDepositDetails(DEPOSIT, given);

    given.add(SECOND);

    assertThat(details.withdrawals()).containsExactly(FIRST);
  }
}
