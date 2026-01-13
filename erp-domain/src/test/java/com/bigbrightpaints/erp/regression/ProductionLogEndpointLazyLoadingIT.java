package com.bigbrightpaints.erp.regression;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.factory.domain.ProductionLog;
import com.bigbrightpaints.erp.modules.factory.domain.ProductionLogRepository;
import com.bigbrightpaints.erp.modules.factory.domain.ProductionLogStatus;
import com.bigbrightpaints.erp.modules.production.domain.ProductionBrand;
import com.bigbrightpaints.erp.modules.production.domain.ProductionBrandRepository;
import com.bigbrightpaints.erp.modules.production.domain.ProductionProduct;
import com.bigbrightpaints.erp.modules.production.domain.ProductionProductRepository;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "spring.jpa.open-in-view=false")
@DisplayName("Regression: Production log list/detail should not hit lazy-loading errors")
class ProductionLogEndpointLazyLoadingIT extends AbstractIntegrationTest {

    private static final String COMPANY_CODE = "LEAD-015";
    private static final String ADMIN_EMAIL = "lead015@erp.test";
    private static final String ADMIN_PASSWORD = "lead015";

    @Autowired private TestRestTemplate rest;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private ProductionBrandRepository productionBrandRepository;
    @Autowired private ProductionProductRepository productionProductRepository;
    @Autowired private ProductionLogRepository productionLogRepository;

    private HttpHeaders headers;
    private ProductionLog productionLog;

    @DynamicPropertySource
    static void disableOpenInView(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.open-in-view", () -> false);
    }

    @BeforeEach
    void setUp() {
        dataSeeder.ensureUser(ADMIN_EMAIL, ADMIN_PASSWORD, "Lead 015 Admin", COMPANY_CODE,
                java.util.List.of("ROLE_ADMIN", "ROLE_FACTORY"));

        Company company = companyRepository.findByCodeIgnoreCase(COMPANY_CODE).orElseThrow();
        ProductionBrand brand = ensureBrand(company);
        ProductionProduct product = ensureProduct(company, brand);
        productionLog = ensureLog(company, brand, product);
        headers = createHeaders(login());
    }

    @Test
    void productionLogListAndDetailReturnOk() {
        ResponseEntity<Map> listResponse = rest.exchange(
                "/api/v1/factory/production/logs",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).containsEntry("success", true);

        ResponseEntity<Map> detailResponse = rest.exchange(
                "/api/v1/factory/production/logs/" + productionLog.getId(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );
        assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detailResponse.getBody()).containsEntry("success", true);
        Map<?, ?> detailData = (Map<?, ?>) detailResponse.getBody().get("data");
        assertThat(((Number) detailData.get("id")).longValue()).isEqualTo(productionLog.getId());
    }

    private String login() {
        Map<String, Object> request = Map.of(
                "email", ADMIN_EMAIL,
                "password", ADMIN_PASSWORD,
                "companyCode", COMPANY_CODE
        );
        ResponseEntity<Map> response = rest.postForEntity("/api/v1/auth/login", request, Map.class);
        return response.getBody().get("accessToken").toString();
    }

    private HttpHeaders createHeaders(String token) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(token);
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.set("X-Company-Id", COMPANY_CODE);
        return httpHeaders;
    }

    private ProductionBrand ensureBrand(Company company) {
        String code = "BR-" + UUID.randomUUID();
        ProductionBrand brand = new ProductionBrand();
        brand.setCompany(company);
        brand.setCode(code);
        brand.setName("Lead 015 Brand " + code);
        return productionBrandRepository.save(brand);
    }

    private ProductionProduct ensureProduct(Company company, ProductionBrand brand) {
        String sku = "SKU-" + UUID.randomUUID();
        ProductionProduct product = new ProductionProduct();
        product.setCompany(company);
        product.setBrand(brand);
        product.setProductName("Lead 015 Product " + sku);
        product.setCategory("TEST");
        product.setSkuCode(sku);
        product.setUnitOfMeasure("UNIT");
        return productionProductRepository.save(product);
    }

    private ProductionLog ensureLog(Company company, ProductionBrand brand, ProductionProduct product) {
        ProductionLog log = new ProductionLog();
        log.setCompany(company);
        log.setBrand(brand);
        log.setProduct(product);
        log.setProductionCode("LEAD015-" + UUID.randomUUID());
        log.setBatchSize(new BigDecimal("5"));
        log.setUnitOfMeasure("UNIT");
        log.setMixedQuantity(new BigDecimal("5"));
        log.setStatus(ProductionLogStatus.READY_TO_PACK);
        log.setTotalPackedQuantity(BigDecimal.ZERO);
        log.setWastageQuantity(new BigDecimal("5"));
        log.setProducedAt(Instant.now());
        log.setMaterialCostTotal(BigDecimal.ZERO);
        log.setLaborCostTotal(BigDecimal.ZERO);
        log.setOverheadCostTotal(BigDecimal.ZERO);
        log.setUnitCost(BigDecimal.ZERO);
        return productionLogRepository.save(log);
    }
}
