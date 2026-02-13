package com.bigbrightpaints.erp.modules.production.service;

import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.util.CompanyEntityLookup;
import com.bigbrightpaints.erp.modules.accounting.service.CompanyDefaultAccountsService;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialRepository;
import com.bigbrightpaints.erp.modules.production.domain.CatalogImportRepository;
import com.bigbrightpaints.erp.modules.production.domain.ProductionBrandRepository;
import com.bigbrightpaints.erp.modules.production.domain.ProductionProduct;
import com.bigbrightpaints.erp.modules.production.domain.ProductionProductRepository;
import jakarta.persistence.OptimisticLockException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionCatalogServiceRetryPolicyTest {

    @Mock private CompanyContextService companyContextService;
    @Mock private ProductionBrandRepository brandRepository;
    @Mock private ProductionProductRepository productRepository;
    @Mock private FinishedGoodRepository finishedGoodRepository;
    @Mock private RawMaterialRepository rawMaterialRepository;
    @Mock private CompanyEntityLookup companyEntityLookup;
    @Mock private CompanyDefaultAccountsService companyDefaultAccountsService;
    @Mock private CatalogImportRepository catalogImportRepository;
    @Mock private AuditService auditService;
    @Mock private PlatformTransactionManager transactionManager;

    private ProductionCatalogService service;

    @BeforeEach
    void setUp() {
        service = new ProductionCatalogService(
                companyContextService,
                brandRepository,
                productRepository,
                finishedGoodRepository,
                rawMaterialRepository,
                companyEntityLookup,
                companyDefaultAccountsService,
                catalogImportRepository,
                auditService,
                transactionManager);
    }

    @Test
    void isRetryableImportFailure_treatsOptimisticLockingFailureAsRetryable() {
        boolean retryable = invokeIsRetryableImportFailure(
                new ObjectOptimisticLockingFailureException(ProductionProduct.class, 42L));

        assertThat(retryable).isTrue();
    }

    @Test
    void isRetryableImportFailure_treatsNestedOptimisticLockExceptionAsRetryable() {
        RuntimeException wrapped = new RuntimeException("outer", new OptimisticLockException("stale"));

        boolean retryable = invokeIsRetryableImportFailure(wrapped);

        assertThat(retryable).isTrue();
    }

    @Test
    void refreshCachedProductFromCurrentTransaction_returnsNullWhenStaleCachedIdIsMissing() {
        Company company = new Company();
        ProductionProduct cached = new ProductionProduct();
        ReflectionTestUtils.setField(cached, "id", 99L);
        when(productRepository.findByCompanyAndId(company, 99L)).thenReturn(Optional.empty());

        ProductionProduct refreshed = invokeRefreshCachedProduct(company, cached);

        assertThat(refreshed).isNull();
    }

    @Test
    void refreshCachedProductFromCurrentTransaction_reloadsManagedProductWhenPresent() {
        Company company = new Company();
        ProductionProduct cached = new ProductionProduct();
        ReflectionTestUtils.setField(cached, "id", 100L);
        ProductionProduct managed = new ProductionProduct();
        ReflectionTestUtils.setField(managed, "id", 100L);
        when(productRepository.findByCompanyAndId(company, 100L)).thenReturn(Optional.of(managed));

        ProductionProduct refreshed = invokeRefreshCachedProduct(company, cached);

        assertThat(refreshed).isSameAs(managed);
    }

    @Test
    void refreshCachedProductFromCurrentTransaction_keepsTransientProductWithoutLookup() {
        Company company = new Company();
        ProductionProduct transientProduct = new ProductionProduct();

        ProductionProduct refreshed = invokeRefreshCachedProduct(company, transientProduct);

        assertThat(refreshed).isSameAs(transientProduct);
        verifyNoInteractions(productRepository);
    }

    private boolean invokeIsRetryableImportFailure(Throwable error) {
        Boolean retryable = ReflectionTestUtils.invokeMethod(service, "isRetryableImportFailure", error);
        return Boolean.TRUE.equals(retryable);
    }

    private ProductionProduct invokeRefreshCachedProduct(Company company, ProductionProduct cached) {
        return ReflectionTestUtils.invokeMethod(service, "refreshCachedProductFromCurrentTransaction", company, cached);
    }
}
