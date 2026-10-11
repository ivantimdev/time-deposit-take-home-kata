package org.ikigaidigital.adapter.in.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;

final class TimeDepositTables {

  private final JdbcTemplate jdbc;

  TimeDepositTables(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  void insertDeposit(int id, String planType, int days, String balance) {
    jdbc.update(
        "insert into time_deposits (id, plan_type, days, balance) values (?, ?, ?, ?)",
        id,
        planType,
        days,
        new BigDecimal(balance));
  }

  void insertWithdrawal(int id, int timeDepositId, String amount, LocalDate date) {
    jdbc.update(
        "insert into withdrawals (id, time_deposit_id, amount, date) values (?, ?, ?, ?)",
        id,
        timeDepositId,
        new BigDecimal(amount),
        date);
  }

  // Integration tests share one database, and some of them expect both tables to be empty.
  void deleteAll() {
    jdbc.update("delete from withdrawals");
    jdbc.update("delete from time_deposits");
  }
}
