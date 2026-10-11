package org.ikigaidigital.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface TimeDepositJpaRepository extends JpaRepository<TimeDepositEntity, Integer> {

  // ASSUMPTION A10: SELECT ... FOR UPDATE holds the rows until the caller's transaction ends.
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from TimeDepositEntity t order by t.id")
  List<TimeDepositEntity> findAllForUpdate();

  List<TimeDepositEntity> findAllByOrderByIdAsc();
}
