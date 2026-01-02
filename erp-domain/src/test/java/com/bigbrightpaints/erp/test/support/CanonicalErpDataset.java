package com.bigbrightpaints.erp.test.support;

import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGood;
import com.bigbrightpaints.erp.modules.production.domain.ProductionBrand;
import com.bigbrightpaints.erp.modules.production.domain.ProductionProduct;
import com.bigbrightpaints.erp.modules.purchasing.domain.Supplier;
import com.bigbrightpaints.erp.modules.sales.domain.Dealer;

import java.util.Map;

public record CanonicalErpDataset(
        Company company,
        Map<String, Account> accounts,
        Dealer dealer,
        Supplier supplier,
        ProductionBrand brand,
        ProductionProduct product,
        FinishedGood finishedGood
) {
    public Account requireAccount(String code) {
        Account account = accounts.get(code);
        if (account == null) {
            throw new IllegalStateException("Missing account for code: " + code);
        }
        return account;
    }
}
