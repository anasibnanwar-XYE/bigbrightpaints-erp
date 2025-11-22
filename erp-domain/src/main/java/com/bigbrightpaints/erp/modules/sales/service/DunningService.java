package com.bigbrightpaints.erp.modules.sales.service;

import com.bigbrightpaints.erp.modules.accounting.dto.AgingSummaryResponse;
import com.bigbrightpaints.erp.modules.accounting.dto.AgingBucketDto;
import com.bigbrightpaints.erp.modules.accounting.service.StatementService;
import com.bigbrightpaints.erp.modules.sales.domain.Dealer;
import com.bigbrightpaints.erp.modules.sales.domain.DealerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class DunningService {

    private static final Logger log = LoggerFactory.getLogger(DunningService.class);

    private final DealerRepository dealerRepository;
    private final StatementService statementService;

    public DunningService(DealerRepository dealerRepository,
                          StatementService statementService) {
        this.dealerRepository = dealerRepository;
        this.statementService = statementService;
    }

    public boolean evaluateDealerHold(Long dealerId, int overdueDaysThreshold, BigDecimal overdueAmountThreshold) {
        Dealer dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() -> new IllegalArgumentException("Dealer not found"));
        AgingSummaryResponse aging = statementService.dealerAging(dealerId, LocalDate.now(), null);
        BigDecimal overdue = aging.buckets().stream()
                .filter(b -> b.fromDays() >= overdueDaysThreshold)
                .map(AgingBucketDto::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (overdue.compareTo(overdueAmountThreshold) > 0) {
            dealer.setStatus("ON_HOLD");
            dealerRepository.save(dealer);
            log.info("Dealer {} set to ON_HOLD due to overdue {}", dealer.getCode(), overdue);
            return true;
        }
        return false;
    }

    /**
     * Simple daily automation: put dealers on hold if >45 days bucket has balance.
     */
    @Scheduled(cron = "0 15 3 * * *")
    public void dailyDunningSweep() {
        List<Dealer> dealers = dealerRepository.findAll();
        for (Dealer dealer : dealers) {
            try {
                AgingSummaryResponse aging = statementService.dealerAging(dealer.getId(), LocalDate.now(), null);
                BigDecimal overdue = aging.buckets().stream()
                        .filter(b -> b.fromDays() >= 45)
                        .map(AgingBucketDto::amount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (overdue.compareTo(BigDecimal.ZERO) > 0 && !"ON_HOLD".equalsIgnoreCase(dealer.getStatus())) {
                    dealer.setStatus("ON_HOLD");
                    dealerRepository.save(dealer);
                    log.info("Dealer {} placed ON_HOLD by dunning sweep; overdue {}", dealer.getCode(), overdue);
                }
            } catch (Exception e) {
                log.warn("Failed dunning evaluation for dealer {}", dealer.getCode(), e);
            }
        }
    }
}
