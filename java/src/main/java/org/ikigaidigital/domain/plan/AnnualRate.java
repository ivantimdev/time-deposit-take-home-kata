package org.ikigaidigital.domain.plan;

public record AnnualRate(double rate) {

  // ASSUMPTION A7: days gates, it does not scale; one call is one month, a twelfth of the rate.
  // The operand order is kept from the original because double arithmetic is not associative.
  public double monthlyInterestOn(double balance) {
    return balance * rate / 12;
  }
}
