package com.bigbrightpaints.erp.modules.accounting.service;

import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class AuditDigestScheduler {

    private static final Logger log = LoggerFactory.getLogger(AuditDigestScheduler.class);

    private final AccountingService accountingService;
    private final CompanyRepository companyRepository;

    public AuditDigestScheduler(AccountingService accountingService,
                                CompanyRepository companyRepository) {
        this.accountingService = accountingService;
        this.companyRepository = companyRepository;
    }

    /**
        * Emit previous-day audit digest for each company at 02:30 server time.
        * Lightweight: logs lines; consumers can ship logs to SIEM.
        */
    @Scheduled(cron = "0 30 2 * * *")
    public void publishDailyDigest() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        companyRepository.findAll().forEach(company -> {
            try {
                CompanyContextHolder.setCompanyId(company.getCode());
                var digest = accountingService.auditDigest(yesterday, yesterday);
                if (digest.entries().isEmpty()) {
                    return;
                }
                digest.entries().forEach(line ->
                        log.info("[AUDIT-DIGEST] company={} period={} {}", company.getCode(), digest.periodLabel(), line));
            } catch (Exception ex) {
                log.warn("Failed to emit audit digest for company {}", company.getCode(), ex);
            } finally {
                CompanyContextHolder.clear();
            }
        });
    }
}
