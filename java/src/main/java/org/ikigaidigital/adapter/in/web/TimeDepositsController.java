package org.ikigaidigital.adapter.in.web;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ikigaidigital.adapter.in.web.api.TimeDepositsApi;
import org.ikigaidigital.adapter.in.web.model.TimeDeposit;
import org.ikigaidigital.application.port.in.ListTimeDepositsUseCase;
import org.ikigaidigital.application.port.in.UpdateBalancesUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

// ASSUMPTION A1: the two operations of the contract are the only endpoints of the service.
@RestController
@RequiredArgsConstructor
class TimeDepositsController implements TimeDepositsApi {

  private final ListTimeDepositsUseCase listTimeDepositsUseCase;
  private final UpdateBalancesUseCase updateBalancesUseCase;
  private final TimeDepositResponseMapper responseMapper;

  @Override
  public ResponseEntity<List<TimeDeposit>> listTimeDeposits() {
    return ResponseEntity.ok(responseMapper.toResponse(listTimeDepositsUseCase.listTimeDeposits()));
  }

  // ASSUMPTION A8: answers with the updated deposits so the caller sees the effect at once.
  @Override
  public ResponseEntity<List<TimeDeposit>> updateBalances() {
    return ResponseEntity.ok(responseMapper.toResponse(updateBalancesUseCase.updateBalances()));
  }
}
