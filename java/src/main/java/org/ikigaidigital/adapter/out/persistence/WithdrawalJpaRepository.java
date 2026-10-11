package org.ikigaidigital.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface WithdrawalJpaRepository extends JpaRepository<WithdrawalEntity, Integer> {

  List<WithdrawalEntity> findAllByOrderByTimeDepositIdAscIdAsc();
}
