package org.ikigaidigital.application.port.out;

import java.util.List;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.domain.TimeDepositDetails;

public interface TimeDepositRepository {

  /**
   * Loads all deposits ordered by id and locks them for the current transaction: no concurrent
   * update of the same rows may interleave until the transaction ends.
   */
  // ASSUMPTION A10: the lock, not an idempotency key, keeps concurrent updates from interleaving.
  List<TimeDeposit> loadAllForUpdate();

  void saveAll(List<TimeDeposit> deposits);

  /** Loads all deposits ordered by id, each with its withdrawals ordered by id. */
  // ASSUMPTION A9: deposits and their withdrawals are returned in ascending id order.
  List<TimeDepositDetails> loadAllWithWithdrawals();
}
