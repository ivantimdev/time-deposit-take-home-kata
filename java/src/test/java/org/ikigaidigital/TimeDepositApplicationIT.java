package org.ikigaidigital;

import static org.assertj.core.api.Assertions.assertThat;

import org.ikigaidigital.application.port.in.ListTimeDepositsUseCase;
import org.ikigaidigital.application.port.in.UpdateBalancesUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class TimeDepositApplicationIT extends PostgresContainerSupport {

  private final ApplicationContext context;
  private final Environment environment;
  private final JdbcTemplate jdbc;

  TimeDepositApplicationIT(ApplicationContext context, Environment environment, JdbcTemplate jdbc) {
    this.context = context;
    this.environment = environment;
    this.jdbc = jdbc;
  }

  @Test
  void providesTheUpdateBalancesUseCase() {
    assertThat(context.getBeansOfType(UpdateBalancesUseCase.class)).hasSize(1);
  }

  @Test
  void providesTheListTimeDepositsUseCase() {
    assertThat(context.getBeansOfType(ListTimeDepositsUseCase.class)).hasSize(1);
  }

  @Test
  void runsWithoutTheLocalProfile() {
    assertThat(environment.getActiveProfiles()).doesNotContain("local");
  }

  @Test
  void startsWithNoTimeDeposits() {
    assertThat(jdbc.queryForObject("select count(*) from time_deposits", Integer.class)).isZero();
  }

  @Test
  void startsWithNoWithdrawals() {
    assertThat(jdbc.queryForObject("select count(*) from withdrawals", Integer.class)).isZero();
  }

  @Test
  void listsNoTimeDepositsThroughTheUseCase() {
    assertThat(context.getBean(ListTimeDepositsUseCase.class).listTimeDeposits()).isEmpty();
  }
}
