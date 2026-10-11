package org.ikigaidigital.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.ikigaidigital.PostgresContainerSupport;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.domain.TimeDepositDetails;
import org.ikigaidigital.domain.Withdrawal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TimeDepositPersistenceAdapter.class)
class TimeDepositPersistenceAdapterIT extends PostgresContainerSupport {

  private final TimeDepositPersistenceAdapter adapter;
  private final JdbcTemplate jdbc;
  private final TestEntityManager entityManager;
  private final Environment environment;

  TimeDepositPersistenceAdapterIT(
      TimeDepositPersistenceAdapter adapter,
      JdbcTemplate jdbc,
      TestEntityManager entityManager,
      Environment environment) {
    this.adapter = adapter;
    this.jdbc = jdbc;
    this.entityManager = entityManager;
    this.environment = environment;
  }

  @Test
  void runsWithSchemaValidationEnabled() {
    assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
  }

  @Test
  void appliesTheSchemaMigration() {
    assertThat(
            jdbc.queryForList(
                "select version from flyway_schema_history where success", String.class))
        .contains("1");
  }

  @Test
  void recordsNoFailedMigration() {
    assertThat(
            jdbc.queryForObject(
                "select count(*) from flyway_schema_history where not success", Integer.class))
        .isZero();
  }

  @ParameterizedTest
  @ValueSource(strings = {"basic", "student", "premium"})
  void savesAndLoadsADepositOfEachPlanType(String planType) {
    insertDeposit(1, planType, 45, "100.00");

    adapter.saveAll(List.of(new TimeDeposit(1, planType, 101.25, 45)));
    flushAndClear();

    assertThat(adapter.loadAllWithWithdrawals())
        .extracting(TimeDepositDetails::deposit)
        .extracting(
            TimeDeposit::getId,
            TimeDeposit::getPlanType,
            TimeDeposit::getBalance,
            TimeDeposit::getDays)
        .containsExactly(tuple(1, planType, 101.25, 45));
  }

  @ParameterizedTest
  @CsvSource({"0.10, 0.1", "123456.78, 123456.78", "999999999.99, 999999999.99"})
  void storesTheBalanceToTheCent(String storedBalance, double balance) {
    insertDeposit(1, "basic", 45, "0.00");

    adapter.saveAll(List.of(new TimeDeposit(1, "basic", balance, 45)));
    flushAndClear();

    assertThat(jdbc.queryForObject("select balance from time_deposits", BigDecimal.class))
        .isEqualTo(new BigDecimal(storedBalance));
  }

  @ParameterizedTest
  @CsvSource({"0.10, 0.1", "123456.78, 123456.78", "999999999.99, 999999999.99"})
  void loadsTheStoredBalanceUnchanged(String storedBalance, double balance) {
    insertDeposit(1, "basic", 45, storedBalance);

    assertThat(adapter.loadAllForUpdate())
        .extracting(TimeDeposit::getBalance)
        .containsExactly(balance);
  }

  @Test
  void roundsABalanceWithFloatingPointNoiseToTheCent() {
    insertDeposit(1, "basic", 45, "0.00");

    adapter.saveAll(List.of(new TimeDeposit(1, "basic", 10008.330000000002, 45)));
    flushAndClear();

    assertThat(jdbc.queryForObject("select balance from time_deposits", BigDecimal.class))
        .isEqualTo(new BigDecimal("10008.33"));
  }

  @Test
  void listsDepositsOrderedById() {
    insertDeposit(3, "premium", 60, "300.00");
    insertDeposit(1, "basic", 45, "100.00");
    insertDeposit(2, "student", 100, "200.00");

    assertThat(adapter.loadAllWithWithdrawals())
        .extracting(details -> details.deposit().getId())
        .containsExactly(1, 2, 3);
  }

  @Test
  void loadsDepositsForUpdateOrderedById() {
    insertDeposit(3, "premium", 60, "300.00");
    insertDeposit(1, "basic", 45, "100.00");
    insertDeposit(2, "student", 100, "200.00");

    assertThat(adapter.loadAllForUpdate()).extracting(TimeDeposit::getId).containsExactly(1, 2, 3);
  }

  @Test
  void loadsDepositsForUpdateWithTheirStoredValues() {
    insertDeposit(1, "student", 100, "200.50");

    assertThat(adapter.loadAllForUpdate())
        .extracting(
            TimeDeposit::getId,
            TimeDeposit::getPlanType,
            TimeDeposit::getBalance,
            TimeDeposit::getDays)
        .containsExactly(tuple(1, "student", 200.5, 100));
  }

