package org.ikigaidigital.application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.port.out.TimeDepositRepository;
import org.ikigaidigital.domain.TimeDepositDetails;
import org.ikigaidigital.domain.Withdrawal;

public class InMemoryTimeDepositRepository implements TimeDepositRepository {

  public static final String LOAD_ALL_FOR_UPDATE = "loadAllForUpdate";
  public static final String SAVE_ALL = "saveAll";
  public static final String LOAD_ALL_WITH_WITHDRAWALS = "loadAllWithWithdrawals";

  private final Map<Integer, TimeDeposit> deposits = new TreeMap<>();
  private final Map<Integer, List<Withdrawal>> withdrawals = new TreeMap<>();
  private final List<String> calls = new ArrayList<>();
  private final List<TimeDeposit> loadedForUpdate = new ArrayList<>();
  private final List<TimeDeposit> saved = new ArrayList<>();

  public InMemoryTimeDepositRepository add(
      TimeDeposit deposit, Withdrawal... withdrawalsOfDeposit) {
    deposits.put(deposit.getId(), copyOf(deposit));
    withdrawals.put(deposit.getId(), Arrays.asList(withdrawalsOfDeposit));
    return this;
  }

  @Override
  public List<TimeDeposit> loadAllForUpdate() {
    calls.add(LOAD_ALL_FOR_UPDATE);
    List<TimeDeposit> loaded = deposits.values().stream().map(this::copyOf).toList();
    loadedForUpdate.addAll(loaded);
    return loaded;
  }

  @Override
  public void saveAll(List<TimeDeposit> depositsToSave) {
    calls.add(SAVE_ALL);
    saved.addAll(depositsToSave);
    depositsToSave.forEach(deposit -> deposits.put(deposit.getId(), copyOf(deposit)));
  }

  @Override
  public List<TimeDepositDetails> loadAllWithWithdrawals() {
    calls.add(LOAD_ALL_WITH_WITHDRAWALS);
    return deposits.values().stream()
        .map(deposit -> new TimeDepositDetails(copyOf(deposit), withdrawals.get(deposit.getId())))
        .toList();
  }

  public List<String> calls() {
    return List.copyOf(calls);
  }

  public List<TimeDeposit> loadedForUpdate() {
    return List.copyOf(loadedForUpdate);
  }

  public List<TimeDeposit> saved() {
    return List.copyOf(saved);
  }

  private TimeDeposit copyOf(TimeDeposit deposit) {
    return new TimeDeposit(
        deposit.getId(), deposit.getPlanType(), deposit.getBalance(), deposit.getDays());
  }
}
