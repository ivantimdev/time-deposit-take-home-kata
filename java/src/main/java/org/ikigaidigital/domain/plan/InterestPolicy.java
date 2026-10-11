package org.ikigaidigital.domain.plan;

import org.ikigaidigital.TimeDeposit;

public interface InterestPolicy {

  double monthlyInterest(TimeDeposit deposit);
}
