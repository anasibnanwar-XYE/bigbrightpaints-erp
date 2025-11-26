package com.bigbrightpaints.erp.modules.purchasing.service;

import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.purchasing.domain.Supplier;
import com.bigbrightpaints.erp.modules.purchasing.domain.SupplierRepository;
import com.bigbrightpaints.erp.modules.purchasing.dto.SupplierRequest;
import com.bigbrightpaints.erp.modules.purchasing.dto.SupplierResponse;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final CompanyContextService companyContextService;
    private final AccountRepository accountRepository;

    public SupplierService(SupplierRepository supplierRepository,
                           CompanyContextService companyContextService,
                           AccountRepository accountRepository) {
        this.supplierRepository = supplierRepository;
        this.companyContextService = companyContextService;
        this.accountRepository = accountRepository;
    }

    public List<SupplierResponse> listSuppliers() {
        Company company = companyContextService.requireCurrentCompany();
        return supplierRepository.findByCompanyOrderByNameAsc(company).stream()
                .map(this::toResponse)
                .toList();
    }

    public SupplierResponse getSupplier(Long id) {
        Company company = companyContextService.requireCurrentCompany();
        return toResponse(requireSupplier(company, id));
    }

    @Transactional
    public SupplierResponse createSupplier(SupplierRequest request) {
        Company company = companyContextService.requireCurrentCompany();
        Supplier supplier = new Supplier();
        supplier.setCompany(company);
        supplier.setName(request.name().trim());
        supplier.setCode(resolveSupplierCode(request.code(), request.name(), company));
        supplier.setEmail(normalize(request.contactEmail()));
        supplier.setPhone(normalize(request.contactPhone()));
        supplier.setAddress(normalize(request.address()));
        supplier.setCreditLimit(request.creditLimit() != null ? request.creditLimit() : BigDecimal.ZERO);
        supplier = supplierRepository.save(supplier);

        Account payableAccount = createPayableAccount(company, supplier);
        supplier.setPayableAccount(payableAccount);
        supplier = supplierRepository.save(supplier);
        return toResponse(supplier);
    }

    @Transactional
    public SupplierResponse updateSupplier(Long id, SupplierRequest request) {
        Company company = companyContextService.requireCurrentCompany();
        Supplier supplier = requireSupplier(company, id);
        String oldCode = supplier.getCode();
        String oldName = supplier.getName();
        String newName = request.name().trim();
        supplier.setName(newName);
        String newCode = oldCode;
        if (StringUtils.hasText(request.code())) {
            newCode = resolveSupplierCode(request.code(), request.name(), company, supplier.getId());
            supplier.setCode(newCode);
        }
        syncPayableAccount(supplier, oldCode, newCode, oldName, newName);
        supplier.setEmail(normalize(request.contactEmail()));
        supplier.setPhone(normalize(request.contactPhone()));
        supplier.setAddress(normalize(request.address()));
        supplier.setCreditLimit(request.creditLimit() != null ? request.creditLimit() : BigDecimal.ZERO);
        return toResponse(supplier);
    }

    private void syncPayableAccount(Supplier supplier, String oldCode, String newCode, String oldName, String newName) {
        Account payableAccount = supplier.getPayableAccount();
        if (payableAccount == null) {
            return;
        }
        boolean changed = false;
        if (oldCode != null && !oldCode.equals(newCode)) {
            String oldAccountCode = payableAccount.getCode();
            if (oldAccountCode != null && oldAccountCode.startsWith("AP-" + oldCode)) {
                String suffix = oldAccountCode.substring(("AP-" + oldCode).length());
                String newAccountCode = "AP-" + newCode + suffix;
                if (accountRepository.findByCompanyAndCodeIgnoreCase(supplier.getCompany(), newAccountCode).isEmpty()) {
                    payableAccount.setCode(newAccountCode);
                    changed = true;
                }
            }
        }
        if (oldName != null && !oldName.equals(newName)) {
            payableAccount.setName(newName + " Payable");
            changed = true;
        }
        if (changed) {
            accountRepository.save(payableAccount);
        }
    }

    private Supplier requireSupplier(Company company, Long id) {
        return supplierRepository.findByCompanyAndId(company, id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
    }

    private Account createPayableAccount(Company company, Supplier supplier) {
        String baseCode = "AP-" + supplier.getCode();
        String code = baseCode;
        int attempt = 1;
        while (accountRepository.findByCompanyAndCodeIgnoreCase(company, code).isPresent()) {
            code = baseCode + "-" + attempt++;
        }
        Account account = new Account();
        account.setCompany(company);
        account.setCode(code);
        account.setName(supplier.getName() + " Payable");
        account.setType(AccountType.LIABILITY);
        return accountRepository.save(account);
    }

    private SupplierResponse toResponse(Supplier supplier) {
        Account payableAccount = supplier.getPayableAccount();
        Long accountId = payableAccount != null ? payableAccount.getId() : null;
        String accountCode = payableAccount != null ? payableAccount.getCode() : null;
        return new SupplierResponse(
                supplier.getId(),
                supplier.getPublicId(),
                supplier.getCode(),
                supplier.getName(),
                supplier.getStatus(),
                supplier.getEmail(),
                supplier.getPhone(),
                supplier.getAddress(),
                supplier.getCreditLimit(),
                supplier.getOutstandingBalance(),
                accountId,
                accountCode
        );
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String resolveSupplierCode(String requestedCode, String name, Company company) {
        return resolveSupplierCode(requestedCode, name, company, null);
    }

    private String resolveSupplierCode(String requestedCode, String name, Company company, Long currentId) {
        String base = StringUtils.hasText(requestedCode)
                ? requestedCode.trim()
                : generateCodeFromName(name);
        String code = base;
        int attempt = 1;
        while (supplierRepository.findByCompanyAndCodeIgnoreCase(company, code)
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .isPresent()) {
            code = base + "-" + attempt++;
        }
        return code;
    }

    private String generateCodeFromName(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? "SUPPLIER" : normalized;
    }
}
