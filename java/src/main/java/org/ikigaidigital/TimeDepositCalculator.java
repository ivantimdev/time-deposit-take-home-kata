package org.ikigaidigital;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.ikigaidigital.domain.plan.PlanCatalog;

public class TimeDepositCalculator {

  private final PlanCatalog plans;

  public TimeDepositCalculator() {
    this(PlanCatalog.standard());
  }

  public TimeDepositCalculator(PlanCatalog plans) {
    this.plans = plans;
  }

  public void updateBalance(List<TimeDeposit> deposits) {
    for (TimeDeposit deposit : deposits) {
      double interest =
          plans
              .planFor(deposit.getPlanType())
              .map(plan -> plan.interest().monthlyInterest(deposit))
              .orElse(0.0);
      deposit.setBalance(deposit.getBalance() + roundedToCents(interest));
    }
  }

  // new BigDecimal(double) rounds the exact binary value; BigDecimal.valueOf would round its
  // shortest decimal form and credit a different cent on some balances (characterization tests).
  private static double roundedToCents(double interest) {
    return new BigDecimal(interest).setScale(2, RoundingMode.HALF_UP).doubleValue();
  }
}
