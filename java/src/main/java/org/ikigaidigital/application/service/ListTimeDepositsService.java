package org.ikigaidigital.application.service;

import java.util.List;
import org.ikigaidigital.application.port.in.ListTimeDepositsUseCase;
import org.ikigaidigital.application.port.out.TimeDepositRepository;
import org.ikigaidigital.domain.TimeDepositDetails;
import org.springframework.transaction.annotation.Transactional;

public class ListTimeDepositsService implements ListTimeDepositsUseCase {

  private final TimeDepositRepository repository;

  public ListTimeDepositsService(TimeDepositRepository repository) {
    this.repository = repository;
  }

  // ASSUMPTION A2: the use case owns the transaction boundary.
  @Transactional(readOnly = true)
  @Override
  public List<TimeDepositDetails> listTimeDeposits() {
    return repository.loadAllWithWithdrawals();
  }
}
