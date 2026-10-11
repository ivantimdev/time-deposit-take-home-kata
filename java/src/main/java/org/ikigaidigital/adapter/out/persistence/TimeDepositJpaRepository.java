package org.ikigaidigital.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface TimeDepositJpaRepository extends JpaRepository<TimeDepositEntity, Integer> {

  // ASSUMPTION A10: a row lock (PostgreSQL emits FOR NO KEY UPDATE) held until the caller's
  // transaction ends; a concurrent update waits for it.
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from TimeDepositEntity t order by t.id")
  List<TimeDepositEntity> findAllForUpdate();

  List<TimeDepositEntity> findAllByOrderByIdAsc();
}
