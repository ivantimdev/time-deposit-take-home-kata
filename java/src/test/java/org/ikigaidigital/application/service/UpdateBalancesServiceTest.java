package org.ikigaidigital.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ikigaidigital.application.InMemoryTimeDepositRepository.LOAD_ALL_FOR_UPDATE;
import static org.ikigaidigital.application.InMemoryTimeDepositRepository.LOAD_ALL_WITH_WITHDRAWALS;
import static org.ikigaidigital.application.InMemoryTimeDepositRepository.SAVE_ALL;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.application.InMemoryTimeDepositRepository;
import org.ikigaidigital.domain.TimeDepositDetails;
import org.ikigaidigital.domain.Withdrawal;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class UpdateBalancesServiceTest {

  private static final Withdrawal FIRST_WITHDRAWAL =
      new Withdrawal(1, 2, new BigDecimal("100.00"), LocalDate.of(2024, 1, 15));
  private static final Withdrawal SECOND_WITHDRAWAL =
      new Withdrawal(2, 2, new BigDecimal("50.00"), LocalDate.of(2024, 2, 15));

  private final InMemoryTimeDepositRepository repository =
      new InMemoryTimeDepositRepository()
          .add(new TimeDeposit(3, "premium", 3000.0, 46))
          .add(new TimeDeposit(1, "basic", 1000.0, 31))
          .add(new TimeDeposit(4, "basic", 500.0, 10))
          .add(new TimeDeposit(2, "student", 2000.0, 100), SECOND_WITHDRAWAL, FIRST_WITHDRAWAL);

  private final UpdateBalancesService service =
      new UpdateBalancesService(repository, new TimeDepositCalculator());

  private static List<TimeDeposit> depositsById() {
    return List.of(
        new TimeDeposit(1, "basic", 1000.0, 31),
        new TimeDeposit(2, "student", 2000.0, 100),
        new TimeDeposit(3, "premium", 3000.0, 46),
        new TimeDeposit(4, "basic", 500.0, 10));
  }

  @Test
  void appliesOneMonthOfInterestToEveryDeposit() {
    List<TimeDeposit> expected = depositsById();
    new TimeDepositCalculator().updateBalance(expected);

    List<TimeDepositDetails> updated = service.updateBalances();

    assertThat(updated)
        .extracting(details -> details.deposit().getBalance())
        .containsExactlyElementsOf(expected.stream().map(TimeDeposit::getBalance).toList());
  }

  @Test
  void savesTheUpdatedBalances() {
    List<TimeDeposit> expected = depositsById();
    new TimeDepositCalculator().updateBalance(expected);

    service.updateBalances();

    assertThat(repository.loadAllWithWithdrawals())
        .extracting(details -> details.deposit().getBalance())
        .containsExactlyElementsOf(expected.stream().map(TimeDeposit::getBalance).toList());
  }

  @Test
  void savesExactlyTheDepositsItLoadedForUpdate() {
    service.updateBalances();

    assertThat(repository.saved()).containsExactlyElementsOf(repository.loadedForUpdate());
  }

  @Test
  void locksTheDepositsBeforeSavingAndReadsTheResultAfterwards() {
    service.updateBalances();

    assertThat(repository.calls())
        .containsExactly(LOAD_ALL_FOR_UPDATE, SAVE_ALL, LOAD_ALL_WITH_WITHDRAWALS);
  }

  @Test
  void returnsTheWithdrawalsUntouched() {
    List<TimeDepositDetails> updated = service.updateBalances();

    assertThat(updated)
        .extracting(TimeDepositDetails::withdrawals)
        .containsExactly(
            List.of(), List.of(FIRST_WITHDRAWAL, SECOND_WITHDRAWAL), List.of(), List.of());
  }

  @Test
  void returnsTheDepositsOrderedById() {
    List<TimeDepositDetails> updated = service.updateBalances();

    assertThat(updated)
        .extracting(details -> details.deposit().getId())
        .containsExactly(1, 2, 3, 4);
  }

  @Test
  void returnsNothingForAnEmptyRepository() {
    UpdateBalancesService emptyService =
        new UpdateBalancesService(new InMemoryTimeDepositRepository(), new TimeDepositCalculator());

    assertThat(emptyService.updateBalances()).isEmpty();
  }

  @Test
  void updatesBalancesInOneReadWriteTransaction() throws NoSuchMethodException {
    Transactional transactional =
        UpdateBalancesService.class.getMethod("updateBalances").getAnnotation(Transactional.class);

    assertThat(transactional.readOnly()).isFalse();
  }
}
