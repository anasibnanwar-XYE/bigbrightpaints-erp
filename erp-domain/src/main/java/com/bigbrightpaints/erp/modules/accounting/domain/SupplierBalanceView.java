package com.bigbrightpaints.erp.modules.accounting.domain;

import java.math.BigDecimal;

public record SupplierBalanceView(Long supplierId, BigDecimal balance) {}
