package com.bigbrightpaints.erp.modules.purchasing.dto;

import com.bigbrightpaints.erp.modules.accounting.domain.GstRegistrationType;
import java.math.BigDecimal;
import java.util.UUID;

public record SupplierResponse(Long id,
                               UUID publicId,
                               String code,
                               String name,
                               String status,
                               String email,
                               String phone,
                               String address,
                               BigDecimal creditLimit,
                               BigDecimal outstandingBalance,
                               Long payableAccountId,
                               String payableAccountCode,
                               String gstNumber,
                               String stateCode,
                               GstRegistrationType gstRegistrationType) {

    public SupplierResponse(Long id,
                            UUID publicId,
                            String code,
                            String name,
                            String status,
                            String email,
                            String phone,
                            String address,
                            BigDecimal creditLimit,
                            BigDecimal outstandingBalance,
                            Long payableAccountId,
                            String payableAccountCode) {
        this(id, publicId, code, name, status, email, phone, address, creditLimit, outstandingBalance,
                payableAccountId, payableAccountCode, null, null, GstRegistrationType.UNREGISTERED);
    }
}
