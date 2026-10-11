package org.ikigaidigital;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.ikigaidigital.PostgresContainerSupport.POSTGRES;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import org.ikigaidigital.application.port.in.ListTimeDepositsUseCase;
import org.ikigaidigital.domain.TimeDepositDetails;
import org.ikigaidigital.domain.Withdrawal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("local")
class SampleDataIT {

  // The seed is a repeatable migration that Flyway records in its history. Applied to the database
  // the other integration tests share, every later context without the local profile would fail
  // validation on an applied migration it cannot resolve. So the sample data gets its own database
  // on the shared container, and nothing here reaches the other tests.
  private static final String SAMPLE_DATABASE = "sample_data";

  static {
    try (Connection connection =
            DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Statement statement = connection.createStatement()) {
      statement.execute("create database " + SAMPLE_DATABASE);
    } catch (SQLException exception) {
      throw new IllegalStateException(exception);
    }
  }

  @DynamicPropertySource
  static void sampleDatabase(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.datasource.url",
        () ->
            "jdbc:postgresql://"
                + POSTGRES.getHost()
                + ":"
                + POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)
                + "/"
                + SAMPLE_DATABASE);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  private final ListTimeDepositsUseCase listTimeDeposits;
  private final JdbcTemplate jdbc;

  SampleDataIT(ListTimeDepositsUseCase listTimeDeposits, JdbcTemplate jdbc) {
    this.listTimeDeposits = listTimeDeposits;
    this.jdbc = jdbc;
  }

  @AfterEach
  void keepOnlyTheSampleData() {
    jdbc.update("delete from withdrawals where id > 3");
    jdbc.update("delete from time_deposits where id > 4");
  }

  @Test
  void loadsTheFourSampleDeposits() {
    assertThat(listTimeDeposits.listTimeDeposits())
        .extracting(TimeDepositDetails::deposit)
        .extracting(
            TimeDeposit::getId,
            TimeDeposit::getPlanType,
            TimeDeposit::getDays,
            TimeDeposit::getBalance)
        .containsExactly(
            tuple(1, "basic", 45, 10000.0),
            tuple(2, "student", 100, 10000.0),
            tuple(3, "premium", 60, 10000.0),
            tuple(4, "premium", 20, 10000.0));
  }

  @Test
  void loadsTheThreeSampleWithdrawals() {
    assertThat(listTimeDeposits.listTimeDeposits())
        .flatExtracting(TimeDepositDetails::withdrawals)
        .containsExactly(
            new Withdrawal(1, 1, new BigDecimal("500.00"), LocalDate.of(2026, 9, 1)),
            new Withdrawal(2, 1, new BigDecimal("250.00"), LocalDate.of(2026, 10, 1)),
            new Withdrawal(3, 3, new BigDecimal("1000.00"), LocalDate.of(2026, 9, 15)));
  }

  // The seed moves both identity sequences past its explicit ids.
  @Test
  void acceptsAWithdrawalWithoutAnIdAfterTheSampleData() {
    assertThat(
            jdbc.queryForObject(
                "insert into withdrawals (time_deposit_id, amount, date)"
                    + " values (2, 100.00, '2026-10-11') returning id",
                Integer.class))
        .isEqualTo(4);
  }

  @Test
  void acceptsATimeDepositWithoutAnIdAfterTheSampleData() {
    assertThat(
            jdbc.queryForObject(
                "insert into time_deposits (plan_type, days, balance)"
                    + " values ('basic', 1, 1.00) returning id",
                Integer.class))
        .isEqualTo(5);
  }
}
