package org.ikigaidigital.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.ikigaidigital.TimeDeposit;

public record TimeDepositDetails(TimeDeposit deposit, List<Withdrawal> withdrawals) {

  public TimeDepositDetails {
    List<Withdrawal> byId = new ArrayList<>(withdrawals);
    byId.sort(Comparator.comparingInt(Withdrawal::id));
    withdrawals = List.copyOf(byId);
  }
}
