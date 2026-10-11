package org.ikigaidigital.domain.plan;

import org.ikigaidigital.TimeDeposit;

public record FixedRateInterest(AnnualRate rate, EarningPeriod period) implements InterestPolicy {

  @Override
  public double monthlyInterest(TimeDeposit deposit) {
    if (!period.includes(deposit.getDays())) {
      return 0;
    }
    return rate.monthlyInterestOn(deposit.getBalance());
  }
}
