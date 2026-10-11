package org.ikigaidigital.adapter.out.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.ikigaidigital.domain.plan.PlanType;

@Converter(autoApply = false)
class PlanTypeConverter implements AttributeConverter<PlanType, String> {

  @Override
  public String convertToDatabaseColumn(PlanType planType) {
    return planType.code();
  }

  // ASSUMPTION A12: the CHECK constraint admits only known codes, so any other one is a defect.
  @Override
  public PlanType convertToEntityAttribute(String code) {
    return PlanType.fromCode(code).orElseThrow();
  }
}
