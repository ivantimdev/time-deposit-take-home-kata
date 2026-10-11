package org.ikigaidigital.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.ikigaidigital.application.InMemoryTimeDepositRepository.LOAD_ALL_WITH_WITHDRAWALS;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.InMemoryTimeDepositRepository;
import org.ikigaidigital.domain.TimeDepositDetails;
import org.ikigaidigital.domain.Withdrawal;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class ListTimeDepositsServiceTest {

  private static final Withdrawal FIRST_WITHDRAWAL =
      new Withdrawal(1, 2, new BigDecimal("100.00"), LocalDate.of(2024, 1, 15));
  private static final Withdrawal SECOND_WITHDRAWAL =
      new Withdrawal(2, 2, new BigDecimal("50.00"), LocalDate.of(2024, 2, 15));

  private final InMemoryTimeDepositRepository repository =
      new InMemoryTimeDepositRepository()
          .add(new TimeDeposit(3, "premium", 3000.0, 46))
          .add(new TimeDeposit(1, "basic", 1000.0, 31))
          .add(new TimeDeposit(2, "student", 2000.0, 100), SECOND_WITHDRAWAL, FIRST_WITHDRAWAL);

  private final ListTimeDepositsService service = new ListTimeDepositsService(repository);

  @Test
  void returnsEveryDepositUnchangedOrderedById() {
    List<TimeDepositDetails> listed = service.listTimeDeposits();

    assertThat(listed)
        .extracting(
            details -> details.deposit().getId(),
            details -> details.deposit().getPlanType(),
            details -> details.deposit().getBalance(),
            details -> details.deposit().getDays())
        .containsExactly(
            tuple(1, "basic", 1000.0, 31),
            tuple(2, "student", 2000.0, 100),
            tuple(3, "premium", 3000.0, 46));
  }

  @Test
  void returnsTheWithdrawalsOrderedById() {
    List<TimeDepositDetails> listed = service.listTimeDeposits();

    assertThat(listed)
        .extracting(TimeDepositDetails::withdrawals)
        .containsExactly(List.of(), List.of(FIRST_WITHDRAWAL, SECOND_WITHDRAWAL), List.of());
  }

  @Test
  void readsWithoutLockingOrSaving() {
    service.listTimeDeposits();

    assertThat(repository.calls()).containsExactly(LOAD_ALL_WITH_WITHDRAWALS);
  }

  @Test
  void returnsNothingForAnEmptyRepository() {
    ListTimeDepositsService emptyService =
        new ListTimeDepositsService(new InMemoryTimeDepositRepository());

    assertThat(emptyService.listTimeDeposits()).isEmpty();
  }

  @Test
  void listsInAReadOnlyTransaction() throws NoSuchMethodException {
    Transactional transactional =
        ListTimeDepositsService.class
            .getMethod("listTimeDeposits")
            .getAnnotation(Transactional.class);

    assertThat(transactional.readOnly()).isTrue();
  }
}
