package org.ikigaidigital.domain.plan;

public record AnnualRate(double rate) {

  // ASSUMPTION A7: one call is one month of interest at a twelfth of the annual rate.
  // The operand order is kept from the original because double arithmetic is not associative.
  public double monthlyInterestOn(double balance) {
    return balance * rate / 12;
  }
}
