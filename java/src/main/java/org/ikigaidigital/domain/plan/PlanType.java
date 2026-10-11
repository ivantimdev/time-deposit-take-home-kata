package org.ikigaidigital.domain.plan;

import java.util.Arrays;
import java.util.Optional;

public enum PlanType {
  BASIC("basic"),
  STUDENT("student"),
  PREMIUM("premium");

  private final String code;

  PlanType(String code) {
    this.code = code;
  }

  public String code() {
    return code;
  }

  // ASSUMPTION A6: codes match exactly and case-sensitively; any other code has no plan.
  public static Optional<PlanType> fromCode(String code) {
    return Arrays.stream(values()).filter(type -> type.code().equals(code)).findFirst();
  }
}
