package org.ikigaidigital.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.ikigaidigital.domain.plan.PlanType;

// ASSUMPTION A11: the logical name timeDeposits is stored as the table time_deposits.
@Entity
@Table(name = "time_deposits")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class TimeDepositEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "plan_type", nullable = false, length = 32)
  @Convert(converter = PlanTypeConverter.class)
  private PlanType planType;

  @Column(name = "days", nullable = false)
  private int days;

  // The only value a balance update changes.
  @Setter
  @Column(name = "balance", nullable = false, precision = 19, scale = 2)
  private BigDecimal balance;
}
