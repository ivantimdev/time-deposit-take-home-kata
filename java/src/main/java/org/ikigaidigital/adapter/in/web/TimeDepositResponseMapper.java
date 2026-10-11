package org.ikigaidigital.adapter.in.web;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.ikigaidigital.adapter.in.web.model.TimeDeposit;
import org.ikigaidigital.adapter.in.web.model.Withdrawal;
import org.ikigaidigital.domain.TimeDepositDetails;
import org.springframework.stereotype.Component;

@Component
class TimeDepositResponseMapper {

  List<TimeDeposit> toResponse(List<TimeDepositDetails> deposits) {
    return deposits.stream().map(TimeDepositResponseMapper::toResponse).toList();
  }

  private static TimeDeposit toResponse(TimeDepositDetails details) {
    var deposit = details.deposit();
    return new TimeDeposit(
        deposit.getId(),
        TimeDeposit.PlanTypeEnum.fromValue(deposit.getPlanType()),
        toAmount(deposit.getBalance()),
        deposit.getDays(),
        details.withdrawals().stream()
            .map(
                withdrawal ->
                    new Withdrawal(withdrawal.id(), withdrawal.amount(), withdrawal.date()))
            .toList());
  }

  // ASSUMPTION A13: BigDecimal.valueOf reads the double's shortest decimal form, the cents the
  // calculator produced; new BigDecimal(double) would expose binary noise.
  private static BigDecimal toAmount(Double balance) {
    return BigDecimal.valueOf(balance).setScale(2, RoundingMode.HALF_UP);
  }
}
