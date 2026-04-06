package com.bigbrightpaints.erp.modules.factory.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bigbrightpaints.erp.core.util.CompanyScopedLookupService;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.factory.domain.FactoryTask;
import com.bigbrightpaints.erp.modules.factory.domain.FactoryTaskRepository;
import com.bigbrightpaints.erp.modules.factory.domain.ProductionLog;
import com.bigbrightpaints.erp.modules.factory.domain.ProductionLogRepository;
import com.bigbrightpaints.erp.modules.factory.domain.ProductionPlan;
import com.bigbrightpaints.erp.modules.factory.domain.ProductionPlanRepository;

@Service
public class CompanyScopedFactoryLookupService {

  private final CompanyScopedLookupService companyScopedLookupService;
  private final ProductionLogRepository productionLogRepository;
  private final ProductionPlanRepository productionPlanRepository;
  private final FactoryTaskRepository factoryTaskRepository;

  @Autowired
  public CompanyScopedFactoryLookupService(
      CompanyScopedLookupService companyScopedLookupService,
      ProductionLogRepository productionLogRepository,
      ProductionPlanRepository productionPlanRepository,
      FactoryTaskRepository factoryTaskRepository) {
    this.companyScopedLookupService = companyScopedLookupService;
    this.productionLogRepository = productionLogRepository;
    this.productionPlanRepository = productionPlanRepository;
    this.factoryTaskRepository = factoryTaskRepository;
  }

  public ProductionLog requireProductionLog(Company company, Long logId) {
    return companyScopedLookupService.require(
        company, logId, productionLogRepository::findByCompanyAndId, "Production log");
  }

  public ProductionLog lockProductionLog(Company company, Long logId) {
    return companyScopedLookupService.require(
        company, logId, productionLogRepository::lockByCompanyAndId, "Production log");
  }

  public ProductionPlan requireProductionPlan(Company company, Long planId) {
    return companyScopedLookupService.require(
        company, planId, productionPlanRepository::findByCompanyAndId, "Production plan");
  }

  public FactoryTask requireFactoryTask(Company company, Long taskId) {
    return companyScopedLookupService.require(
        company, taskId, factoryTaskRepository::findByCompanyAndId, "Factory task");
  }
}
