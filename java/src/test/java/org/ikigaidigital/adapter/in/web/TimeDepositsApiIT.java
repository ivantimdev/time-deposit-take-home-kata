package org.ikigaidigital.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.ikigaidigital.PostgresContainerSupport;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TimeDepositsApiIT extends PostgresContainerSupport {

  private static final String TIME_DEPOSITS = "/api/v1/time-deposits";
  private static final String UPDATE_BALANCES = "/api/v1/time-deposits/update-balances";
  private static final Set<String> TIME_DEPOSIT_FIELDS =
      Set.of("id", "planType", "balance", "days", "withdrawals");
  private static final Set<String> WITHDRAWAL_FIELDS = Set.of("id", "amount", "date");

  // Keeps every decimal exactly as sent, trailing zeros included, so the scale can be asserted.
  private static final JsonMapper JSON =
      JsonMapper.builder()
          .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
          .disable(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES)
          .build();

  private final TestRestTemplate http;
  private final TimeDepositTables tables;

  TimeDepositsApiIT(TestRestTemplate http, JdbcTemplate jdbc) {
    this.http = http;
    this.tables = new TimeDepositTables(jdbc);
  }

  @BeforeEach
  @AfterEach
  void emptyTheTables() {
    tables.deleteAll();
  }

  @Test
  void listsNoTimeDepositsWhenNoneAreStored() {
    ResponseEntity<String> response = http.getForEntity(TIME_DEPOSITS, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    assertThat(json(response.getBody())).isEmpty();
  }

  @Test
  void listsEveryFieldOfEveryTimeDepositOrderedById() {
    insertOneDepositPerPlan();

    JsonNode deposits = json(http.getForEntity(TIME_DEPOSITS, String.class).getBody());

    assertThat(deposits).hasSize(3);
    assertDeposit(deposits.get(0), 1, "basic", "10000.00", 45);
    assertDeposit(deposits.get(1), 2, "student", "5000.00", 100);
    assertDeposit(deposits.get(2), 3, "premium", "20000.00", 60);
  }

  @Test
  void listsTheWithdrawalsOfEachTimeDepositOrderedById() {
    insertOneDepositPerPlan();

    JsonNode deposits = json(http.getForEntity(TIME_DEPOSITS, String.class).getBody());

    assertWithdrawal(deposits.get(0).get("withdrawals").get(0), 11, "250.00", "2026-08-01");
    assertWithdrawal(deposits.get(0).get("withdrawals").get(1), 12, "500.00", "2026-07-15");
    assertThat(deposits.get(0).get("withdrawals")).hasSize(2);
    assertThat(deposits.get(1).get("withdrawals")).isEmpty();
    assertWithdrawal(deposits.get(2).get("withdrawals").get(0), 13, "1000.00", "2026-09-15");
    assertThat(deposits.get(2).get("withdrawals")).hasSize(1);
  }

  @Test
  void listsTimeDepositsWithNoPropertiesBeyondTheContract() {
    insertOneDepositPerPlan();

    JsonNode deposits = json(http.getForEntity(TIME_DEPOSITS, String.class).getBody());

    assertThat(deposits)
        .allSatisfy(deposit -> assertThat(fieldNames(deposit)).isEqualTo(TIME_DEPOSIT_FIELDS));
    assertThat(deposits.findParents("amount"))
        .hasSize(3)
        .allSatisfy(withdrawal -> assertThat(fieldNames(withdrawal)).isEqualTo(WITHDRAWAL_FIELDS));
  }

  @Test
  void updatesBalancesAsTheCalculatorDoes() {
    insertOneDepositPerPlan();

    ResponseEntity<String> response = http.postForEntity(UPDATE_BALANCES, null, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    assertThat(balances(json(response.getBody()))).isEqualTo(oracleBalances(1));
  }

  @Test
  void creditsOneMonthOfBasicInterestToTheBasicDeposit() {
    insertOneDepositPerPlan();

    JsonNode deposits = json(http.postForEntity(UPDATE_BALANCES, null, String.class).getBody());

    assertThat(deposits.get(0).get("balance").decimalValue()).isEqualTo(new BigDecimal("10008.33"));
  }

  @Test
  void updatesBalancesWithoutChangingDaysOrWithdrawals() {
    insertOneDepositPerPlan();
    JsonNode before = json(http.getForEntity(TIME_DEPOSITS, String.class).getBody());

    JsonNode after = json(http.postForEntity(UPDATE_BALANCES, null, String.class).getBody());

    assertThat(after.findValues("days")).isEqualTo(before.findValues("days"));
    assertThat(after.findValues("withdrawals")).isEqualTo(before.findValues("withdrawals"));
  }

  @Test
  void listsTheTimeDepositsExactlyAsTheUpdateReturnedThem() {
    insertOneDepositPerPlan();

    JsonNode updated = json(http.postForEntity(UPDATE_BALANCES, null, String.class).getBody());

    assertThat(json(http.getForEntity(TIME_DEPOSITS, String.class).getBody())).isEqualTo(updated);
  }

  @Test
  void compoundsInterestOverThreeUpdates() {
    insertOneDepositPerPlan();

    http.postForEntity(UPDATE_BALANCES, null, String.class);
    http.postForEntity(UPDATE_BALANCES, null, String.class);
    JsonNode deposits = json(http.postForEntity(UPDATE_BALANCES, null, String.class).getBody());

    assertThat(balances(deposits)).isEqualTo(oracleBalances(3));
  }

  // Literals from the original calculator (commit 3efc9b5), applied three times.
  @Test
  void reachesTheOriginalBalancesAfterThreeUpdates() {
    insertOneDepositPerPlan();

    http.postForEntity(UPDATE_BALANCES, null, String.class);
    http.postForEntity(UPDATE_BALANCES, null, String.class);
    JsonNode deposits = json(http.postForEntity(UPDATE_BALANCES, null, String.class).getBody());

    assertThat(balances(deposits))
        .containsExactly(
            new BigDecimal("10025.02"), new BigDecimal("5037.59"), new BigDecimal("20251.04"));
  }

  @Test
  void leavesPremiumAtThirtyDaysAndStudentAtFourHundredDaysUnchanged() {
    tables.insertDeposit(1, "premium", 30, "10000.00");
    tables.insertDeposit(2, "student", 400, "10000.00");

    JsonNode deposits = json(http.postForEntity(UPDATE_BALANCES, null, String.class).getBody());

    assertThat(balances(deposits))
        .containsExactly(new BigDecimal("10000.00"), new BigDecimal("10000.00"));
  }

  @Test
  void updatesNoTimeDepositsWhenNoneAreStored() {
    ResponseEntity<String> response = http.postForEntity(UPDATE_BALANCES, null, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(json(response.getBody())).isEmpty();
  }

  // ASSUMPTION A10: concurrent updates are serialised, so none is lost or applied twice.
  @Test
  void appliesEveryConcurrentUpdateExactlyOnce() throws Exception {
    final int updates = 5;
    insertOneDepositPerPlan();

    List<HttpStatusCode> statuses = updateBalancesConcurrently(updates);

    assertThat(statuses).hasSize(updates).containsOnly(HttpStatus.OK);
    assertThat(balances(json(http.getForEntity(TIME_DEPOSITS, String.class).getBody())))
        .isEqualTo(oracleBalances(updates));
  }

  // Literals from the original calculator (commit 3efc9b5), applied five times.
  @Test
  void reachesTheOriginalBalancesAfterFiveConcurrentUpdates() throws Exception {
    insertOneDepositPerPlan();

    updateBalancesConcurrently(5);

    assertThat(balances(json(http.getForEntity(TIME_DEPOSITS, String.class).getBody())))
        .containsExactly(
            new BigDecimal("10041.73"), new BigDecimal("5062.81"), new BigDecimal("20420.15"));
  }

  @Test
  void servesTheContractUnchanged() throws IOException {
    ResponseEntity<byte[]> response = http.getForEntity("/openapi.yaml", byte[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody())
        .isEqualTo(new ClassPathResource("openapi/openapi.yaml").getContentAsByteArray());
  }

  @Test
  void servesTheContractAsYaml() {
    assertThat(http.getForEntity("/openapi.yaml", byte[].class).getHeaders().getContentType())
        .isEqualTo(MediaType.parseMediaType("application/yaml"));
  }

  @Test
  void servesSwaggerUiPointedAtTheContract() {
    ResponseEntity<String> response = http.getForEntity("/swagger-ui/index.html", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("url: \"/openapi.yaml\"");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/swagger-ui/swagger-ui.css",
        "/swagger-ui/swagger-ui-bundle.js",
        "/swagger-ui/swagger-ui-standalone-preset.js"
      })
  void servesTheFilesSwaggerUiLoads(String path) {
    assertThat(http.getForEntity(path, String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  // The bundled scripts open with "For license information please see <script>.LICENSE.txt".
  @ParameterizedTest
  @ValueSource(
      strings = {
        "/swagger-ui/swagger-ui-bundle.js.LICENSE.txt",
        "/swagger-ui/swagger-ui-standalone-preset.js.LICENSE.txt"
      })
  void servesTheLicenceNoticesOfTheBundledScripts(String path) {
    assertThat(http.getForEntity(path, String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  // ASSUMPTION A1: nothing but the two operations and their documentation answers.
  @ParameterizedTest
  @ValueSource(
      strings = {
        "/api/v1/time-deposits/1",
        "/actuator/health",
        "/v3/api-docs",
        "/webjars/swagger-ui/index.html",
        "/webjars/swagger-ui/swagger-initializer.js",
        "/swagger-ui/oauth2-redirect.html"
      })
  void answersNotFoundBeyondTheTwoEndpoints(String path) {
    assertThat(http.getForEntity(path, String.class).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  // ASSUMPTION A1: /error is Spring Boot's error handling, not an endpoint; called directly it
  // has no error to render and answers 500.
  @Test
  void answersTheFrameworkErrorPageWithServerError() {
    assertThat(http.getForEntity("/error", String.class).getStatusCode())
        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private List<HttpStatusCode> updateBalancesConcurrently(int updates) throws Exception {
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(updates);
    try {
      List<Future<ResponseEntity<String>>> responses =
          IntStream.range(0, updates)
              .mapToObj(
                  update ->
                      executor.submit(
                          () -> {
                            start.await();
                            return http.postForEntity(UPDATE_BALANCES, null, String.class);
                          }))
              .toList();
      start.countDown();
      List<HttpStatusCode> statuses = new ArrayList<>();
      for (Future<ResponseEntity<String>> response : responses) {
        statuses.add(response.get(30, TimeUnit.SECONDS).getStatusCode());
      }
      return statuses;
    } finally {
      executor.shutdownNow();
    }
  }

  private void insertOneDepositPerPlan() {
    tables.insertDeposit(3, "premium", 60, "20000.00");
    tables.insertDeposit(1, "basic", 45, "10000.00");
    tables.insertDeposit(2, "student", 100, "5000.00");
    tables.insertWithdrawal(12, 1, "500.00", LocalDate.of(2026, 7, 15));
    tables.insertWithdrawal(11, 1, "250.00", LocalDate.of(2026, 8, 1));
    tables.insertWithdrawal(13, 3, "1000.00", LocalDate.of(2026, 9, 15));
  }

  // The calculator, pinned by the frozen characterization tests, is the oracle for one update;
  // the sibling tests with literal balances guard against a drift of both.
  private static List<BigDecimal> oracleBalances(int updates) {
    List<TimeDeposit> deposits =
        List.of(
            new TimeDeposit(1, "basic", 10000.00, 45),
            new TimeDeposit(2, "student", 5000.00, 100),
            new TimeDeposit(3, "premium", 20000.00, 60));
    TimeDepositCalculator calculator = new TimeDepositCalculator();
    IntStream.range(0, updates).forEach(update -> calculator.updateBalance(deposits));
    return deposits.stream()
        .map(deposit -> BigDecimal.valueOf(deposit.getBalance()).setScale(2, RoundingMode.HALF_UP))
        .toList();
  }

  private static void assertDeposit(
      JsonNode deposit, int id, String planType, String balance, int days) {
    assertThat(deposit.get("id").intValue()).isEqualTo(id);
    assertThat(deposit.get("planType").textValue()).isEqualTo(planType);
    assertThat(deposit.get("balance").decimalValue()).isEqualTo(new BigDecimal(balance));
    assertThat(deposit.get("days").intValue()).isEqualTo(days);
  }

  private static void assertWithdrawal(JsonNode withdrawal, int id, String amount, String date) {
    assertThat(withdrawal.get("id").intValue()).isEqualTo(id);
    assertThat(withdrawal.get("amount").decimalValue()).isEqualTo(new BigDecimal(amount));
    assertThat(withdrawal.get("date").textValue()).isEqualTo(date);
  }

  private static List<BigDecimal> balances(JsonNode deposits) {
    return deposits.findValues("balance").stream().map(JsonNode::decimalValue).toList();
  }

  private static Set<String> fieldNames(JsonNode node) {
    Set<String> names = new HashSet<>();
    node.fieldNames().forEachRemaining(names::add);
    return names;
  }

  private static JsonNode json(String body) {
    try {
      return JSON.readTree(body);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
