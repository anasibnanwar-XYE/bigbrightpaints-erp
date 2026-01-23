package com.bigbrightpaints.erp.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class CompanyContextHolderTest {

    @Test
    void setGetClear_cycle() {
        CompanyContextHolder.setCompanyId("ACME");
        assertThat(CompanyContextHolder.getCompanyId()).isEqualTo("ACME");
        CompanyContextHolder.clear();
        assertThat(CompanyContextHolder.getCompanyId()).isNull();
    }

    @Test
    void threadIsolation_doesNotLeakBetweenThreads() throws InterruptedException {
        CompanyContextHolder.setCompanyId("MAIN");
        AtomicReference<String> workerValue = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            CompanyContextHolder.setCompanyId("WORKER");
            workerValue.set(CompanyContextHolder.getCompanyId());
            CompanyContextHolder.clear();
        });
        worker.start();
        worker.join();
        assertThat(workerValue.get()).isEqualTo("WORKER");
        assertThat(CompanyContextHolder.getCompanyId()).isEqualTo("MAIN");
        CompanyContextHolder.clear();
    }
}
