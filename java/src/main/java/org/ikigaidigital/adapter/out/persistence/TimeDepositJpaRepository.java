package org.ikigaidigital.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface TimeDepositJpaRepository extends JpaRepository<TimeDepositEntity, Integer> {

  // ASSUMPTION A10: PESSIMISTIC_WRITE serialises concurrent balance updates; the rows stay locked
  // until the transaction ends. Hibernate renders it as FOR NO KEY UPDATE on PostgreSQL, so
  // another writer inserting a withdrawal for a locked deposit is not blocked.
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from TimeDepositEntity t order by t.id")
  List<TimeDepositEntity> findAllForUpdate();

  List<TimeDepositEntity> findAllByOrderByIdAsc();
}
