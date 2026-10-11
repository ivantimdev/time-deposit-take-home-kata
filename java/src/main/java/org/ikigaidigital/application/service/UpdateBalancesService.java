package org.ikigaidigital.application.service;

import java.util.List;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.application.port.in.UpdateBalancesUseCase;
import org.ikigaidigital.application.port.out.TimeDepositRepository;
import org.ikigaidigital.domain.TimeDepositDetails;
import org.springframework.transaction.annotation.Transactional;

public class UpdateBalancesService implements UpdateBalancesUseCase {

  private final TimeDepositRepository repository;
  private final TimeDepositCalculator calculator;

  public UpdateBalancesService(TimeDepositRepository repository, TimeDepositCalculator calculator) {
    this.repository = repository;
    this.calculator = calculator;
  }

  // ASSUMPTION A2: the use case owns the transaction boundary.
  // ASSUMPTION A10: concurrent updates serialise on the row lock held until this transaction ends.
  @Transactional
  @Override
  public List<TimeDepositDetails> updateBalances() {
    List<TimeDeposit> deposits = repository.loadAllForUpdate();
    calculator.updateBalance(deposits);
    repository.saveAll(deposits);
    // ASSUMPTION A8: the update returns the updated deposits in the shape the list returns.
    return repository.loadAllWithWithdrawals();
  }
}
