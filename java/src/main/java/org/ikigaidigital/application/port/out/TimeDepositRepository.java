package org.ikigaidigital.application.port.out;

import java.util.List;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.domain.TimeDepositDetails;

public interface TimeDepositRepository {

  // ASSUMPTION A10: the rows stay locked until the caller's transaction ends, so concurrent
  // updates cannot interleave; no idempotency key is needed.
  List<TimeDeposit> loadAllForUpdate();

  void saveAll(List<TimeDeposit> deposits);

  // ASSUMPTION A9: deposits and their withdrawals are returned in ascending id order.
  List<TimeDepositDetails> loadAllWithWithdrawals();
}
