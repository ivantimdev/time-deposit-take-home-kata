package org.ikigaidigital.adapter.out.persistence;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.domain.Withdrawal;

final class TimeDepositMapper {

  private TimeDepositMapper() {}

  // ASSUMPTION A21: balances up to 15 significant digits round-trip exactly through the Double.
  static TimeDeposit toDomain(TimeDepositEntity entity) {
    return new TimeDeposit(
        entity.getId(),
        entity.getPlanType().code(),
        entity.getBalance().doubleValue(),
        entity.getDays());
  }

  static Withdrawal toDomain(WithdrawalEntity entity) {
    return new Withdrawal(
        entity.getId(), entity.getTimeDepositId(), entity.getAmount(), entity.getDate());
  }

  // ASSUMPTION A13: BigDecimal.valueOf reads the double's shortest decimal form, which is the
  // cents the calculator produced; new BigDecimal(double) would expose binary noise.
  static BigDecimal toBalanceColumn(Double balance) {
    return BigDecimal.valueOf(balance).setScale(2, RoundingMode.HALF_UP);
  }
}