  @Test
  void groupsWithdrawalsUnderTheirDepositOrderedById() {
    insertDeposit(1, "basic", 45, "100.00");
    insertDeposit(2, "premium", 60, "200.00");
    insertWithdrawal(12, 2, "30.00", "2026-09-03");
    insertWithdrawal(11, 1, "20.00", "2026-09-02");
    insertWithdrawal(10, 1, "10.00", "2026-09-01");

    assertThat(adapter.loadAllWithWithdrawals())
        .extracting(TimeDepositDetails::withdrawals)
        .containsExactly(
            List.of(
                new Withdrawal(10, 1, new BigDecimal("10.00"), LocalDate.of(2026, 9, 1)),
                new Withdrawal(11, 1, new BigDecimal("20.00"), LocalDate.of(2026, 9, 2))),
            List.of(new Withdrawal(12, 2, new BigDecimal("30.00"), LocalDate.of(2026, 9, 3))));
  }

  @Test
  void listsADepositWithoutWithdrawalsWithAnEmptyList() {
    insertDeposit(1, "basic", 45, "100.00");

    assertThat(adapter.loadAllWithWithdrawals())
        .singleElement()
        .extracting(TimeDepositDetails::withdrawals, InstanceOfAssertFactories.LIST)
        .isEmpty();
  }

  @Test
  void listsNothingWhenNoDepositIsStored() {
    assertThat(adapter.loadAllWithWithdrawals()).isEmpty();
  }

  @Test
  void savesOnlyTheBalance() {
    insertDeposit(1, "basic", 45, "100.00");

    adapter.saveAll(List.of(new TimeDeposit(1, "premium", 200.0, 99)));
    flushAndClear();

    assertThat(jdbc.queryForMap("select plan_type, days, balance from time_deposits where id = 1"))
        .containsEntry("plan_type", "basic")
        .containsEntry("days", 45)
        .containsEntry("balance", new BigDecimal("200.00"));
  }

  @Test
  void leavesDepositsMissingFromTheSavedListUnchanged() {
    insertDeposit(1, "basic", 45, "100.00");
    insertDeposit(2, "basic", 45, "100.00");

    adapter.saveAll(List.of(new TimeDeposit(2, "basic", 150.0, 45)));
    flushAndClear();

    assertThat(jdbc.queryForList("select balance from time_deposits order by id", BigDecimal.class))
        .containsExactly(new BigDecimal("100.00"), new BigDecimal("150.00"));
  }

  @Test
  void refusesToSaveADepositThatIsNotStored() {
    assertThatThrownBy(() -> adapter.saveAll(List.of(new TimeDeposit(7, "basic", 1.0, 45))))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("no stored time deposit with id 7");
  }

  @Test
  void rejectsANegativeBalance() {
    assertThatThrownBy(() -> insertDeposit(1, "basic", 45, "-0.01"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void acceptsAnEmptiedDeposit() {
    insertDeposit(1, "basic", 45, "0.00");

    assertThat(adapter.loadAllForUpdate()).extracting(TimeDeposit::getBalance).containsExactly(0.0);
  }

  @Test
  void rejectsNegativeDays() {
    assertThatThrownBy(() -> insertDeposit(1, "basic", -1, "100.00"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"gold", "Basic", ""})
  void rejectsAnUnknownPlanType(String planType) {
    assertThatThrownBy(() -> insertDeposit(1, planType, 45, "100.00"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"0.00", "-10.00"})
  void rejectsAWithdrawalAmountThatIsNotPositive(String amount) {
    insertDeposit(1, "basic", 45, "100.00");

    assertThatThrownBy(() -> insertWithdrawal(1, 1, amount, "2026-09-01"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsAWithdrawalOfAnUnknownDeposit() {
    assertThatThrownBy(() -> insertWithdrawal(1, 999, "10.00", "2026-09-01"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsDeletingADepositThatHasWithdrawals() {
    insertDeposit(1, "basic", 45, "100.00");
    insertWithdrawal(1, 1, "10.00", "2026-09-01");

    assertThatThrownBy(() -> jdbc.update("delete from time_deposits where id = 1"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void indexesWithdrawalsByDepositAndDate() {
    assertThat(
            jdbc.queryForObject(
                "select indexdef from pg_indexes where tablename = 'withdrawals'"
                    + " and indexname = 'withdrawals_time_deposit_id_date_idx'",
                String.class))
        .endsWith("(time_deposit_id, date)");
  }

  private void insertDeposit(int id, String planType, int days, String balance) {
    jdbc.update(
        "insert into time_deposits (id, plan_type, days, balance) values (?, ?, ?, ?)",
        id,
        planType,
        days,
        new BigDecimal(balance));
  }

  private void insertWithdrawal(int id, int timeDepositId, String amount, String date) {
    jdbc.update(
        "insert into withdrawals (id, time_deposit_id, amount, date) values (?, ?, ?, ?)",
        id,
        timeDepositId,
        new BigDecimal(amount),
        LocalDate.parse(date));
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }
}
