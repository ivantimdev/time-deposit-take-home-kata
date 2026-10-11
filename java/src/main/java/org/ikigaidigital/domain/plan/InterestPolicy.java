package org.ikigaidigital.domain.plan;

import org.ikigaidigital.TimeDeposit;

// The seam for interest rules beyond a fixed rate: the assignment asks for a design that can take
// on more complex interest calculations, so a plan holds a policy rather than a rate.
public interface InterestPolicy {

  double monthlyInterest(TimeDeposit deposit);
}
