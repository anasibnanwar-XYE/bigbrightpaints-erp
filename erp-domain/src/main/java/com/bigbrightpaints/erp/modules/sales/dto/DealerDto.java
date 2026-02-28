package com.bigbrightpaints.erp.modules.sales.dto;

import com.bigbrightpaints.erp.modules.accounting.domain.GstRegistrationType;
import java.math.BigDecimal;
import java.util.UUID;

public record DealerDto(Long id,
                        UUID publicId,
                        String name,
                        String code,
                        String email,
                        String phone,
                        String status,
                        BigDecimal creditLimit,
                        BigDecimal outstandingBalance,
                        String gstNumber,
                        String stateCode,
                        GstRegistrationType gstRegistrationType) {

    public DealerDto(Long id,
                     UUID publicId,
                     String name,
                     String code,
                     String email,
                     String phone,
                     String status,
                     BigDecimal creditLimit,
                     BigDecimal outstandingBalance) {
        this(id, publicId, name, code, email, phone, status, creditLimit, outstandingBalance,
                null, null, GstRegistrationType.UNREGISTERED);
    }
}
