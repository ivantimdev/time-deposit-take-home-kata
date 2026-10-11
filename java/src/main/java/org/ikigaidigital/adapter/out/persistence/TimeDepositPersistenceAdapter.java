package org.ikigaidigital.adapter.out.persistence;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.port.out.TimeDepositRepository;
import org.ikigaidigital.domain.TimeDepositDetails;
import org.ikigaidigital.domain.Withdrawal;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class TimeDepositPersistenceAdapter implements TimeDepositRepository {

  private final TimeDepositJpaRepository timeDeposits;
  private final WithdrawalJpaRepository withdrawals;

  @Override
  public List<TimeDeposit> loadAllForUpdate() {
    return timeDeposits.findAllForUpdate().stream().map(TimeDepositMapper::toDomain).toList();
  }

  @Override
  public void saveAll(List<TimeDeposit> deposits) {
    Map<Integer, TimeDepositEntity> entitiesById =
        timeDeposits.findAllById(deposits.stream().map(TimeDeposit::getId).toList()).stream()
            .collect(Collectors.toMap(TimeDepositEntity::getId, Function.identity()));
    for (TimeDeposit deposit : deposits) {
      TimeDepositEntity entity = entitiesById.get(deposit.getId());
      if (entity == null) {
        throw new IllegalStateException("no stored time deposit with id " + deposit.getId());
      }
      entity.setBalance(TimeDepositMapper.toBalanceColumn(deposit.getBalance()));
    }
    timeDeposits.saveAll(entitiesById.values());
  }

  // Two queries grouped in memory: no query per deposit and no JPA navigation between entities.
  @Override
  public List<TimeDepositDetails> loadAllWithWithdrawals() {
    Map<Integer, List<Withdrawal>> withdrawalsByDeposit =
        withdrawals.findAllByOrderByTimeDepositIdAscIdAsc().stream()
            .map(TimeDepositMapper::toDomain)
            .collect(Collectors.groupingBy(Withdrawal::timeDepositId));
    return timeDeposits.findAllByOrderByIdAsc().stream()
        .map(
            entity ->
                new TimeDepositDetails(
                    TimeDepositMapper.toDomain(entity),
                    withdrawalsByDeposit.getOrDefault(entity.getId(), List.of())))
        .toList();
  }
}
