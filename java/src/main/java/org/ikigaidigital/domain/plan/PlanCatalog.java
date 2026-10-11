package org.ikigaidigital.domain.plan;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class PlanCatalog {

  private final Map<PlanType, Plan> plans = new EnumMap<>(PlanType.class);

  private PlanCatalog(Plan... plans) {
    for (Plan plan : plans) {
      this.plans.put(plan.type(), plan);
    }
  }

  public static PlanCatalog of(Plan... plans) {
    return new PlanCatalog(plans);
  }

  // ASSUMPTION A3: student interest runs from day 31 to day 365 inclusive, none from day 366.
  // ASSUMPTION A4: premium interest starts on day 46.
  // ASSUMPTION A5: the thirty-day grace period is each plan's earning period, not a global guard.
  public static PlanCatalog standard() {
    return of(
        new Plan(
            PlanType.BASIC, new FixedRateInterest(new AnnualRate(0.01), EarningPeriod.after(30))),
        new Plan(
            PlanType.STUDENT,
            new FixedRateInterest(new AnnualRate(0.03), EarningPeriod.between(31, 365))),
        new Plan(
            PlanType.PREMIUM,
            new FixedRateInterest(new AnnualRate(0.05), EarningPeriod.after(45))));
  }

  public Optional<Plan> planFor(String planTypeCode) {
    return PlanType.fromCode(planTypeCode).map(plans::get);
  }
}
