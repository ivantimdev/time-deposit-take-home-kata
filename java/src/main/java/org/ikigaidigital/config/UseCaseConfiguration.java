package org.ikigaidigital.config;

import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.application.port.in.ListTimeDepositsUseCase;
import org.ikigaidigital.application.port.in.UpdateBalancesUseCase;
import org.ikigaidigital.application.port.out.TimeDepositRepository;
import org.ikigaidigital.application.service.ListTimeDepositsService;
import org.ikigaidigital.application.service.UpdateBalancesService;
import org.ikigaidigital.domain.plan.PlanCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class UseCaseConfiguration {

  @Bean
  TimeDepositCalculator timeDepositCalculator() {
    return new TimeDepositCalculator(PlanCatalog.standard());
  }

  @Bean
  UpdateBalancesUseCase updateBalancesUseCase(
      TimeDepositRepository repository, TimeDepositCalculator calculator) {
    return new UpdateBalancesService(repository, calculator);
  }

  @Bean
  ListTimeDepositsUseCase listTimeDepositsUseCase(TimeDepositRepository repository) {
    return new ListTimeDepositsService(repository);
  }
}
