package com.bigbrightpaints.erp.core.config;

import com.bigbrightpaints.erp.core.service.CriticalFixtureService;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"test", "mock"})
public class CriticalFixtureInitializer {

    @Bean
    CommandLineRunner seedCriticalFixtures(CompanyRepository companyRepository,
                                           CriticalFixtureService criticalFixtureService) {
        return args -> companyRepository.findAll()
                .forEach(criticalFixtureService::seedCompanyFixtures);
    }
}
