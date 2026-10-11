package org.ikigaidigital.domain.plan;

public record EarningPeriod(int firstDay, int lastDay) {

  public static EarningPeriod from(int day) {
    return new EarningPeriod(day, Integer.MAX_VALUE);
  }

  public static EarningPeriod after(int day) {
    return new EarningPeriod(day + 1, Integer.MAX_VALUE);
  }

  public static EarningPeriod between(int firstDay, int lastDay) {
    return new EarningPeriod(firstDay, lastDay);
  }

  public boolean includes(int days) {
    return firstDay <= days && days <= lastDay;
  }
}
