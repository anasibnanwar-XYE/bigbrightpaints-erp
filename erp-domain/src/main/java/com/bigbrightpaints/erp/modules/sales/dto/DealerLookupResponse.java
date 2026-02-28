package com.bigbrightpaints.erp.modules.sales.dto;

import com.bigbrightpaints.erp.modules.accounting.domain.GstRegistrationType;
import java.math.BigDecimal;
import java.util.UUID;

public record DealerLookupResponse(
        Long id,
        UUID publicId,
        String name,
        String code,
        BigDecimal outstandingBalance,
        BigDecimal creditLimit,
        Long receivableAccountId,
        String receivableAccountCode,
        String stateCode,
        GstRegistrationType gstRegistrationType
) {

    public DealerLookupResponse(Long id,
                                UUID publicId,
                                String name,
                                String code,
                                BigDecimal outstandingBalance,
                                BigDecimal creditLimit,
                                Long receivableAccountId,
                                String receivableAccountCode) {
        this(id, publicId, name, code, outstandingBalance, creditLimit, receivableAccountId,
                receivableAccountCode, null, GstRegistrationType.UNREGISTERED);
    }
}
